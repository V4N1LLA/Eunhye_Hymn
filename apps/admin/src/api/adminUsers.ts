import { apiDelete, apiGet, apiPatch, apiPost } from "./client";

export interface UserIdentitySummary {
  provider: string | null;
  providerSubjectMasked: string | null;
  email: string | null;
  emailMasked: string | null;
  createdAt: string;
}

export interface UserProfileSummary {
  churchName: string | null;
  name: string | null;
  group: string | null;
  gender: string | null;
  updatedAt: string | null;
}

export interface UserVerificationSummary {
  phoneNumber: string | null;
  phoneVerifiedAt: string | null;
  phoneVerified: boolean;
  updatedAt: string | null;
}

export interface UserResponse {
  id: string;
  displayName: string;
  role: string;
  status: string;
  createdAt: string;
  lastLoginAt: string | null;
  primaryEmail: string | null;
  profile: UserProfileSummary | null;
  verification: UserVerificationSummary | null;
  identities: UserIdentitySummary[];
}

export interface UpdateUserRequest {
  role?: string;
  status?: string;
  displayName?: string;
  churchName?: string;
  name?: string;
  group?: string;
  gender?: string;
  phoneNumber?: string;
}

export interface CreateUserRequest {
  displayName: string;
  role?: string;
  status?: string;
}

export interface ListUsersParams {
  churchName?: string;
}

export function listUsers(params?: ListUsersParams): Promise<UserResponse[]> {
  const normalizedChurch = params?.churchName?.trim();
  const path = normalizedChurch
    ? `/admin/users?churchName=${encodeURIComponent(normalizedChurch)}`
    : "/admin/users";
  return apiGet<UserResponse[]>(path);
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
