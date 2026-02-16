import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  createAdminEventExportJob,
  downloadAdminEventExportJobCsv,
  exportAdminEventsCsv,
  getAdminEventExportOpsMetrics,
  getAdminEventExportJob,
  type AdminEventExportJob,
  type AdminEventExportOpsMetrics,
  listAdminEvents,
  type AdminEventItem,
  type AdminEventSummary,
  type EventType,
} from "../api/adminEvents";
import { listUsers } from "../api/adminUsers";

const EVENT_TYPE_OPTIONS: Array<{ label: string; value: "all" | EventType }> = [
  { label: "전체", value: "all" },
  { label: "열람", value: "HYMN_OPENED" },
  { label: "파트 재생", value: "PART_PLAYED" },
  { label: "메모 저장", value: "NOTE_SAVED" },
  { label: "즐겨찾기", value: "FAVORITE_TOGGLED" },
];

const DAY_PRESETS = [1, 7, 30, 60, 90];
const MIN_SUMMARY_DAYS = 1;
const MAX_SUMMARY_DAYS = 90;
const SIZE_OPTIONS = [20, 50, 100, 200];
const ASYNC_EXPORT_LIMIT = 50_000;
const ASYNC_EXPORT_POLL_INTERVAL_MS = 2_000;

function toIsoUtc(localDateTime: string): string | undefined {
  if (!localDateTime) {
    return undefined;
  }
  const parsed = new Date(localDateTime);
  if (Number.isNaN(parsed.getTime())) {
    return undefined;
  }
  return parsed.toISOString();
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString("ko-KR", { hour12: false });
}

function shortUuid(value: string | null): string {
  if (!value) return "-";
  if (value.length <= 12) return value;
  return `${value.slice(0, 8)}...${value.slice(-4)}`;
}

function triggerDownload(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  document.body.removeChild(anchor);
  URL.revokeObjectURL(url);
}

function clampSummaryDays(days: number): number {
  return Math.min(Math.max(days, MIN_SUMMARY_DAYS), MAX_SUMMARY_DAYS);
}

function formatSeconds(value: number): string {
  return `${value.toLocaleString("ko-KR", { maximumFractionDigits: 2 })}s`;
}

function formatAsyncJobStatus(status: AdminEventExportJob["status"]): string {
  if (status === "QUEUED") {
    return "대기 중";
  }
  if (status === "RUNNING") {
    return "처리 중";
  }
  if (status === "COMPLETED") {
    return "완료";
  }
  return "실패";
}

export default function AdminEventPage() {
  const [eventType, setEventType] = useState<"all" | EventType>("all");
  const [userId, setUserId] = useState("");
  const [hymnId, setHymnId] = useState("");
  const [fromLocal, setFromLocal] = useState("");
  const [toLocal, setToLocal] = useState("");
  const [summaryDays, setSummaryDays] = useState(7);
  const [opsMetricsDays, setOpsMetricsDays] = useState(7);
  const [size, setSize] = useState(50);
  const [page, setPage] = useState(1);

  const [items, setItems] = useState<AdminEventItem[]>([]);
  const [summary, setSummary] = useState<AdminEventSummary | null>(null);
  const [pagination, setPagination] = useState({
    page: 1,
    size: 50,
    total: 0,
    totalPages: 0,
    hasPrevious: false,
    hasNext: false,
  });
  const [loading, setLoading] = useState(true);
  const [exporting, setExporting] = useState(false);
  const [creatingAsyncJob, setCreatingAsyncJob] = useState(false);
  const [downloadingAsyncJob, setDownloadingAsyncJob] = useState(false);
  const [asyncJob, setAsyncJob] = useState<AdminEventExportJob | null>(null);
  const [opsMetrics, setOpsMetrics] = useState<AdminEventExportOpsMetrics | null>(null);
  const [opsMetricsLoading, setOpsMetricsLoading] = useState(true);
  const [userNamesById, setUserNamesById] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const isSummaryWindowFromDateFilter = Boolean(fromLocal || toLocal);

  const maxSummaryCount = useMemo(() => {
    if (!summary || summary.byType.length === 0) {
      return 1;
    }
    return Math.max(...summary.byType.map((item) => item.count), 1);
  }, [summary]);

  const fetchEvents = async (targetPage: number) => {
    setLoading(true);
    setError(null);
    try {
      const data = await listAdminEvents({
        eventType: eventType === "all" ? undefined : eventType,
        userId: userId.trim() || undefined,
        hymnId: hymnId.trim() || undefined,
        from: toIsoUtc(fromLocal),
        to: toIsoUtc(toLocal),
        page: targetPage,
        size,
        summaryDays,
      });
      setItems(data.items);
      setSummary(data.summary);
      setPagination(data.pagination);
    } catch (err) {
      setError(err instanceof Error ? err.message : "감사 로그를 불러오지 못했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const fetchOpsMetrics = async (days: number) => {
    setOpsMetricsLoading(true);
    try {
      const data = await getAdminEventExportOpsMetrics(days);
      setOpsMetrics(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "운영 지표를 불러오지 못했습니다.");
    } finally {
      setOpsMetricsLoading(false);
    }
  };

  const fetchUserNames = async () => {
    try {
      const users = await listUsers();
      const nextMap: Record<string, string> = {};
      for (const user of users) {
        nextMap[user.id] = user.displayName;
      }
      setUserNamesById(nextMap);
    } catch {
      // Ignore user-name map failures; events table should still work with raw UUIDs.
    }
  };

  useEffect(() => {
    void fetchEvents(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

  useEffect(() => {
    void fetchUserNames();
  }, []);

  useEffect(() => {
    void fetchOpsMetrics(opsMetricsDays);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opsMetricsDays]);

  useEffect(() => {
    if (!asyncJob || (asyncJob.status !== "QUEUED" && asyncJob.status !== "RUNNING")) {
      return;
    }

    let cancelled = false;
    const pollStatus = async () => {
      if (cancelled) {
        return;
      }
      try {
        const latest = await getAdminEventExportJob(asyncJob.id);
        if (!cancelled) {
          setAsyncJob(latest);
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : "비동기 내보내기 상태를 확인하지 못했습니다.");
        }
      }
    };

    void pollStatus();
    const timer = window.setInterval(() => {
      void pollStatus();
    }, ASYNC_EXPORT_POLL_INTERVAL_MS);

    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [asyncJob?.id, asyncJob?.status]);

  const handleSubmit = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setPage(1);
    void fetchEvents(1);
  };

  const handleExportCsv = async () => {
    setExporting(true);
    setError(null);
    try {
      const result = await exportAdminEventsCsv({
        eventType: eventType === "all" ? undefined : eventType,
        userId: userId.trim() || undefined,
        hymnId: hymnId.trim() || undefined,
        from: toIsoUtc(fromLocal),
        to: toIsoUtc(toLocal),
        limit: 5000,
      });
      triggerDownload(result.blob, result.filename);
    } catch (err) {
      setError(err instanceof Error ? err.message : "CSV 내보내기에 실패했습니다.");
    } finally {
      setExporting(false);
    }
  };

  const handleCreateAsyncExportJob = async () => {
    setCreatingAsyncJob(true);
    setError(null);
    try {
      const job = await createAdminEventExportJob({
        eventType: eventType === "all" ? undefined : eventType,
        userId: userId.trim() || undefined,
        hymnId: hymnId.trim() || undefined,
        from: toIsoUtc(fromLocal),
        to: toIsoUtc(toLocal),
        limit: ASYNC_EXPORT_LIMIT,
      });
      setAsyncJob(job);
    } catch (err) {
      setError(err instanceof Error ? err.message : "비동기 내보내기 요청에 실패했습니다.");
    } finally {
      setCreatingAsyncJob(false);
    }
  };

  const handleDownloadAsyncExport = async () => {
    if (!asyncJob || !asyncJob.downloadable) {
      return;
    }

    setDownloadingAsyncJob(true);
    setError(null);
    try {
      const result = await downloadAdminEventExportJobCsv(asyncJob.id);
      triggerDownload(result.blob, result.filename);
      const refreshed = await getAdminEventExportJob(asyncJob.id);
      setAsyncJob(refreshed);
    } catch (err) {
      setError(err instanceof Error ? err.message : "비동기 CSV 다운로드에 실패했습니다.");
    } finally {
      setDownloadingAsyncJob(false);
    }
  };

  const resolveUserName = (targetUserId: string): string => {
    return userNamesById[targetUserId] ?? "미등록 사용자";
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">감사 로그/분석</h1>
        <p className="mt-1 text-sm text-slate-600">
          사용자 이벤트 조회, 기간 집계, CSV 내보내기(동기/비동기)까지 한 화면에서 처리합니다.
        </p>
      </section>

      <div className="bg-white rounded-2xl border border-indigo-100 p-4 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-base font-semibold text-gray-900">비동기 내보내기 운영 지표</h2>
            {opsMetrics && (
              <p className="text-xs text-gray-500 mt-1">
                {formatDateTime(opsMetrics.fromInclusive)} ~ {formatDateTime(opsMetrics.toExclusive)}
              </p>
            )}
          </div>
          <div className="flex flex-wrap gap-2">
            {DAY_PRESETS.map((days) => (
              <button
                key={days}
                type="button"
                onClick={() => setOpsMetricsDays(days)}
                className={`px-2 py-1 text-xs rounded border ${
                  opsMetricsDays === days
                    ? "bg-indigo-600 text-white border-indigo-600"
                    : "bg-white text-gray-700 border-gray-300 hover:bg-gray-50"
                }`}
              >
                {`${days}d`}
              </button>
            ))}
          </div>
        </div>

        {opsMetricsLoading && <p className="text-sm text-gray-500 mt-3">지표 로딩 중...</p>}

        {!opsMetricsLoading && opsMetrics && (
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mt-3">
            <div className="rounded border border-gray-200 px-3 py-2">
              <p className="text-xs text-gray-500">총 작업 수</p>
              <p className="text-xl font-semibold text-gray-900">{opsMetrics.jobs.total.toLocaleString("ko-KR")}</p>
              <p className="text-xs text-gray-500 mt-1">
                completed {opsMetrics.jobs.completed.toLocaleString("ko-KR")} / failed{" "}
                {opsMetrics.jobs.failed.toLocaleString("ko-KR")}
              </p>
            </div>
            <div className="rounded border border-gray-200 px-3 py-2">
              <p className="text-xs text-gray-500">실패율</p>
              <p className="text-xl font-semibold text-rose-600">{opsMetrics.jobs.failureRatePercent.toFixed(2)}%</p>
              <p className="text-xs text-gray-500 mt-1">
                queued {opsMetrics.jobs.queued.toLocaleString("ko-KR")} / running{" "}
                {opsMetrics.jobs.running.toLocaleString("ko-KR")}
              </p>
            </div>
            <div className="rounded border border-gray-200 px-3 py-2">
              <p className="text-xs text-gray-500">처리 시간</p>
              <p className="text-xl font-semibold text-gray-900">{formatSeconds(opsMetrics.processing.averageSeconds)}</p>
              <p className="text-xs text-gray-500 mt-1">
                p95 {formatSeconds(opsMetrics.processing.p95Seconds)} / sample{" "}
                {opsMetrics.processing.measuredJobs.toLocaleString("ko-KR")}
              </p>
            </div>
            <div className="rounded border border-gray-200 px-3 py-2">
              <p className="text-xs text-gray-500">정리 건수</p>
              <p className="text-xl font-semibold text-emerald-700">
                {opsMetrics.cleanup.deletedJobs.toLocaleString("ko-KR")}
              </p>
              <p className="text-xs text-gray-500 mt-1">runs {opsMetrics.cleanup.runCount.toLocaleString("ko-KR")}</p>
            </div>
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit} className="bg-white rounded-lg shadow p-4 mb-4">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <select
            value={eventType}
            onChange={(e) => setEventType(e.target.value as "all" | EventType)}
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          >
            {EVENT_TYPE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <input
            type="text"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            placeholder="userId(UUID)"
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <input
            type="text"
            value={hymnId}
            onChange={(e) => setHymnId(e.target.value)}
            placeholder="hymnId(UUID)"
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <input
            type="datetime-local"
            value={fromLocal}
            onChange={(e) => setFromLocal(e.target.value)}
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <input
            type="datetime-local"
            value={toLocal}
            onChange={(e) => setToLocal(e.target.value)}
            className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <div className="grid grid-cols-2 gap-3">
            <select
              value={size}
              onChange={(e) => setSize(Number(e.target.value))}
              className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              title="페이지 크기"
            >
              {SIZE_OPTIONS.map((option) => (
                <option key={option} value={option}>{`${option}건`}</option>
              ))}
            </select>
            <input
              type="number"
              min={MIN_SUMMARY_DAYS}
              max={MAX_SUMMARY_DAYS}
              value={summaryDays}
              onChange={(e) => {
                const parsed = Number.parseInt(e.target.value, 10);
                if (Number.isNaN(parsed)) {
                  setSummaryDays(MIN_SUMMARY_DAYS);
                  return;
                }
                setSummaryDays(clampSummaryDays(parsed));
              }}
              onBlur={() => setSummaryDays((prev) => clampSummaryDays(prev))}
              className="border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              title="집계 기간(일)"
              placeholder="집계 기간(일)"
            />
          </div>
        </div>
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="text-xs text-gray-500">집계 기간 프리셋</span>
          {DAY_PRESETS.map((days) => (
            <button
              key={days}
              type="button"
              onClick={() => setSummaryDays(days)}
              className={`px-2 py-1 text-xs rounded border ${
                summaryDays === days
                  ? "bg-indigo-600 text-white border-indigo-600"
                  : "bg-white text-gray-700 border-gray-300 hover:bg-gray-50"
              }`}
            >
              {`${days}일`}
            </button>
          ))}
          <span className="text-xs text-gray-500">허용 범위: 1~90일</span>
        </div>
        <div className="mt-3 flex justify-end gap-2">
          <button
            type="button"
            onClick={handleCreateAsyncExportJob}
            className="border border-emerald-600 text-emerald-700 px-4 py-2 rounded hover:bg-emerald-50 disabled:opacity-60"
            disabled={loading || creatingAsyncJob}
            title={`필터 조건 기준 최대 ${ASYNC_EXPORT_LIMIT.toLocaleString("ko-KR")}건 비동기 내보내기`}
          >
            {creatingAsyncJob ? "요청 중..." : "비동기 CSV 요청"}
          </button>
          <button
            type="button"
            onClick={handleExportCsv}
            className="border border-indigo-600 text-indigo-700 px-4 py-2 rounded hover:bg-indigo-50 disabled:opacity-60"
            disabled={loading || exporting}
          >
            {exporting ? "내보내는 중..." : "CSV 내보내기"}
          </button>
          <button
            type="submit"
            className="bg-indigo-600 text-white px-4 py-2 rounded hover:bg-indigo-700 disabled:opacity-60"
            disabled={loading}
          >
            {loading ? "조회 중..." : "조회"}
          </button>
        </div>
      </form>

      {error && <p className="text-red-600 mb-4">{error}</p>}

      {asyncJob && (
        <div className="bg-white rounded-lg shadow p-4 mb-4 border border-emerald-100">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="text-sm text-gray-500">비동기 내보내기 작업</p>
              <p className="text-xs text-gray-500 break-all">jobId: {asyncJob.id}</p>
            </div>
            <span
              className={`px-2 py-1 text-xs rounded-full font-medium ${
                asyncJob.status === "COMPLETED"
                  ? "bg-emerald-100 text-emerald-700"
                  : asyncJob.status === "FAILED"
                    ? "bg-red-100 text-red-700"
                    : "bg-amber-100 text-amber-700"
              }`}
            >
              {formatAsyncJobStatus(asyncJob.status)}
            </span>
          </div>
          <div className="mt-3 grid grid-cols-1 md:grid-cols-2 gap-2 text-sm text-gray-600">
            <div>요청 상한: {asyncJob.exportLimit.toLocaleString("ko-KR")}건</div>
            <div>완료 건수: {(asyncJob.rowCount ?? 0).toLocaleString("ko-KR")}건</div>
            <div>요청 시각: {formatDateTime(asyncJob.createdAt)}</div>
            <div>완료 시각: {asyncJob.completedAt ? formatDateTime(asyncJob.completedAt) : "-"}</div>
          </div>
          {asyncJob.errorMessage && <p className="mt-2 text-sm text-red-600">{asyncJob.errorMessage}</p>}
          <div className="mt-3 flex flex-wrap items-center justify-end gap-2">
            {(asyncJob.status === "QUEUED" || asyncJob.status === "RUNNING") && (
              <span className="text-xs text-gray-500">2초 간격으로 상태를 자동 갱신합니다.</span>
            )}
            <button
              type="button"
              onClick={handleDownloadAsyncExport}
              disabled={!asyncJob.downloadable || downloadingAsyncJob}
              className="border border-emerald-600 text-emerald-700 px-3 py-1.5 rounded hover:bg-emerald-50 disabled:opacity-50"
            >
              {downloadingAsyncJob ? "다운로드 중..." : "비동기 CSV 다운로드"}
            </button>
          </div>
        </div>
      )}

      {summary && (
        <div className="bg-white rounded-lg shadow p-4 mb-4">
          <div className="flex flex-wrap items-center justify-between gap-2 mb-3">
            <h2 className="text-lg font-semibold">
              {isSummaryWindowFromDateFilter ? "지정 기간 이벤트 집계" : `최근 ${summaryDays}일 이벤트 집계`}
            </h2>
            <div className="text-sm text-gray-500">
              {formatDateTime(summary.fromInclusive)} ~ {formatDateTime(summary.toExclusive)}
            </div>
          </div>
          <p className="text-sm text-gray-600 mb-3">총 이벤트: {summary.total.toLocaleString("ko-KR")}건</p>
          <div className="space-y-2">
            {summary.byType.map((row) => {
              const width = `${(row.count / maxSummaryCount) * 100}%`;
              return (
                <div key={row.eventType}>
                  <div className="flex justify-between text-sm mb-1">
                    <span className="font-medium">{row.eventType}</span>
                    <span className="text-gray-600">{row.count.toLocaleString("ko-KR")}</span>
                  </div>
                  <div className="h-2 bg-gray-100 rounded">
                    <div className="h-2 bg-indigo-500 rounded" style={{ width }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg shadow overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-50 border-b">
            <tr>
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">시각</th>
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">이벤트</th>
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">사용자</th>
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">hymnId</th>
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">part</th>
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">metadata</th>
            </tr>
          </thead>
          <tbody>
            {!loading && items.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  조회된 이벤트가 없습니다.
                </td>
              </tr>
            )}
            {items.map((item) => (
              <tr key={item.id} className="border-b last:border-b-0 hover:bg-gray-50 align-top">
                <td className="px-4 py-3 text-sm whitespace-nowrap">{formatDateTime(item.createdAt)}</td>
                <td className="px-4 py-3 text-sm font-medium">{item.eventType}</td>
                <td className="px-4 py-3 text-xs text-gray-600">
                  <div className="font-semibold text-slate-800">{resolveUserName(item.userId)}</div>
                  <div className="mt-1 font-mono text-[11px] text-slate-500" title={item.userId}>
                    {shortUuid(item.userId)}
                  </div>
                </td>
                <td className="px-4 py-3 text-xs text-gray-600 break-all">{item.hymnId ?? "-"}</td>
                <td className="px-4 py-3 text-sm">{item.part ?? "-"}</td>
                <td className="px-4 py-3 text-xs text-gray-600 break-all">{item.metadataJson ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="mt-4 flex items-center justify-between text-sm text-gray-600">
        <span>
          페이지 {pagination.totalPages === 0 ? 0 : pagination.page} / {pagination.totalPages}, 전체 {" "}
          {pagination.total.toLocaleString("ko-KR")}건
        </span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setPage((prev) => Math.max(prev - 1, 1))}
            disabled={loading || !pagination.hasPrevious}
            className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50"
          >
            이전
          </button>
          <button
            type="button"
            onClick={() => setPage((prev) => prev + 1)}
            disabled={loading || !pagination.hasNext}
            className="px-3 py-1 border border-gray-300 rounded hover:bg-gray-50 disabled:opacity-50"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  );
}
