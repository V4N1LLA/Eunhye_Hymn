import { apiGet } from "./client";
import { getAccessToken } from "../auth/tokenStore";

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

export async function exportAdminEventsCsv(
  params: ExportAdminEventsCsvParams = {},
): Promise<ExportAdminEventsCsvResult> {
  const token = getAccessToken();
  if (!token) {
    throw new Error("인증 토큰이 없습니다. 다시 로그인해 주세요.");
  }

  const query = new URLSearchParams();
  if (params.eventType) query.set("eventType", params.eventType);
  if (params.userId) query.set("userId", params.userId);
  if (params.hymnId) query.set("hymnId", params.hymnId);
  if (params.from) query.set("from", params.from);
  if (params.to) query.set("to", params.to);
  if (params.limit != null) query.set("limit", String(params.limit));

  const suffix = query.toString();
  const path = suffix ? `/api/v1/admin/events/export?${suffix}` : "/api/v1/admin/events/export";

  const response = await fetch(path, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });

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
