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

export function devLogin(req: DevLoginRequest): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/auth/dev/login", req);
}

export function refreshToken(token: string): Promise<TokenResponse> {
  return apiPost<TokenResponse>("/auth/refresh", { refreshToken: token });
}

export function logout(token: string): Promise<void> {
  return apiPost<void>("/auth/logout", { refreshToken: token });
}
