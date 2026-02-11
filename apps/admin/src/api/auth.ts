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
  provider: "GOOGLE" | "KAKAO";
  token: string;
  inviteCode?: string;
}

export interface SocialLoginResponse {
  accessToken: string;
  refreshToken: string;
  newUser: boolean;
}

export function socialLogin(req: SocialLoginRequest): Promise<SocialLoginResponse> {
  return apiPost<SocialLoginResponse>("/auth/social", req);
}

export function validateInviteCode(code: string): Promise<{ valid: boolean }> {
  return apiPost<{ valid: boolean }>("/auth/invite/validate", { code });
}

export function devLogin(req: DevLoginRequest): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/auth/dev/login", req);
}

export function refreshToken(token: string): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/auth/refresh", { refreshToken: token });
}

export function logout(token: string): Promise<void> {
  return apiPost<void>("/auth/logout", { refreshToken: token });
}
