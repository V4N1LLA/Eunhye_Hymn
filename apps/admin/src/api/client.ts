const API_BASE = "/api/v1";

type UnauthorizedMode = "redirect" | "throw";

export interface ApiRequestOptions {
  unauthorized?: UnauthorizedMode;
  includeAuth?: boolean;
}

interface ApiEnvelope<T = unknown> {
  success?: boolean;
  data?: T;
  error?: {
    message?: string;
  };
}

function getToken(): string | null {
  return localStorage.getItem("accessToken");
}

function authHeaders(includeAuth: boolean): Record<string, string> {
  if (!includeAuth) {
    return {};
  }

  const token = getToken();
  if (token) {
    return { Authorization: `Bearer ${token}` };
  }
  return {};
}

let refreshPromise: Promise<boolean> | null = null;

function clearTokens(): void {
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
}

async function tryRefreshToken(): Promise<boolean> {
  if (refreshPromise) return refreshPromise;

  const rt = localStorage.getItem("refreshToken");
  if (!rt) return false;

  refreshPromise = (async () => {
    try {
      const response = await fetch(`${API_BASE}/auth/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken: rt }),
      });

      if (!response.ok) return false;

      const payload = (await response.json()) as ApiEnvelope<{
        accessToken?: string;
        refreshToken?: string;
      }>;
      const tokens = payload.data;
      if (!tokens?.accessToken || !tokens?.refreshToken) return false;

      localStorage.setItem("accessToken", tokens.accessToken);
      localStorage.setItem("refreshToken", tokens.refreshToken);
      return true;
    } catch {
      return false;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

function redirectToLogin(): never {
  clearTokens();
  window.location.href = "/login";
  throw new Error("인증이 만료되었습니다.");
}

function resolveErrorMessage(payload: ApiEnvelope | null, fallback: string): string {
  return payload?.error?.message ?? fallback;
}

async function handleResponse<T>(response: Response, unauthorized: UnauthorizedMode): Promise<T> {
  const payload = (await response.json().catch(() => null)) as ApiEnvelope<T> | null;

  if (response.status === 401) {
    if (unauthorized === "redirect") {
      return redirectToLogin();
    }
    throw new Error(resolveErrorMessage(payload, "인증에 실패했습니다."));
  }

  if (!response.ok || payload?.success === false) {
    throw new Error(resolveErrorMessage(payload, "요청 처리 중 오류가 발생했습니다."));
  }

  return payload?.data as T;
}

interface RequestOptions extends ApiRequestOptions {
  method: "GET" | "POST" | "PATCH" | "DELETE";
  path: string;
  body?: unknown;
}

async function apiFetch<T>({
  method,
  path,
  body,
  includeAuth = true,
  unauthorized = "redirect",
}: RequestOptions): Promise<T> {
  const doFetch = () => {
    const headers: Record<string, string> = { ...authHeaders(includeAuth) };
    if (body !== undefined) {
      headers["Content-Type"] = "application/json";
    }

    return fetch(`${API_BASE}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  };

  const response = await doFetch();

  if (response.status === 401 && includeAuth && unauthorized === "redirect") {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      const retryResponse = await doFetch();
      return handleResponse<T>(retryResponse, unauthorized);
    }
  }

  return handleResponse<T>(response, unauthorized);
}

export async function apiGet<T>(path: string, options?: ApiRequestOptions): Promise<T> {
  return apiFetch<T>({ method: "GET", path, ...options });
}

export async function apiPost<T>(path: string, body?: unknown, options?: ApiRequestOptions): Promise<T> {
  return apiFetch<T>({ method: "POST", path, body, ...options });
}

export async function apiPatch<T>(path: string, body: unknown, options?: ApiRequestOptions): Promise<T> {
  return apiFetch<T>({ method: "PATCH", path, body, ...options });
}

export async function apiDelete<T>(path: string, options?: ApiRequestOptions): Promise<T> {
  return apiFetch<T>({ method: "DELETE", path, ...options });
}
