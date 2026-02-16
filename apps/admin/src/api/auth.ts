import { apiPost } from "./client";

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
}

export interface DevLoginRequest {
  userId: string;
  role: "USER" | "ADMIN";
  displayName: string;
}

export interface SocialLoginRequest {
  provider: "KAKAO";
  token: string;
  inviteCode?: string;
}

export interface SocialLoginResponse {
  accessToken: string;
  refreshToken: string;
  newUser: boolean;
}

export interface AdminPasswordLoginRequest {
  loginId: string;
  password: string;
}

export interface AdminPasswordChangeRequest {
  currentPassword: string;
  newLoginId: string;
  newPassword: string;
}

export interface AdminPasswordChangeResponse {
  loginId: string;
  updatedAt: string;
}

const PUBLIC_AUTH_OPTIONS = {
  includeAuth: false,
  unauthorized: "throw",
} as const;

export function socialLogin(req: SocialLoginRequest): Promise<SocialLoginResponse> {
  return apiPost<SocialLoginResponse>("/auth/social", req, PUBLIC_AUTH_OPTIONS);
}

export function adminPasswordLogin(req: AdminPasswordLoginRequest): Promise<SocialLoginResponse> {
  return apiPost<SocialLoginResponse>("/auth/admin/login", req, PUBLIC_AUTH_OPTIONS);
}

export function changeAdminPasswordCredential(req: AdminPasswordChangeRequest): Promise<AdminPasswordChangeResponse> {
  return apiPost<AdminPasswordChangeResponse>("/admin/auth/password", req);
}

export function validateInviteCode(code: string): Promise<{ valid: boolean }> {
  return apiPost<{ valid: boolean }>("/auth/invite/validate", { code }, PUBLIC_AUTH_OPTIONS);
}

export function devLogin(req: DevLoginRequest): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/auth/dev/login", req, PUBLIC_AUTH_OPTIONS);
}

export function refreshToken(token: string): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/auth/refresh", { refreshToken: token }, PUBLIC_AUTH_OPTIONS);
}

export function logout(token: string): Promise<void> {
  return apiPost<void>("/auth/logout", { refreshToken: token }, PUBLIC_AUTH_OPTIONS);
}
