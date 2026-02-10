import { apiGet, apiPatch } from "./client";

export interface UserInfo {
  id: string;
  displayName: string;
  role: string;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
}

export interface UpdateUserRequest {
  role?: string;
  status?: string;
}

export function listUsers(): Promise<UserInfo[]> {
  return apiGet<UserInfo[]>("/admin/users");
}

export function updateUser(id: string, request: UpdateUserRequest): Promise<UserInfo> {
  return apiPatch<UserInfo>(`/admin/users/${id}`, request);
}
