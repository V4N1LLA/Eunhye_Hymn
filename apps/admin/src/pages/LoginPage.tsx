import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function isLocalDevHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
}

function LoginWaveBackground() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden">
      <div className="absolute -right-[320px] -top-[920px] sm:-right-[180px]">
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
      <div className="absolute -bottom-[430px] -left-[220px] sm:-bottom-[520px]">
        <svg width="968" height="896" viewBox="0 0 968 896" aria-hidden="true">
          <defs>
            <path
              id="login-wave-path-2"
              d="M896 448C1142.63 465.575 695.258 896 448 896C200.742 896 0 695.258 0 448C0 200.742 200.742 0 448 0C695.258 0 475 418 896 448Z"
            />
            <linearGradient id="login-wave-gradient-2" x1="0.5" y1="0" x2="0.5" y2="1">
              <stop offset="0" stopColor="#4f46e5" />
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
    <div className="relative min-h-screen overflow-hidden bg-[#eef2ff] px-4 py-8 md:py-12">
      <LoginWaveBackground />
      <div className="relative z-10 mx-auto w-full max-w-6xl">
        <div className="grid items-start gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <section className="rounded-[24px] border border-white/45 bg-white/65 p-6 shadow-[0_16px_50px_rgb(15_23_42/14%)] backdrop-blur-xl md:p-8">
            <div className="flex items-center gap-3">
              <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-indigo-600 text-lg font-bold text-white shadow-lg shadow-indigo-600/35">
                EH
              </span>
              <div>
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-indigo-700">Eunhye Hymn Admin</div>
                <div className="mt-0.5 text-sm font-semibold text-slate-900">운영 콘솔 로그인</div>
              </div>
            </div>

            <h1 className="mt-5 text-3xl font-semibold leading-tight text-slate-900">관리자 로그인</h1>
            <p className="mt-2 max-w-xl text-sm text-slate-600">
              단일 관리자 콘솔입니다. 관리자 ID/비밀번호로 로그인해 주세요.
            </p>

            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="rounded-2xl border border-slate-200/80 bg-white/90 px-4 py-3 shadow-sm">
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-500">Auth</div>
                <div className="mt-1 text-sm font-semibold text-slate-900">관리자 계정 로그인</div>
                <div className="mt-1 text-xs text-slate-500">ID/비밀번호 기반 인증</div>
              </div>
              <div className="rounded-2xl border border-slate-200/80 bg-white/90 px-4 py-3 shadow-sm">
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-500">Ops</div>
                <div className="mt-1 text-sm font-semibold text-slate-900">운영 메뉴 즉시 접근</div>
                <div className="mt-1 text-xs text-slate-500">로그인 후 화면 바로 이동</div>
              </div>
              <div className="rounded-2xl border border-slate-200/80 bg-white/90 px-4 py-3 shadow-sm">
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-500">Recover</div>
                <div className="mt-1 text-sm font-semibold text-slate-900">세션 초기화 지원</div>
                <div className="mt-1 text-xs text-slate-500">권한 오류 시 즉시 복구</div>
              </div>
            </div>

            <div className="mt-5 rounded-2xl border border-slate-200/80 bg-slate-50/90 px-4 py-3 text-sm text-slate-700">
              <div className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-500">연결 정보</div>
              <div className="mt-1 font-mono text-xs text-slate-600">{host}</div>
              <div className="mt-3 space-y-1 text-xs text-slate-500">
                <div>1. 관리자 계정 정보 입력</div>
                <div>2. 로그인 후 좌측 메뉴에서 운영 화면 이동</div>
                <div>3. 권한 문제가 있으면 세션 초기화 후 재로그인</div>
              </div>
            </div>
          </section>

          <section className="rounded-[24px] border border-white/45 bg-white/80 p-6 shadow-[0_18px_60px_rgb(15_23_42/18%)] backdrop-blur-xl md:p-8">
            <div className="flex items-start justify-between gap-3">
              <div>
                <h2 className="text-lg font-semibold text-slate-900">관리자 인증</h2>
                <p className="mt-1 text-sm text-slate-500">ID/비밀번호 로그인 전용</p>
              </div>
              <span className="inline-flex rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] font-semibold text-emerald-700">
                운영 접속
              </span>
            </div>

            {notice && <div className="soy-alert soy-alert-success mt-4">{notice}</div>}
            {error && <div className="soy-alert soy-alert-error mt-4">{error}</div>}

            <form onSubmit={handleSubmit} className="mt-4 space-y-3">
              <label className="block">
                <span className="soy-label">로그인 ID</span>
                <input
                  value={loginId}
                  onChange={(event) => setLoginId(event.target.value)}
                  autoComplete="username"
                  className="soy-input !rounded-xl !border-slate-200 !bg-white/90"
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
                  className="soy-input !rounded-xl !border-slate-200 !bg-white/90"
                  placeholder="********"
                />
              </label>

              <button
                type="submit"
                disabled={loading}
                className="soy-btn soy-btn-primary !w-full justify-center !rounded-xl !py-3 !text-sm !font-semibold"
              >
                {loading ? "로그인 중..." : "로그인"}
              </button>
            </form>

            <div className="mt-3 grid gap-2 sm:grid-cols-2">
              <button
                type="button"
                disabled={loading}
                onClick={handleResetSession}
                className="soy-btn soy-btn-secondary !w-full justify-center !rounded-xl"
              >
                세션 초기화
              </button>
              <Link
                to="/login/help"
                className="soy-btn soy-btn-ghost !w-full justify-center !rounded-xl !border !border-slate-200 !text-slate-600"
              >
                로그인 도움말 열기
              </Link>
            </div>
          </section>

          {showDevLogin && (
            <section className="rounded-[20px] border border-amber-200/80 bg-amber-50/95 p-6 shadow-sm lg:col-span-2">
              <div className="flex flex-wrap items-center justify-between gap-2">
                <div>
                  <h3 className="text-sm font-semibold text-amber-900">로컬 개발용 로그인</h3>
                  <p className="mt-1 text-xs text-amber-800">localhost 환경에서만 표시됩니다.</p>
                </div>
                <button
                  type="button"
                  onClick={() => setShowDevForm((prev) => !prev)}
                  className="soy-btn !rounded-lg !border !border-amber-300 !bg-white !px-3 !py-1.5 !text-xs !font-semibold !text-amber-800 hover:!bg-amber-100"
                >
                  {showDevForm ? "개발 폼 닫기" : "개발 폼 열기"}
                </button>
              </div>

              {showDevForm && (
                <form onSubmit={handleDevLogin} className="mt-3 flex flex-wrap items-end gap-2">
                  <label className="min-w-[220px] flex-1">
                    <span className="mb-1 block text-xs font-semibold text-amber-900">표시 이름</span>
                    <input
                      value={displayName}
                      onChange={(event) => setDisplayName(event.target.value)}
                      className="w-full rounded-lg border border-amber-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-2 focus:ring-amber-200"
                    />
                  </label>
                  <button
                    type="submit"
                    disabled={loading}
                    className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-700 disabled:opacity-50"
                  >
                    {loading ? "처리 중..." : "개발 로그인"}
                  </button>
                </form>
              )}
            </section>
          )}
        </div>
      </div>
    </div>
  );
}
