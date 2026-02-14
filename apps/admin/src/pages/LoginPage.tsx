import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function isLocalDevHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
}

export default function LoginPage() {
  const { loginWithAdminPassword, login } = useAuth();
  const navigate = useNavigate();
  const host = window.location.host;
  const showDevLogin = isLocalDevHost(window.location.hostname);

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [showDevLoginForm, setShowDevLoginForm] = useState(false);
  const [displayName, setDisplayName] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginId.trim() || !password) {
      setError("아이디와 비밀번호를 입력하세요.");
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await loginWithAdminPassword(loginId.trim(), password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleDevLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!displayName.trim()) {
      setError("Display name is required.");
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await login({
        userId: crypto.randomUUID(),
        role: "ADMIN",
        displayName: displayName.trim(),
      });
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4 py-10">
      <div className="bg-white rounded-xl shadow-sm border border-slate-200 w-full max-w-md p-8">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-center text-slate-900">Eunhye Admin</h1>
          <p className="mt-2 text-center text-sm text-slate-600">
            운영자 전용 로그인 (host: <span className="font-mono">{host}</span>)
          </p>
        </div>

        {error && (
          <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-3">
          <div>
            <label htmlFor="loginId" className="block text-sm font-medium text-slate-700 mb-1">
              아이디
            </label>
            <input
              id="loginId"
              type="text"
              value={loginId}
              onChange={(e) => setLoginId(e.target.value)}
              placeholder="admin id"
              autoComplete="username"
              required
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-slate-700 mb-1">
              비밀번호
            </label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="password"
              autoComplete="current-password"
              required
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="w-full bg-indigo-600 text-white rounded-lg py-2 font-semibold hover:bg-indigo-700 disabled:opacity-50"
          >
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>

        <div className="mt-4 text-xs text-slate-500">
          로그인 문제가 있으면 <Link to="/help" className="underline">도움말</Link>을 확인하세요.
        </div>

        {showDevLogin && (
          <>
            <div className="flex items-center gap-3 my-4">
              <div className="flex-1 h-px bg-gray-300" />
              <span className="text-xs text-gray-400">or</span>
              <div className="flex-1 h-px bg-gray-300" />
            </div>

            <button
              type="button"
              onClick={() => setShowDevLoginForm((v) => !v)}
              className="w-full text-sm text-gray-500 hover:text-gray-700 mb-2"
            >
              {showDevLoginForm ? "Hide Dev Login" : "Dev Login (local only)"}
            </button>

            {showDevLoginForm && (
              <form onSubmit={handleDevLogin} className="flex flex-col gap-3">
                <div>
                  <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-1">
                    Display Name
                  </label>
                  <input
                    id="displayName"
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Admin"
                    className="w-full border border-gray-300 rounded px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
                <p className="text-xs text-gray-500">Dev login is enabled only on localhost.</p>
                <button
                  type="submit"
                  disabled={loading}
                  className="bg-gray-600 text-white rounded py-2 font-medium hover:bg-gray-700 disabled:opacity-50"
                >
                  {loading ? "Signing in..." : "Dev Sign In"}
                </button>
              </form>
            )}
          </>
        )}
      </div>
    </div>
  );
}

