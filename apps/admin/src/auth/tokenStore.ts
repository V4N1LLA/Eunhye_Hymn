const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";

function getSessionStorage(): Storage | null {
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

function getLocalStorage(): Storage | null {
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

export function migrateLegacyLocalStorageTokens(): void {
  const session = getSessionStorage();
  const local = getLocalStorage();
  if (!session || !local) {
    return;
  }

  const legacyAccess = local.getItem(ACCESS_TOKEN_KEY);
  const legacyRefresh = local.getItem(REFRESH_TOKEN_KEY);

  if (legacyAccess && !session.getItem(ACCESS_TOKEN_KEY)) {
    session.setItem(ACCESS_TOKEN_KEY, legacyAccess);
  }
  if (legacyRefresh && !session.getItem(REFRESH_TOKEN_KEY)) {
    session.setItem(REFRESH_TOKEN_KEY, legacyRefresh);
  }

  // Reduce persistence risk by removing legacy tokens from localStorage.
  local.removeItem(ACCESS_TOKEN_KEY);
  local.removeItem(REFRESH_TOKEN_KEY);
}

export function getAccessToken(): string | null {
  return getSessionStorage()?.getItem(ACCESS_TOKEN_KEY) ?? null;
}

export function getRefreshToken(): string | null {
  return getSessionStorage()?.getItem(REFRESH_TOKEN_KEY) ?? null;
}

export function setTokens(accessToken: string, refreshToken: string): void {
  const session = getSessionStorage();
  if (!session) {
    return;
  }
  session.setItem(ACCESS_TOKEN_KEY, accessToken);
  session.setItem(REFRESH_TOKEN_KEY, refreshToken);
}

export function clearTokens(): void {
  const session = getSessionStorage();
  if (!session) {
    return;
  }
  session.removeItem(ACCESS_TOKEN_KEY);
  session.removeItem(REFRESH_TOKEN_KEY);
}

