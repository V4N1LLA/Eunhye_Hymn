import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  adminPasswordLogin as apiAdminPasswordLogin,
  devLogin as apiDevLogin,
  logout as apiLogout,
  socialLogin as apiSocialLogin,
  type DevLoginRequest,
} from "../api/auth";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  migrateLegacyLocalStorageTokens,
  setTokens,
} from "./tokenStore";

interface User {
  userId: string;
  role: string;
}

interface AuthContextValue {
  isAuthenticated: boolean;
  user: User | null;
  login: (req: DevLoginRequest) => Promise<void>;
  loginWithAdminPassword: (loginId: string, password: string) => Promise<{ newUser: boolean }>;
  loginWithSocial: (provider: "GOOGLE" | "KAKAO", token: string, inviteCode?: string) => Promise<{ newUser: boolean }>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function parseJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const base64 = token.split(".")[1];
    // JWT uses base64url, but atob expects base64.
    const normalized = base64.replace(/-/g, "+").replace(/_/g, "/").padEnd(Math.ceil(base64.length / 4) * 4, "=");
    const json = atob(normalized);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

function loadUser(): User | null {
  const token = getAccessToken();
  if (!token) return null;
  const payload = parseJwtPayload(token);
  if (!payload) return null;
  const userId = payload.sub ?? payload.userId;
  const role = payload.role ?? "USER";
  if (typeof userId !== "string" || typeof role !== "string") {
    return null;
  }
  return {
    userId,
    role,
  };
}

migrateLegacyLocalStorageTokens();

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(loadUser);

  const setTokensAndUser = useCallback((accessToken: string, refreshToken: string, fallbackUser?: User) => {
    setTokens(accessToken, refreshToken);
    const parsed = parseJwtPayload(accessToken);
    const userId = parsed?.sub ?? parsed?.userId;
    const role = parsed?.role ?? "USER";
    setUser(
      parsed && typeof userId === "string" && typeof role === "string"
        ? { userId, role }
        : fallbackUser ?? null,
    );
  }, []);

  const login = useCallback(async (req: DevLoginRequest) => {
    const tokens = await apiDevLogin(req);
    setTokensAndUser(tokens.accessToken, tokens.refreshToken, { userId: req.userId, role: req.role });
  }, [setTokensAndUser]);

  const loginWithAdminPassword = useCallback(async (loginId: string, password: string) => {
    const result = await apiAdminPasswordLogin({ loginId, password });
    const payload = parseJwtPayload(result.accessToken);
    const role = payload?.role;
    if (role !== "ADMIN") {
      clearTokens();
      setUser(null);
      throw new Error("관리자 권한 토큰이 아닙니다. 세션을 초기화한 뒤 관리자 계정으로 다시 로그인해 주세요.");
    }
    setTokensAndUser(result.accessToken, result.refreshToken);
    return { newUser: result.newUser };
  }, [setTokensAndUser]);

  const loginWithSocial = useCallback(async (provider: "GOOGLE" | "KAKAO", token: string, inviteCode?: string) => {
    const result = await apiSocialLogin({ provider, token, inviteCode });
    setTokensAndUser(result.accessToken, result.refreshToken);
    return { newUser: result.newUser };
  }, [setTokensAndUser]);

  const logout = useCallback(async () => {
    const rt = getRefreshToken();
    if (rt) {
      try {
        await apiLogout(rt);
      } catch {
        // ignore logout errors
      }
    }
    clearTokens();
    setUser(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      isAuthenticated: user !== null,
      user,
      login,
      loginWithAdminPassword,
      loginWithSocial,
      logout,
    }),
    [user, login, loginWithAdminPassword, loginWithSocial, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}

