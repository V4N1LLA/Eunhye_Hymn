import { apiDelete, apiGet, apiPatch, apiPost } from "./client";

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

export interface CreateUserRequest {
  displayName: string;
  role?: string;
  status?: string;
}

export function listUsers(): Promise<UserResponse[]> {
  return apiGet<UserResponse[]>("/admin/users");
}

export function createUser(req: CreateUserRequest): Promise<UserResponse> {
  return apiPost<UserResponse>("/admin/users", req);
}

export function updateUser(id: string, req: UpdateUserRequest): Promise<UserResponse> {
  return apiPatch<UserResponse>(`/admin/users/${id}`, req);
}

export function deleteUser(id: string): Promise<UserResponse> {
  return apiDelete<UserResponse>(`/admin/users/${id}`);
}
