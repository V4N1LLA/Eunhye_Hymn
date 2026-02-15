import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function isLocalDevHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
}

export default function LoginPage() {
  const { loginWithAdminPassword, login, logout } = useAuth();
  const navigate = useNavigate();
  const host = window.location.host;
  const showDevLogin = isLocalDevHost(window.location.hostname);

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const [showDevLoginForm, setShowDevLoginForm] = useState(false);
  const [displayName, setDisplayName] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginId.trim() || !password) {
      setError("아이디와 비밀번호를 입력하세요.");
      return;
    }

    setNotice(null);
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

    setNotice(null);
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

  const handleResetSession = async () => {
    setError(null);
    setNotice(null);
    setLoading(true);
    try {
      await logout();
      setNotice("이 브라우저의 기존 토큰을 초기화했습니다. 이제 관리자 계정으로 다시 로그인하세요.");
    } catch {
      setNotice("세션을 초기화했습니다. 다시 로그인해 주세요.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-100 px-4 py-8 md:py-12">
      <div className="mx-auto grid w-full max-w-5xl gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="inline-flex rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">
            Single Operator Mode
          </div>
          <h1 className="mt-3 text-2xl font-bold text-slate-900">Eunhye Admin 로그인</h1>
          <p className="mt-2 text-sm text-slate-600">
            운영자 1인 전용 관리 콘솔입니다. 서버에 설정된 관리자 ID/PW로만 로그인됩니다.
          </p>
          <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
            <div className="font-semibold text-slate-900">접속 정보</div>
            <div className="mt-1">host: <span className="font-mono">{host}</span></div>
            <div className="mt-3 space-y-1 text-slate-600">
              <div>1) 관리자 ID/PW 입력</div>
              <div>2) 로그인 후 상단/좌측 메뉴로 바로 이동</div>
              <div>3) 권한 오류 시 세션 초기화 후 재로그인</div>
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="mb-5">
            <h2 className="text-lg font-semibold text-slate-900">관리자 인증</h2>
            <p className="mt-1 text-sm text-slate-600">OAuth 없이 ID/PW만 사용합니다.</p>
          </div>

          {notice && (
            <div className="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {notice}
            </div>
          )}

          {error && (
            <div className="mb-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-3">
            <div>
              <label htmlFor="loginId" className="mb-1 block text-sm font-medium text-slate-700">
                아이디
              </label>
              <input
                id="loginId"
                type="text"
                value={loginId}
                onChange={(e) => setLoginId(e.target.value)}
                placeholder="운영자 아이디"
                autoComplete="username"
                required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <div>
              <label htmlFor="password" className="mb-1 block text-sm font-medium text-slate-700">
                비밀번호
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="운영자 비밀번호"
                autoComplete="current-password"
                required
                className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-lg bg-indigo-600 py-2 font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>
          </form>

          <button
            type="button"
            onClick={handleResetSession}
            disabled={loading}
            className="mt-3 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            세션 초기화 후 다시 로그인
          </button>

          <div className="mt-4 text-xs text-slate-500">
            로그인 문제가 있으면 <Link to="/login/help" className="font-semibold underline">도움말</Link>을 확인하세요.
          </div>
        </section>

        {showDevLogin && (
          <section className="rounded-2xl border border-amber-200 bg-amber-50 p-6 shadow-sm">
            <div className="text-sm font-semibold text-amber-800">로컬 개발용 로그인</div>
            <p className="mt-1 text-xs text-amber-700">localhost에서만 사용됩니다. 스테이징/운영에서는 사용하지 마세요.</p>

            <button
              type="button"
              onClick={() => setShowDevLoginForm((v) => !v)}
              className="mt-3 rounded-lg border border-amber-300 px-3 py-2 text-sm font-semibold text-amber-800 hover:bg-amber-100"
            >
              {showDevLoginForm ? "개발 로그인 닫기" : "개발 로그인 열기"}
            </button>

            {showDevLoginForm && (
              <form onSubmit={handleDevLogin} className="mt-3 flex flex-col gap-3">
                <div>
                  <label htmlFor="displayName" className="mb-1 block text-sm font-medium text-amber-900">
                    Display Name
                  </label>
                  <input
                    id="displayName"
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="Admin"
                    className="w-full rounded-lg border border-amber-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-amber-500"
                  />
                </div>
                <button
                  type="submit"
                  disabled={loading}
                  className="rounded-lg bg-amber-600 py-2 font-semibold text-white hover:bg-amber-700 disabled:opacity-50"
                >
                  {loading ? "처리 중..." : "Dev Login"}
                </button>
              </form>
            )}
          </section>
        )}
      </div>
    </div>
  );
}

