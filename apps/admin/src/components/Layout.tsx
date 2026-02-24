import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type NavItem = {
  to: string;
  label: string;
  description: string;
  matchPrefix?: string;
};

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "대시보드", description: "운영 상태 요약 보기" },
  {
    to: "/hymns",
    label: "찬양 관리",
    description: "찬양 목록/편집/상태",
    matchPrefix: "/hymns",
  },
  {
    to: "/assets/upload",
    label: "에셋 업로드",
    description: "이미지/악보/음원 등록",
    matchPrefix: "/assets",
  },
  {
    to: "/invite-codes",
    label: "초대코드 관리",
    description: "생성/사용/비활성화",
    matchPrefix: "/invite-codes",
  },
  {
    to: "/users",
    label: "사용자 관리",
    description: "권한/상태 관리",
    matchPrefix: "/users",
  },
  {
    to: "/profile-change-requests",
    label: "개인정보 변경 요청",
    description: "승인/반려 처리",
    matchPrefix: "/profile-change-requests",
  },
  {
    to: "/events",
    label: "감사 로그/분석",
    description: "조회/내보내기/지표",
    matchPrefix: "/events",
  },
  {
    to: "/ai/recommendations",
    label: "AI 추천",
    description: "상황 기반 찬송 추천",
    matchPrefix: "/ai",
  },
  {
    to: "/help",
    label: "운영 도움말",
    description: "로그인/권한 문제 해결",
    matchPrefix: "/help",
  },
];

function resolveCurrentSection(pathname: string): NavItem {
  if (pathname === "/") {
    return NAV_ITEMS[0];
  }

  const matched = NAV_ITEMS.find(
    (item) => item.matchPrefix && pathname.startsWith(item.matchPrefix),
  );
  return matched ?? NAV_ITEMS[0];
}

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const host = window.location.host;
  const currentSection = resolveCurrentSection(location.pathname);
  const isAdmin = user?.role === "ADMIN";

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  const sidebarLinkClass = ({ isActive }: { isActive: boolean }) => {
    const base = "block rounded-xl border px-3 py-3 transition";
    if (isActive) {
      return `${base} border-indigo-300 bg-white text-indigo-900 shadow-sm`;
    }
    return `${base} border-transparent text-indigo-100 hover:border-indigo-500 hover:bg-indigo-700/70`;
  };

  const mobileLinkClass = ({ isActive }: { isActive: boolean }) => {
    const base =
      "rounded-lg px-3 py-1.5 text-xs font-semibold whitespace-nowrap transition";
    if (isActive) {
      return `${base} bg-indigo-600 text-white`;
    }
    return `${base} bg-slate-100 text-slate-700 hover:bg-slate-200`;
  };

  return (
    <div className="relative min-h-screen bg-gradient-to-b from-slate-100 via-slate-100 to-indigo-50/40 text-slate-900">
      <div className="pointer-events-none absolute -left-20 top-16 h-64 w-64 rounded-full bg-indigo-200/40 blur-3xl" />
      <div className="pointer-events-none absolute right-0 top-0 h-56 w-56 rounded-full bg-violet-200/35 blur-3xl" />
      <div className="relative flex min-h-screen">
        <aside className="relative hidden w-80 shrink-0 flex-col overflow-hidden bg-gradient-to-b from-indigo-950 via-indigo-900 to-indigo-900 text-white lg:flex">
          <div className="pointer-events-none absolute -right-20 top-0 h-56 w-56 rounded-full bg-violet-400/25 blur-3xl" />
          <div className="border-b border-indigo-800 px-5 py-5">
            <NavLink
              to="/"
              className="block rounded-xl border border-indigo-700/70 bg-indigo-800/60 px-3 py-3 hover:bg-indigo-700/70"
            >
              <div className="text-lg font-bold tracking-wide">Eunhye Admin</div>
              <div className="mt-1 text-xs text-indigo-100/80">
                클릭하면 홈으로 이동
              </div>
              <div className="mt-2 text-xs font-mono text-indigo-200">
                {host}
              </div>
            </NavLink>
          </div>

          <nav className="flex-1 space-y-2 px-4 py-4">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={sidebarLinkClass}
                end={item.to === "/"}
              >
                <div className="text-sm font-semibold">{item.label}</div>
                <div className="mt-1 text-xs text-indigo-200">
                  {item.description}
                </div>
              </NavLink>
            ))}
          </nav>

          <div className="space-y-3 border-t border-indigo-800 px-5 py-4 text-sm">
            <div className="rounded-xl border border-indigo-700 bg-indigo-800/60 px-3 py-2">
              <div className="text-xs text-indigo-200">운영 모드</div>
              <div className="mt-1 font-semibold">온라인 운영 모드</div>
            </div>
            <div className="rounded-xl border border-indigo-700 bg-indigo-800/60 px-3 py-2">
              <div className="flex items-center justify-between text-xs text-indigo-200">
                <span>권한</span>
                <span
                  className={`rounded-full px-2 py-0.5 font-semibold ${
                    isAdmin
                      ? "bg-emerald-500/20 text-emerald-100"
                      : "bg-amber-500/20 text-amber-100"
                  }`}
                >
                  {user?.role ?? "-"}
                </span>
              </div>
              <div className="mt-1 truncate text-xs text-indigo-100">
                {user?.userId ?? "세션 없음"}
              </div>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              className="w-full rounded-xl border border-indigo-600 px-3 py-2 text-left font-semibold text-indigo-100 hover:bg-indigo-800/80"
            >
              로그아웃
            </button>
          </div>
        </aside>

        <div className="flex min-h-screen flex-1 flex-col">
          <header className="sticky top-0 z-20 border-b border-slate-200 bg-gradient-to-r from-white/95 to-indigo-50/70 backdrop-blur">
            <div className="flex items-start justify-between gap-4 px-4 py-3 md:px-6">
              <div>
                <div className="text-xs font-semibold text-indigo-600">ADMIN</div>
                <h1 className="text-lg font-bold text-slate-900 md:text-xl">
                  {currentSection.label}
                </h1>
                <p className="mt-0.5 text-xs text-slate-500 md:text-sm">
                  {currentSection.description}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <NavLink
                  to="/"
                  className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50"
                >
                  홈
                </NavLink>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-lg bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-slate-800 lg:hidden"
                >
                  로그아웃
                </button>
              </div>
            </div>
            <nav className="flex gap-2 overflow-x-auto border-t border-slate-200 px-4 py-2 lg:hidden">
              {NAV_ITEMS.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={mobileLinkClass}
                  end={item.to === "/"}
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>
          </header>

          <main className="flex-1 p-4 md:p-6">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}
