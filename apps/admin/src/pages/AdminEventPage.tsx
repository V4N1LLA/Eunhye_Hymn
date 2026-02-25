import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  createAdminEventExportJob,
  downloadAdminEventExportJobCsv,
  exportAdminEventsCsv,
  getAdminEventExportJob,
  getAdminEventExportOpsMetrics,
  listAdminEvents,
  type AdminEventExportJob,
  type AdminEventExportOpsMetrics,
  type AdminEventItem,
  type AdminEventSummary,
  type EventType,
} from "../api/adminEvents";
import { listUsers } from "../api/adminUsers";

const EVENT_TYPE_OPTIONS: Array<{ label: string; value: "all" | EventType }> = [
  { label: "전체", value: "all" },
  { label: "찬양 열람", value: "HYMN_OPENED" },
  { label: "파트 재생", value: "PART_PLAYED" },
  { label: "메모 저장", value: "NOTE_SAVED" },
  { label: "즐겨찾기 토글", value: "FAVORITE_TOGGLED" },
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
  return `${value.toLocaleString("ko-KR", { maximumFractionDigits: 2 })}초`;
}

function formatAsyncJobStatus(status: AdminEventExportJob["status"]): string {
  if (status === "QUEUED") {
    return "대기";
  }
  if (status === "RUNNING") {
    return "실행 중";
  }
  if (status === "COMPLETED") {
    return "완료";
  }
  return "실패";
}

function asyncStatusPillClass(status: AdminEventExportJob["status"]): string {
  if (status === "COMPLETED") return "bg-emerald-100 text-emerald-700";
  if (status === "FAILED") return "bg-red-100 text-red-700";
  return "bg-amber-100 text-amber-700";
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
      setError(err instanceof Error ? err.message : "감사 이벤트를 불러오지 못했습니다.");
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
      setError(err instanceof Error ? err.message : "비동기 내보내기 운영 지표를 불러오지 못했습니다.");
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
      // 사용자 매핑 실패 시 UUID만 표시.
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
          setError(err instanceof Error ? err.message : "비동기 내보내기 상태 조회에 실패했습니다.");
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

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
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
      setError(err instanceof Error ? err.message : "비동기 내보내기 작업 생성에 실패했습니다.");
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
      <section className="soy-panel">
        <p className="soy-kicker">감사 로그</p>
        <h1 className="soy-title">이벤트 조회 및 내보내기</h1>
        <p className="soy-description">필터 조회, 기간 요약, CSV 내보내기(동기/비동기)를 지원합니다.</p>
      </section>

      <section className="soy-panel">
        <div className="soy-panel-header">
          <div>
            <h2 className="soy-title">비동기 내보내기 운영 지표</h2>
            {opsMetrics && (
              <p className="soy-description">
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
                className={`soy-btn ${opsMetricsDays === days ? "soy-btn-primary" : "soy-btn-secondary"}`}
              >
                {`${days}일`}
              </button>
            ))}
          </div>
        </div>

        {opsMetricsLoading && <p className="mt-3 text-sm text-slate-500">지표를 불러오는 중입니다...</p>}

        {!opsMetricsLoading && opsMetrics && (
          <div className="mt-3 grid grid-cols-1 gap-3 md:grid-cols-4">
            <div className="soy-stat-card">
              <p className="soy-stat-label">총 작업 수</p>
              <p className="soy-stat-value">{opsMetrics.jobs.total.toLocaleString("ko-KR")}</p>
              <p className="mt-1 text-xs text-slate-500">
                완료 {opsMetrics.jobs.completed.toLocaleString("ko-KR")} / 실패 {opsMetrics.jobs.failed.toLocaleString("ko-KR")}
              </p>
            </div>
            <div className="soy-stat-card">
              <p className="soy-stat-label">실패율</p>
              <p className="soy-stat-value text-rose-600">{opsMetrics.jobs.failureRatePercent.toFixed(2)}%</p>
              <p className="mt-1 text-xs text-slate-500">
                대기 {opsMetrics.jobs.queued.toLocaleString("ko-KR")} / 실행 {opsMetrics.jobs.running.toLocaleString("ko-KR")}
              </p>
            </div>
            <div className="soy-stat-card">
              <p className="soy-stat-label">처리 시간</p>
              <p className="soy-stat-value">{formatSeconds(opsMetrics.processing.averageSeconds)}</p>
              <p className="mt-1 text-xs text-slate-500">
                p95 {formatSeconds(opsMetrics.processing.p95Seconds)} / 표본 {opsMetrics.processing.measuredJobs.toLocaleString("ko-KR")}
              </p>
            </div>
            <div className="soy-stat-card">
              <p className="soy-stat-label">정리 삭제 건수</p>
              <p className="soy-stat-value text-emerald-700">{opsMetrics.cleanup.deletedJobs.toLocaleString("ko-KR")}</p>
              <p className="mt-1 text-xs text-slate-500">실행 {opsMetrics.cleanup.runCount.toLocaleString("ko-KR")}회</p>
            </div>
          </div>
        )}
      </section>

      <form onSubmit={handleSubmit} className="soy-panel">
        <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
          <select
            value={eventType}
            onChange={(event) => setEventType(event.target.value as "all" | EventType)}
            className="soy-select"
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
            onChange={(event) => setUserId(event.target.value)}
            placeholder="userId (UUID)"
            className="soy-input"
          />

          <input
            type="text"
            value={hymnId}
            onChange={(event) => setHymnId(event.target.value)}
            placeholder="hymnId (UUID)"
            className="soy-input"
          />

          <input type="datetime-local" value={fromLocal} onChange={(event) => setFromLocal(event.target.value)} className="soy-input" />

          <input type="datetime-local" value={toLocal} onChange={(event) => setToLocal(event.target.value)} className="soy-input" />

          <div className="grid grid-cols-2 gap-3">
            <select
              value={size}
              onChange={(event) => setSize(Number(event.target.value))}
              className="soy-select"
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
              onChange={(event) => {
                const parsed = Number.parseInt(event.target.value, 10);
                if (Number.isNaN(parsed)) {
                  setSummaryDays(MIN_SUMMARY_DAYS);
                  return;
                }
                setSummaryDays(clampSummaryDays(parsed));
              }}
              onBlur={() => setSummaryDays((prev) => clampSummaryDays(prev))}
              className="soy-input"
              title="요약 기간(일)"
              placeholder="요약 기간"
            />
          </div>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          <span className="text-xs text-slate-500">요약 프리셋:</span>
          {DAY_PRESETS.map((days) => (
            <button
              key={days}
              type="button"
              onClick={() => setSummaryDays(days)}
              className={`soy-btn ${summaryDays === days ? "soy-btn-primary" : "soy-btn-secondary"}`}
            >
              {`${days}일`}
            </button>
          ))}
          <span className="text-xs text-slate-500">허용 범위: 1~90일</span>
        </div>

        <div className="mt-3 flex flex-wrap justify-end gap-2">
          <button
            type="button"
            onClick={() => void handleCreateAsyncExportJob()}
            className="soy-btn border-emerald-600 text-emerald-700 hover:bg-emerald-50"
            disabled={loading || creatingAsyncJob}
            title={`최대 ${ASYNC_EXPORT_LIMIT.toLocaleString("ko-KR")}건 비동기 내보내기`}
          >
            {creatingAsyncJob ? "요청 중..." : "비동기 CSV 요청"}
          </button>
          <button
            type="button"
            onClick={() => void handleExportCsv()}
            className="soy-btn border-indigo-600 text-indigo-700 hover:bg-indigo-50"
            disabled={loading || exporting}
          >
            {exporting ? "내보내는 중..." : "CSV 내보내기"}
          </button>
          <button type="submit" className="soy-btn soy-btn-primary" disabled={loading}>
            {loading ? "조회 중..." : "조회"}
          </button>
        </div>
      </form>

      {error && <div className="soy-alert soy-alert-error">{error}</div>}

      {asyncJob && (
        <section className="soy-panel border-emerald-100">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div>
              <p className="text-sm text-slate-500">비동기 내보내기 작업</p>
              <p className="break-all text-xs text-slate-500">jobId: {asyncJob.id}</p>
            </div>
            <span className={`soy-pill ${asyncStatusPillClass(asyncJob.status)}`}>{formatAsyncJobStatus(asyncJob.status)}</span>
          </div>

          <div className="mt-3 grid grid-cols-1 gap-2 text-sm text-slate-600 md:grid-cols-2">
            <div>요청 상한: {asyncJob.exportLimit.toLocaleString("ko-KR")}건</div>
            <div>결과 행수: {(asyncJob.rowCount ?? 0).toLocaleString("ko-KR")}건</div>
            <div>생성 시각: {formatDateTime(asyncJob.createdAt)}</div>
            <div>완료 시각: {asyncJob.completedAt ? formatDateTime(asyncJob.completedAt) : "-"}</div>
          </div>

          {asyncJob.errorMessage && <p className="mt-2 text-sm text-red-600">{asyncJob.errorMessage}</p>}

          <div className="mt-3 flex flex-wrap items-center justify-end gap-2">
            {(asyncJob.status === "QUEUED" || asyncJob.status === "RUNNING") && (
              <span className="text-xs text-slate-500">2초 간격으로 상태를 자동 갱신합니다.</span>
            )}
            <button
              type="button"
              onClick={() => void handleDownloadAsyncExport()}
              disabled={!asyncJob.downloadable || downloadingAsyncJob}
              className="soy-btn border-emerald-600 text-emerald-700 hover:bg-emerald-50"
            >
              {downloadingAsyncJob ? "다운로드 중..." : "비동기 CSV 다운로드"}
            </button>
          </div>
        </section>
      )}

      {summary && (
        <section className="soy-panel">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-slate-900">
              {isSummaryWindowFromDateFilter ? "지정 기간 이벤트 요약" : `최근 ${summaryDays}일 이벤트 요약`}
            </h2>
            <div className="text-sm text-slate-500">
              {formatDateTime(summary.fromInclusive)} ~ {formatDateTime(summary.toExclusive)}
            </div>
          </div>

          <p className="mb-3 text-sm text-slate-600">총 이벤트: {summary.total.toLocaleString("ko-KR")}건</p>
          <div className="space-y-2">
            {summary.byType.map((row) => {
              const width = `${(row.count / maxSummaryCount) * 100}%`;
              return (
                <div key={row.eventType}>
                  <div className="mb-1 flex justify-between text-sm">
                    <span className="font-medium">{row.eventType}</span>
                    <span className="text-slate-600">{row.count.toLocaleString("ko-KR")}</span>
                  </div>
                  <div className="h-2 rounded bg-slate-100">
                    <div className="h-2 rounded bg-indigo-500" style={{ width }} />
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}

      <section className="soy-panel p-0">
        <div className="soy-table-wrap">
          <table className="soy-table min-w-[980px]">
            <thead>
              <tr>
                <th>시각</th>
                <th>이벤트</th>
                <th>사용자</th>
                <th>hymnId</th>
                <th>파트</th>
                <th>메타데이터</th>
              </tr>
            </thead>
            <tbody>
              {!loading && items.length === 0 && (
                <tr>
                  <td colSpan={6}>
                    <div className="soy-empty m-3">조회 결과가 없습니다.</div>
                  </td>
                </tr>
              )}

              {items.map((item) => (
                <tr key={item.id}>
                  <td className="whitespace-nowrap">{formatDateTime(item.createdAt)}</td>
                  <td className="font-medium">{item.eventType}</td>
                  <td className="text-xs text-slate-600">
                    <div className="font-semibold text-slate-800">{resolveUserName(item.userId)}</div>
                    <div className="mt-1 font-mono text-[11px] text-slate-500" title={item.userId}>
                      {shortUuid(item.userId)}
                    </div>
                  </td>
                  <td className="break-all text-xs text-slate-600">{item.hymnId ?? "-"}</td>
                  <td>{item.part ?? "-"}</td>
                  <td className="break-all text-xs text-slate-600">{item.metadataJson ?? "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <div className="flex items-center justify-between text-sm text-slate-600">
        <span>
          페이지 {pagination.totalPages === 0 ? 0 : pagination.page} / {pagination.totalPages}, 전체 {pagination.total.toLocaleString("ko-KR")}건
        </span>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setPage((prev) => Math.max(prev - 1, 1))}
            disabled={loading || !pagination.hasPrevious}
            className="soy-btn soy-btn-secondary"
          >
            이전
          </button>
          <button
            type="button"
            onClick={() => setPage((prev) => prev + 1)}
            disabled={loading || !pagination.hasNext}
            className="soy-btn soy-btn-secondary"
          >
            다음
          </button>
        </div>
      </div>
    </div>
  );
}
