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
    <div className="min-h-[70vh] flex items-center justify-center">
      <div className="w-full max-w-lg rounded-xl border border-slate-200 bg-white p-6 shadow-sm">
        <h1 className="text-xl font-bold text-slate-900">관리자 권한이 없습니다</h1>
        <p className="mt-2 text-sm text-slate-600">
          현재 로그인된 계정의 role이 <span className="font-mono">{user?.role ?? "UNKNOWN"}</span> 입니다.
          Admin 기능은 <span className="font-mono">ADMIN</span> role이 필요합니다.
        </p>

        <div className="mt-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-700">
          <div className="font-semibold">해결 방법</div>
          <div className="mt-1 space-y-1 text-slate-600">
            <div>- 로그아웃 후 다시 로그인하세요.</div>
            <div>- (운영자 1인 운영) 서버에서 운영자 allowlist(카카오 ID)를 설정해 ADMIN으로 발급되게 구성하세요.</div>
          </div>
        </div>

        <div className="mt-5 flex gap-2">
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
          >
            로그아웃
          </button>
          <button
            type="button"
            onClick={() => navigate("/help")}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            도움말
          </button>
        </div>
      </div>
    </div>
  );
}

