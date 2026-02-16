import { apiDelete, apiGet, apiPost } from "./client";

export interface InviteCodeResponse {
  code: string;
  createdBy: string | null;
  description: string | null;
  maxUses: number | null;
  usedCount: number;
  enabled: boolean;
  expiresAt: string | null;
  createdAt: string;
}

export interface CreateInviteCodeRequest {
  code: string;
  description?: string;
  maxUses?: number | null;
  expiresAt?: string | null;
}

export function listInviteCodes(): Promise<InviteCodeResponse[]> {
  return apiGet<InviteCodeResponse[]>("/admin/invite-codes");
}

export function createInviteCode(req: CreateInviteCodeRequest): Promise<InviteCodeResponse> {
  return apiPost<InviteCodeResponse>("/admin/invite-codes", req);
}

export function revokeInviteCode(code: string): Promise<void> {
  return apiDelete<void>(`/admin/invite-codes/${encodeURIComponent(code)}`);
}
