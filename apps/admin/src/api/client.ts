const API_BASE = "/api/v1";

function getToken(): string | null {
  return localStorage.getItem("accessToken");
}

function authHeaders(): Record<string, string> {
  const token = getToken();
  if (token) {
    return { Authorization: `Bearer ${token}` };
  }
  return {};
}

let refreshPromise: Promise<boolean> | null = null;

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
      const data = await response.json();
      if (!data.data) return false;
      localStorage.setItem("accessToken", data.data.accessToken);
      localStorage.setItem("refreshToken", data.data.refreshToken);
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
  localStorage.removeItem("accessToken");
  localStorage.removeItem("refreshToken");
  window.location.href = "/login";
  throw new Error("인증이 만료되었습니다.");
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (response.status === 401) {
    return redirectToLogin();
  }

  const data = await response.json();

  if (!response.ok || data?.success === false) {
    const message = data?.error?.message ?? "요청 처리 중 오류가 발생했습니다.";
    throw new Error(message);
  }

  return data.data as T;
}

interface RequestOptions {
  method: string;
  path: string;
  body?: unknown;
}

async function apiFetch<T>(opts: RequestOptions): Promise<T> {
  const doFetch = () => {
    const headers: Record<string, string> = { ...authHeaders() };
    if (opts.body !== undefined) {
      headers["Content-Type"] = "application/json";
    }
    return fetch(`${API_BASE}${opts.path}`, {
      method: opts.method,
      headers,
      body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
    });
  };

  const response = await doFetch();

  if (response.status === 401) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      const retryResponse = await doFetch();
      return handleResponse<T>(retryResponse);
    }
    return redirectToLogin();
  }

  return handleResponse<T>(response);
}

export async function apiGet<T>(path: string): Promise<T> {
  return apiFetch<T>({ method: "GET", path });
}

export async function apiPost<T>(path: string, body?: unknown): Promise<T> {
  return apiFetch<T>({ method: "POST", path, body: body ?? null });
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  return apiFetch<T>({ method: "PATCH", path, body });
}

export async function apiDelete<T>(path: string): Promise<T> {
  return apiFetch<T>({ method: "DELETE", path });
}
