import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function roleLabel(role: string | null | undefined): string {
  switch ((role ?? "").trim().toUpperCase()) {
    case "ADMIN":
      return "관리자";
    case "USER":
      return "일반 사용자";
    default:
      return "미확인";
  }
}

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
        <div className="soy-pill bg-amber-100 text-amber-800">접근 제한</div>
        <h1 className="mt-3 text-2xl font-bold text-slate-900">관리자 권한이 필요합니다.</h1>
        <p className="mt-2 text-sm text-slate-600">
          현재 권한: <span className="font-semibold">{roleLabel(user?.role)}</span> | 필요 권한:{" "}
          <span className="font-semibold">관리자</span>
        </p>

        <div className="mt-4 rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
          <div className="font-semibold text-slate-900">복구 절차</div>
          <div className="mt-2 space-y-1 text-slate-600">
            <div>1. 세션을 초기화합니다.</div>
            <div>2. 관리자 ID/비밀번호로 다시 로그인합니다.</div>
            <div>3. 계속 실패하면 배포 서버 환경변수를 확인합니다.</div>
          </div>
        </div>

        <div className="mt-5 flex flex-wrap gap-2">
          <button type="button" onClick={handleLogout} className="soy-btn soy-btn-primary">
            세션 초기화 후 재로그인
          </button>
          <button type="button" onClick={() => navigate("/help")} className="soy-btn soy-btn-secondary">
            도움말 열기
          </button>
        </div>
      </div>
    </div>
  );
}
