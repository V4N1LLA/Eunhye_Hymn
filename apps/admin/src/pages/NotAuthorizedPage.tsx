import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function NotAuthorizedPage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="flex min-h-[75vh] items-center justify-center">
      <div className="soy-panel w-full max-w-2xl p-6">
        <div className="soy-pill bg-amber-100 text-amber-800">Restricted Access</div>
        <h1 className="mt-3 text-2xl font-bold text-slate-900">Administrator role is required.</h1>
        <p className="mt-2 text-sm text-slate-600">
          Current role: <span className="font-mono font-semibold">{user?.role ?? "UNKNOWN"}</span> |
          Required role: <span className="font-mono font-semibold">ADMIN</span>
        </p>

        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
          <div className="font-semibold text-slate-900">Recovery Steps</div>
          <div className="mt-2 space-y-1 text-slate-600">
            <div>1. Reset current session token.</div>
            <div>2. Sign in again with administrator ID/password.</div>
            <div>3. If it still fails, verify deployed admin env credentials.</div>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          <button type="button" onClick={handleLogout} className="soy-btn soy-btn-primary">
            Reset Session and Sign In
          </button>
          <button type="button" onClick={() => navigate("/help")} className="soy-btn soy-btn-secondary">
            Open Help
          </button>
        </div>
      </div>
    </div>
  );
}
