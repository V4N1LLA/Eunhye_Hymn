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
  const [displayName, setDisplayName] = useState("Admin");
  const [loading, setLoading] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [showDevForm, setShowDevForm] = useState(false);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!loginId.trim() || !password) {
      setError("Login ID and password are required.");
      return;
    }

    setLoading(true);
    setNotice(null);
    setError(null);
    try {
      await loginWithAdminPassword(loginId.trim(), password);
      navigate("/", { replace: true });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Login failed.");
    } finally {
      setLoading(false);
    }
  };

  const handleDevLogin = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!displayName.trim()) {
      setError("Display name is required.");
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
      setError(err instanceof Error ? err.message : "Dev login failed.");
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
      setNotice("Session cleared. Please sign in again with admin credentials.");
    } catch {
      setNotice("Session data reset complete.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="soy-layout-bg min-h-screen px-4 py-8">
      <div className="mx-auto grid w-full max-w-5xl gap-4 lg:grid-cols-[1.2fr_0.8fr]">
        <section className="soy-card p-6">
          <span className="inline-flex rounded-full bg-indigo-50 px-3 py-1 text-xs font-semibold text-indigo-700">
            Soybean-style Admin
          </span>
          <h1 className="mt-3 text-2xl font-semibold text-slate-900">Eunhye Admin Sign-in</h1>
          <p className="mt-2 text-sm text-slate-600">
            Single-operator admin console. Sign in using administrator ID and password.
          </p>

          <div className="mt-4 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
            <div className="text-xs font-semibold uppercase tracking-[0.1em] text-slate-500">Connection</div>
            <div className="mt-1 font-mono text-xs text-slate-600">{host}</div>
            <div className="mt-3 space-y-1 text-xs text-slate-500">
              <div>1. Enter admin credentials.</div>
              <div>2. Open user/event/hymn management from sidebar.</div>
              <div>3. If authorization is broken, reset session first.</div>
            </div>
          </div>
        </section>

        <section className="soy-card p-6">
          <h2 className="text-lg font-semibold text-slate-900">Administrator Login</h2>
          <p className="mt-1 text-sm text-slate-500">ID/password mode only</p>

          {notice && (
            <div className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
              {notice}
            </div>
          )}
          {error && (
            <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="mt-4 space-y-3">
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Login ID</span>
              <input
                value={loginId}
                onChange={(event) => setLoginId(event.target.value)}
                autoComplete="username"
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-200"
                placeholder="admin-id"
              />
            </label>
            <label className="block">
              <span className="mb-1 block text-sm font-medium text-slate-700">Password</span>
              <input
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                autoComplete="current-password"
                className="w-full rounded-md border border-slate-300 px-3 py-2 text-sm focus:border-indigo-400 focus:outline-none focus:ring-2 focus:ring-indigo-200"
                placeholder="••••••••"
              />
            </label>

            <button
              type="submit"
              disabled={loading}
              className="w-full rounded-md bg-indigo-600 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {loading ? "Signing in..." : "Sign In"}
            </button>
          </form>

          <button
            type="button"
            disabled={loading}
            onClick={handleResetSession}
            className="mt-3 w-full rounded-md border border-slate-300 bg-white py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
          >
            Reset Session
          </button>

          <div className="mt-4 text-xs text-slate-500">
            Need help?{" "}
            <Link to="/login/help" className="font-semibold text-indigo-700 underline underline-offset-2">
              Open login guide
            </Link>
          </div>
        </section>

        {showDevLogin && (
          <section className="soy-card border-amber-200 bg-amber-50 p-6 lg:col-span-2">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <h3 className="text-sm font-semibold text-amber-900">Local Development Login</h3>
                <p className="mt-1 text-xs text-amber-800">Enabled only on localhost.</p>
              </div>
              <button
                type="button"
                onClick={() => setShowDevForm((prev) => !prev)}
                className="rounded-md border border-amber-300 bg-white px-3 py-1.5 text-xs font-semibold text-amber-800 hover:bg-amber-100"
              >
                {showDevForm ? "Hide Dev Form" : "Open Dev Form"}
              </button>
            </div>

            {showDevForm && (
              <form onSubmit={handleDevLogin} className="mt-3 flex flex-wrap items-end gap-2">
                <label className="min-w-[220px] flex-1">
                  <span className="mb-1 block text-xs font-semibold text-amber-900">Display Name</span>
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
                  {loading ? "Processing..." : "Dev Login"}
                </button>
              </form>
            )}
          </section>
        )}
      </div>
    </div>
  );
}
