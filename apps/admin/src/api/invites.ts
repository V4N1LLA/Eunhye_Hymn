import { apiGet, apiPost } from "./client";

export interface InviteCode {
  id: string;
  code: string;
  maxUses: number | null;
  usedCount: number;
  expiresAt: string | null;
  revokedAt: string | null;
  createdAt: string;
}

export interface CreateInviteRequest {
  code: string;
  maxUses?: number;
  expiresAt?: string;
}

export function listInvites(): Promise<InviteCode[]> {
  return apiGet<InviteCode[]>("/admin/invites");
}

export function createInvite(request: CreateInviteRequest): Promise<InviteCode> {
  return apiPost<InviteCode>("/admin/invites", request);
}

export function revokeInvite(id: string): Promise<InviteCode> {
  return apiPost<InviteCode>(`/admin/invites/${id}/revoke`, {});
}
