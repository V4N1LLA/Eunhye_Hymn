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
      <div className="w-full max-w-2xl rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="inline-flex rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-800">
          접근 제한
        </div>
        <h1 className="mt-3 text-2xl font-bold text-slate-900">관리자 권한이 확인되지 않았습니다</h1>
        <p className="mt-2 text-sm text-slate-600">
          현재 세션 role: <span className="font-mono font-semibold">{user?.role ?? "UNKNOWN"}</span> (필수 role:{" "}
          <span className="font-mono font-semibold">ADMIN</span>)
        </p>

        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
          <div className="font-semibold text-slate-900">빠른 복구 순서</div>
          <div className="mt-2 space-y-1 text-slate-600">
            <div>1) "세션 초기화 후 재로그인"을 눌러 토큰을 지웁니다.</div>
            <div>2) 관리자 ID/PW로 다시 로그인합니다.</div>
            <div>3) 계속 실패하면 서버 설정값(`ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`)을 확인합니다.</div>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={handleLogout}
            className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700"
          >
            세션 초기화 후 재로그인
          </button>
          <button
            type="button"
            onClick={() => navigate("/help")}
            className="rounded-lg border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50"
          >
            도움말 열기
          </button>
        </div>
      </div>
    </div>
  );
}

