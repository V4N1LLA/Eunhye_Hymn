import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `block px-4 py-2 rounded ${isActive ? "bg-indigo-700 text-white" : "text-indigo-100 hover:bg-indigo-600"}`;

  return (
    <div className="flex h-screen bg-gray-100">
      {/* Sidebar */}
      <aside className="w-56 bg-indigo-800 text-white flex flex-col">
        <div className="px-4 py-5 text-lg font-bold tracking-wide">
          Eunhye Admin
        </div>
        <nav className="flex-1 flex flex-col gap-1 px-2">
          <NavLink to="/hymns" className={linkClass}>
            찬양 관리
          </NavLink>
          <NavLink to="/assets/upload" className={linkClass}>
            에셋 업로드
          </NavLink>
          <NavLink to="/invites" className={linkClass}>
            초대코드
          </NavLink>
        </nav>
        <div className="px-4 py-4 border-t border-indigo-700 text-sm">
          <div className="text-indigo-200 mb-2">{user?.role ?? ""}</div>
          <button
            type="button"
            onClick={handleLogout}
            className="w-full text-left text-indigo-200 hover:text-white"
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
