import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function isLocalDevHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
}

function LoginWaveBackground() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute -right-[300px] -top-[900px] sm:-right-[160px] sm:-top-[860px]">
        <svg width="1337" height="1337" viewBox="0 0 1337 1337" aria-hidden="true">
          <defs>
            <path
              id="login-wave-path-1"
              d="M1337 668.5C1337 1037.46 1037.46 1337 668.5 1337C523.673 1337 337 1236 370.5 1094C434.038 824.673 0 892.628 0 668.5C0 299.545 299.545 0 668.5 0C1037.46 0 1337 299.545 1337 668.5Z"
            />
            <linearGradient id="login-wave-gradient-1" x1="0.79" y1="0.62" x2="0.21" y2="0.86">
              <stop offset="0" stopColor="#c7d2fe" />
              <stop offset="1" stopColor="#6366f1" />
            </linearGradient>
          </defs>
          <use xlinkHref="#login-wave-path-1" fill="url(#login-wave-gradient-1)" />
        </svg>
      </div>
      <div className="absolute -bottom-[420px] -left-[220px] sm:-bottom-[510px] sm:-left-[170px]">
        <svg width="968" height="896" viewBox="0 0 968 896" aria-hidden="true">
          <defs>
            <path
              id="login-wave-path-2"
              d="M896 448C1142.63 465.575 695.258 896 448 896C200.742 896 0 695.258 0 448C0 200.742 200.742 0 448 0C695.258 0 475 418 896 448Z"
            />
            <linearGradient id="login-wave-gradient-2" x1="0.5" y1="0" x2="0.5" y2="1">
              <stop offset="0" stopColor="#6366f1" />
              <stop offset="1" stopColor="#c7d2fe" />
            </linearGradient>
          </defs>
          <use xlinkHref="#login-wave-path-2" fill="url(#login-wave-gradient-2)" />
        </svg>
      </div>
    </div>
  );
}

export default function LoginPage() {
  const { loginWithAdminPassword, login, logout } = useAuth();
  const navigate = useNavigate();
  const showDevLogin = isLocalDevHost(window.location.hostname);
  const host = window.location.host;

  const [loginId, setLoginId] = useState("");
  const [password, setPassword] = useState("");
  const [displayName, setDisplayName] = useState("관리자");
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showDevForm, setShowDevForm] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!loginId.trim() || !password) {
      setError("관리자 로그인 ID와 비밀번호를 입력해 주세요.");
      return;
    }

    setLoading(true);
    setNotice(null);
    setError(null);
    try {
      await loginWithAdminPassword(loginId.trim(), password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleDevLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!displayName.trim()) {
      setError("개발 로그인 표시 이름을 입력해 주세요.");
      return;
    }

    setLoading(true);
    setNotice(null);
    setError(null);
    try {
      await login({
        userId: crypto.randomUUID(),
        role: "ADMIN",
        displayName: displayName.trim(),
      });
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "개발 로그인에 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  const handleResetSession = async () => {
    setLoading(true);
    setNotice(null);
    setError(null);
    try {
      await logout();
      setNotice("세션을 초기화했습니다. 관리자 계정으로 다시 로그인해 주세요.");
    } catch {
      setNotice("세션 초기화를 완료했습니다.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="relative min-h-screen overflow-hidden bg-[#ecebff]">
      <LoginWaveBackground />

      <div className="relative z-10 flex min-h-screen items-center justify-center px-4 py-10">
        <section className="w-full max-w-[420px] rounded-2xl border border-white/80 bg-white/92 p-6 shadow-[0_16px_40px_rgb(15_23_42/14%)] backdrop-blur md:p-7">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-center gap-2">
              <span className="inline-flex h-10 w-10 items-center justify-center rounded-xl bg-indigo-600 text-base font-bold text-white shadow-md shadow-indigo-500/30">
                EH
              </span>
              <div>
                <div className="text-sm font-semibold text-indigo-600">은혜찬양 관리자</div>
                <div className="mt-0.5 text-xs text-slate-500">비밀번호 로그인</div>
              </div>
            </div>
            <div className="inline-flex items-center gap-1 text-slate-400">
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-slate-200 text-xs">☼</span>
              <span className="inline-flex h-7 w-7 items-center justify-center rounded-md border border-slate-200 text-xs">A</span>
            </div>
          </div>

          <h1 className="mt-5 text-lg font-semibold text-slate-900">관리자 로그인</h1>
          <p className="mt-1 text-sm text-slate-500">관리자 ID/비밀번호로 로그인해 주세요.</p>

          {notice && <div className="soy-alert soy-alert-success mt-4">{notice}</div>}
          {error && <div className="soy-alert soy-alert-error mt-4">{error}</div>}

          <form onSubmit={handleSubmit} className="mt-4 space-y-3">
            <label className="block">
              <span className="soy-label">로그인 ID</span>
              <input
                value={loginId}
                onChange={(event) => setLoginId(event.target.value)}
                autoComplete="username"
                className="soy-input"
                placeholder="admin-id"
              />
            </label>
            <label className="block">
              <span className="soy-label">비밀번호</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                className="soy-input"
                placeholder="********"
              />
            </label>

            <button
              type="submit"
              disabled={loading}
              className="soy-btn soy-btn-primary !mt-1 !w-full !justify-center !rounded-lg !py-2.5"
            >
              {loading ? "로그인 중..." : "로그인"}
            </button>
          </form>

          <div className="mt-2 flex items-center justify-between gap-2">
            <button
              type="button"
              disabled={loading}
              onClick={handleResetSession}
              className="text-xs font-semibold text-slate-500 hover:text-slate-700"
            >
              세션 초기화
            </button>
            <Link to="/login/help" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700">
              로그인 도움말
            </Link>
          </div>

          <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-[11px] text-slate-500">
            접속 호스트: <span className="font-mono">{host}</span>
          </div>
        </section>
      </div>

      {showDevLogin && (
        <div className="relative z-10 -mt-4 pb-10">
          <section className="mx-auto w-full max-w-[420px] rounded-2xl border border-amber-200 bg-amber-50/95 p-4 shadow-sm">
            <div className="flex items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-semibold text-amber-900">로컬 개발용 로그인</h3>
                <p className="mt-0.5 text-xs text-amber-800">localhost 환경에서만 표시됩니다.</p>
              </div>
              <button
                type="button"
                onClick={() => setShowDevForm((prev) => !prev)}
                className="rounded-md border border-amber-300 bg-white px-2.5 py-1 text-xs font-semibold text-amber-800 hover:bg-amber-100"
              >
                {showDevForm ? "닫기" : "열기"}
              </button>
            </div>

            {showDevForm && (
              <form onSubmit={handleDevLogin} className="mt-3 flex items-end gap-2">
                <label className="min-w-0 flex-1">
                  <span className="mb-1 block text-xs font-semibold text-amber-900">표시 이름</span>
                  <input
                    value={displayName}
                    onChange={(event) => setDisplayName(event.target.value)}
                    className="w-full rounded-md border border-amber-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-200"
                  />
                </label>
                <button
                  type="submit"
                  disabled={loading}
                  className="rounded-md bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-700 disabled:opacity-50"
                >
                  {loading ? "처리 중..." : "개발 로그인"}
                </button>
              </form>
            )}
          </section>
        </div>
      )}
    </div>
  );
}
