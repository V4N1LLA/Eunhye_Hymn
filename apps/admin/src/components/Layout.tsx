import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type NavItem = {
  to: string;
  label: string;
  description: string;
};

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "홈", description: "대시보드" },
  { to: "/hymns", label: "찬양 관리", description: "목록/편집/상태" },
  { to: "/assets/upload", label: "에셋 업로드", description: "이미지/악보/음원" },
  { to: "/invite-codes", label: "초대코드", description: "생성/비활성화" },
  { to: "/users", label: "사용자", description: "역할/상태" },
  { to: "/events", label: "감사 로그/분석", description: "조회/내보내기" },
  { to: "/help", label: "도움말", description: "로그인/권한" },
];

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const host = window.location.host;

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const linkClass = ({ isActive }: { isActive: boolean }) => {
    const base = "block rounded-lg px-3 py-2 transition";
    if (isActive) {
      return `${base} bg-white text-indigo-900 shadow-sm`;
    }
    return `${base} text-indigo-100 hover:bg-indigo-700/60`;
  };

  return (
    <div className="flex min-h-screen bg-slate-50">
      {/* Sidebar */}
      <aside className="w-72 bg-indigo-900 text-white flex flex-col">
        <div className="px-4 pt-5 pb-4">
          <NavLink to="/" className="block rounded-lg px-2 py-2 hover:bg-indigo-800/60">
            <div className="text-lg font-bold tracking-wide">Eunhye Admin</div>
            <div className="mt-1 text-xs text-indigo-200 font-mono">{host}</div>
          </NavLink>
        </div>

        <nav className="flex-1 flex flex-col gap-1 px-3">
          {NAV_ITEMS.map((item) => (
            <NavLink key={item.to} to={item.to} className={linkClass} end={item.to === "/"}>
              <div className="flex items-center justify-between gap-2">
                <div className="font-medium">{item.label}</div>
              </div>
              <div className="mt-0.5 text-xs text-indigo-200/80">{item.description}</div>
            </NavLink>
          ))}
        </nav>

        <div className="px-4 py-4 border-t border-indigo-800 text-sm space-y-2">
          <div className="flex items-center justify-between">
            <div className="text-indigo-200">Role</div>
            <div className="rounded-full bg-indigo-800 px-2 py-0.5 text-xs font-semibold">
              {user?.role ?? "-"}
            </div>
          </div>
          <button
            type="button"
            onClick={handleLogout}
            className="w-full rounded-lg border border-indigo-700 px-3 py-2 text-left text-indigo-100 hover:bg-indigo-800/60"
          >
            로그아웃
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto p-6">
        <Outlet />
      </main>
    </div>
  );
}
