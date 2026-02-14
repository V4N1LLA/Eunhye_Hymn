import { FormEvent, useEffect, useMemo, useState } from "react";
import {
  exportAdminEventsCsv,
  listAdminEvents,
  type AdminEventItem,
  type AdminEventSummary,
  type EventType,
} from "../api/adminEvents";

const EVENT_TYPE_OPTIONS: Array<{ label: string; value: "all" | EventType }> = [
  { label: "전체", value: "all" },
  { label: "열람", value: "HYMN_OPENED" },
  { label: "파트 재생", value: "PART_PLAYED" },
  { label: "메모 저장", value: "NOTE_SAVED" },
  { label: "즐겨찾기", value: "FAVORITE_TOGGLED" },
];

const SUMMARY_DAY_PRESETS = [1, 7, 30, 60, 90];
const MIN_SUMMARY_DAYS = 1;
const MAX_SUMMARY_DAYS = 90;
const SIZE_OPTIONS = [20, 50, 100, 200];

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

export default function AdminEventPage() {
  const [eventType, setEventType] = useState<"all" | EventType>("all");
  const [userId, setUserId] = useState("");
  const [hymnId, setHymnId] = useState("");
  const [fromLocal, setFromLocal] = useState("");
  const [toLocal, setToLocal] = useState("");
  const [summaryDays, setSummaryDays] = useState(7);
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

  useEffect(() => {
    void fetchEvents(page);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page]);

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

  return (
    <div>
      <h1 className="text-2xl font-bold mb-6">감사 로그/분석</h1>

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
          {SUMMARY_DAY_PRESETS.map((days) => (
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
              <th className="px-4 py-3 text-sm font-semibold text-gray-600">userId</th>
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
                <td className="px-4 py-3 text-xs text-gray-600 break-all">{item.userId}</td>
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
