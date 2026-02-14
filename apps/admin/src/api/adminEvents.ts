import { apiFetchRaw, apiGet, apiPost } from "./client";

export type EventType = "HYMN_OPENED" | "PART_PLAYED" | "NOTE_SAVED" | "FAVORITE_TOGGLED";

export interface AdminEventItem {
  id: string;
  userId: string;
  eventType: EventType;
  hymnId: string | null;
  part: "S" | "A" | "T" | "B" | "ALL" | null;
  metadataJson: string | null;
  createdAt: string;
}

export interface EventTypeCount {
  eventType: EventType;
  count: number;
}

export interface AdminEventSummary {
  fromInclusive: string;
  toExclusive: string;
  total: number;
  byType: EventTypeCount[];
}

export interface AdminEventListResponse {
  items: AdminEventItem[];
  summary: AdminEventSummary;
  pagination: {
    page: number;
    size: number;
    total: number;
    totalPages: number;
    hasPrevious: boolean;
    hasNext: boolean;
  };
}

export interface ListAdminEventsParams {
  eventType?: EventType;
  userId?: string;
  hymnId?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
  limit?: number;
  summaryDays?: number;
}

export function listAdminEvents(params: ListAdminEventsParams = {}): Promise<AdminEventListResponse> {
  const query = new URLSearchParams();

  if (params.eventType) query.set("eventType", params.eventType);
  if (params.userId) query.set("userId", params.userId);
  if (params.hymnId) query.set("hymnId", params.hymnId);
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  if (params.page != null) query.set("page", String(params.page));
  if (params.size != null) query.set("size", String(params.size));
  if (params.limit != null) query.set("limit", String(params.limit));
  if (params.summaryDays != null) query.set("summaryDays", String(params.summaryDays));

  const suffix = query.toString();
  const path = suffix ? `/admin/events?${suffix}` : "/admin/events";
  return apiGet<AdminEventListResponse>(path);
}

export interface ExportAdminEventsCsvParams {
  eventType?: EventType;
  userId?: string;
  hymnId?: string;
  from?: string;
  to?: string;
  limit?: number;
}

export interface ExportAdminEventsCsvResult {
  blob: Blob;
  filename: string;
}

export type EventExportJobStatus = "QUEUED" | "RUNNING" | "COMPLETED" | "FAILED";

export interface AdminEventExportJob {
  id: string;
  status: EventExportJobStatus;
  exportLimit: number;
  rowCount: number | null;
  fileName: string | null;
  errorMessage: string | null;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
  statusUrl: string;
  downloadUrl: string;
  downloadable: boolean;
}

export interface AdminEventExportOpsMetrics {
  windowDays: number;
  fromInclusive: string;
  toExclusive: string;
  jobs: {
    total: number;
    queued: number;
    running: number;
    completed: number;
    failed: number;
    failureRatePercent: number;
  };
  processing: {
    measuredJobs: number;
    averageSeconds: number;
    p95Seconds: number;
  };
  cleanup: {
    runCount: number;
    deletedJobs: number;
  };
}

export interface CreateAdminEventExportJobParams {
  eventType?: EventType;
  userId?: string;
  hymnId?: string;
  from?: string;
  to?: string;
  limit?: number;
}

export async function exportAdminEventsCsv(
  params: ExportAdminEventsCsvParams = {},
): Promise<ExportAdminEventsCsvResult> {
  const query = new URLSearchParams();
  if (params.eventType) query.set("eventType", params.eventType);
  if (params.userId) query.set("userId", params.userId);
  if (params.hymnId) query.set("hymnId", params.hymnId);
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  if (params.limit != null) query.set("limit", String(params.limit));

  const suffix = query.toString();
  const path = suffix ? `/admin/events/export?${suffix}` : "/admin/events/export";
  const response = await apiFetchRaw({ method: "GET", path });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as
      | { error?: { message?: string } }
      | null;
    throw new Error(payload?.error?.message ?? "CSV 내보내기에 실패했습니다.");
  }

  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
  const filename = match?.[1] ?? "admin-events.csv";

  return { blob, filename };
}

export function createAdminEventExportJob(
  params: CreateAdminEventExportJobParams = {},
): Promise<AdminEventExportJob> {
  const query = new URLSearchParams();
  if (params.eventType) query.set("eventType", params.eventType);
  if (params.userId) query.set("userId", params.userId);
  if (params.hymnId) query.set("hymnId", params.hymnId);
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  if (params.limit != null) query.set("limit", String(params.limit));

  const suffix = query.toString();
  const path = suffix ? `/admin/events/export-jobs?${suffix}` : "/admin/events/export-jobs";
  return apiPost<AdminEventExportJob>(path);
}

export function getAdminEventExportJob(jobId: string): Promise<AdminEventExportJob> {
  return apiGet<AdminEventExportJob>(`/admin/events/export-jobs/${jobId}`);
}

export function getAdminEventExportOpsMetrics(days?: number): Promise<AdminEventExportOpsMetrics> {
  const query = new URLSearchParams();
  if (days != null) {
    query.set("days", String(days));
  }
  const suffix = query.toString();
  const path = suffix ? `/admin/events/export-jobs/metrics?${suffix}` : "/admin/events/export-jobs/metrics";
  return apiGet<AdminEventExportOpsMetrics>(path);
}

export async function downloadAdminEventExportJobCsv(jobId: string): Promise<ExportAdminEventsCsvResult> {
  const response = await apiFetchRaw({ method: "GET", path: `/admin/events/export-jobs/${jobId}/download` });

  if (!response.ok) {
    const payload = (await response.json().catch(() => null)) as
      | { error?: { message?: string } }
      | null;
    throw new Error(payload?.error?.message ?? "비동기 CSV 다운로드에 실패했습니다.");
  }

  const blob = await response.blob();
  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = disposition.match(/filename=\"?([^\";]+)\"?/i);
  const filename = match?.[1] ?? "admin-events-async.csv";

  return { blob, filename };
}
