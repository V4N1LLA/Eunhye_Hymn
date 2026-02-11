import { apiGet, apiPatch } from "./client";

export interface UserResponse {
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

export function listUsers(): Promise<UserResponse[]> {
  return apiGet<UserResponse[]>("/admin/users");
}

export function updateUser(id: string, req: UpdateUserRequest): Promise<UserResponse> {
  return apiPatch<UserResponse>(`/admin/users/${id}`, req);
}
