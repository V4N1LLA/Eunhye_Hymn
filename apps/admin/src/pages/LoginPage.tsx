import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function isLocalDevHost(hostname: string): boolean {
  return hostname === "localhost" || hostname === "127.0.0.1";
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
    <div className="relative min-h-screen overflow-hidden bg-slate-100 px-4 py-8 md:py-10">
      <div className="pointer-events-none absolute -left-20 top-0 h-80 w-80 rounded-full bg-indigo-300/20 blur-3xl" />
      <div className="pointer-events-none absolute right-0 top-12 h-96 w-96 rounded-full bg-sky-300/20 blur-3xl" />
      <div className="pointer-events-none absolute bottom-0 left-1/3 h-72 w-72 rounded-full bg-violet-300/20 blur-3xl" />

      <div className="relative mx-auto grid w-full max-w-6xl gap-5 lg:grid-cols-[1.2fr_0.8fr]">
        <section className="soy-card relative overflow-hidden p-6 md:p-8">
          <div className="pointer-events-none absolute -right-12 top-0 h-56 w-56 rounded-full bg-indigo-200/40 blur-3xl" />
          <div className="relative">
            <span className="inline-flex items-center rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">
              Eunhye Hymn Admin
            </span>
            <h1 className="mt-3 text-2xl font-semibold text-slate-900 md:text-3xl">관리자 로그인</h1>
            <p className="mt-2 max-w-xl text-sm text-slate-600">
              단일 관리자 콘솔입니다. 관리자 ID/비밀번호로 로그인해 주세요.
            </p>

            <div className="mt-6 grid gap-3 sm:grid-cols-3">
              <div className="rounded-xl border border-slate-200 bg-white/90 px-4 py-3">
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-500">Auth</div>
                <div className="mt-1 text-sm font-semibold text-slate-900">관리자 계정 로그인</div>
                <div className="mt-1 text-xs text-slate-500">ID/비밀번호 기반 인증</div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white/90 px-4 py-3">
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-500">Ops</div>
                <div className="mt-1 text-sm font-semibold text-slate-900">운영 메뉴 즉시 접근</div>
                <div className="mt-1 text-xs text-slate-500">로그인 후 화면 바로 이동</div>
              </div>
              <div className="rounded-xl border border-slate-200 bg-white/90 px-4 py-3">
                <div className="text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-500">Recover</div>
                <div className="mt-1 text-sm font-semibold text-slate-900">세션 초기화 지원</div>
                <div className="mt-1 text-xs text-slate-500">권한 오류 시 즉시 복구</div>
              </div>
            </div>

            <div className="mt-5 rounded-xl border border-slate-200 bg-slate-50/80 px-4 py-3 text-sm text-slate-700">
              <div className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-500">연결 정보</div>
              <div className="mt-1 font-mono text-xs text-slate-600">{host}</div>
              <div className="mt-3 space-y-1 text-xs text-slate-500">
                <div>1. 관리자 계정 정보 입력</div>
                <div>2. 로그인 후 좌측 메뉴에서 운영 화면 이동</div>
                <div>3. 권한 문제가 있으면 세션 초기화 후 재로그인</div>
              </div>
            </div>
          </div>
        </section>

        <section className="soy-card p-6 md:p-8">
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

            <button type="submit" disabled={loading} className="soy-btn soy-btn-primary !w-full justify-center !py-3">
              {loading ? "로그인 중..." : "로그인"}
            </button>
          </form>

          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            <button
              type="button"
              disabled={loading}
              onClick={handleResetSession}
              className="soy-btn soy-btn-secondary !w-full justify-center"
            >
              세션 초기화
            </button>
            <Link to="/login/help" className="soy-btn soy-btn-ghost !w-full justify-center !border !border-slate-200">
              로그인 도움말 열기
            </Link>
          </div>
        </section>

        {showDevLogin && (
          <section className="soy-card border-amber-200 bg-amber-50/90 p-6 lg:col-span-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-semibold text-amber-900">로컬 개발용 로그인</h3>
                <p className="mt-1 text-xs text-amber-800">localhost 환경에서만 표시됩니다.</p>
              </div>
              <button
                type="button"
                onClick={() => setShowDevForm((prev) => !prev)}
                className="soy-btn !rounded-md !border !border-amber-300 !bg-white !px-3 !py-1.5 !text-xs !font-semibold !text-amber-800 hover:!bg-amber-100"
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
        )}
      </div>
    </div>
  );
}
