import { apiGet, apiPatch, apiPost } from "./client";

export interface HymnResponse {
  id: string;
  title: string;
  number: number | null;
  tags: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface HymnDetailResponse {
  id: string;
  title: string;
  number: number | null;
  tags: string | null;
  enabled: boolean;
  createdAt: string;
  assets: AssetResponse[];
}

export interface AssetResponse {
  id: string;
  hymnId: string;
  type: "PDF" | "AUDIO";
  part: "S" | "A" | "T" | "B" | "ALL";
  url: string;
  objectKey: string;
}

export interface CreateHymnRequest {
  title: string;
  number?: number | null;
  tags?: string | null;
  enabled?: boolean;
}

export interface UpdateHymnRequest {
  title?: string | null;
  number?: number | null;
  tags?: string | null;
  enabled?: boolean | null;
}

export function listHymns(): Promise<HymnResponse[]> {
  return apiGet<HymnResponse[]>("/admin/hymns");
}

export function createHymn(req: CreateHymnRequest): Promise<HymnResponse> {
  return apiPost<HymnResponse>("/admin/hymns", req);
}

export function updateHymn(id: string, req: UpdateHymnRequest): Promise<HymnResponse> {
  return apiPatch<HymnResponse>(`/admin/hymns/${id}`, req);
}

export function getHymnDetail(id: string): Promise<HymnDetailResponse> {
  return apiGet<HymnDetailResponse>(`/hymns/${id}`);
}
