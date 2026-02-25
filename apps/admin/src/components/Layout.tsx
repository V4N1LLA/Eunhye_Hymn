import { useEffect, useMemo, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type NavItem = {
  to: string;
  label: string;
  desc: string;
  matchPrefix?: string;
};

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "대시보드", desc: "운영 현황과 핵심 지표" },
  { to: "/hymns", label: "찬양 관리", desc: "카탈로그와 노출 상태", matchPrefix: "/hymns" },
  { to: "/assets/upload", label: "에셋 업로드", desc: "악보/미디 파일 등록", matchPrefix: "/assets" },
  { to: "/invite-codes", label: "초대 코드", desc: "코드 발급 및 비활성화", matchPrefix: "/invite-codes" },
  { to: "/users", label: "사용자 관리", desc: "교회/역할/프로필 관리", matchPrefix: "/users" },
  { to: "/profile-change-requests", label: "프로필 요청", desc: "변경 요청 승인/반려", matchPrefix: "/profile-change-requests" },
  { to: "/events", label: "감사 이벤트", desc: "사용 로그 조회 및 내보내기", matchPrefix: "/events" },
  { to: "/ai/recommendations", label: "AI 추천", desc: "상황 기반 찬양 추천", matchPrefix: "/ai" },
  { to: "/help", label: "도움말", desc: "권한/로그인 문제 해결", matchPrefix: "/help" },
];

type NotificationItem = {
  id: string;
  title: string;
  desc: string;
  to: string;
};

const NOTIFICATION_ITEMS: NotificationItem[] = [
  {
    id: "staging-check",
    title: "스테이징 점검 체크리스트",
    desc: "배포 후 API 상태와 운영 화면 동작을 빠르게 확인하세요.",
    to: "/help",
  },
  {
    id: "event-export",
    title: "감사 이벤트 내보내기",
    desc: "이벤트 화면에서 CSV 내보내기 작업 상태를 확인하세요.",
    to: "/events",
  },
];

function resolveCurrentSection(pathname: string): NavItem {
  if (pathname === "/") {
    return NAV_ITEMS[0];
  }
  return NAV_ITEMS.find((item) => item.matchPrefix && pathname.startsWith(item.matchPrefix)) ?? NAV_ITEMS[0];
}

function isNavItemActive(item: NavItem, pathname: string): boolean {
  if (item.to === "/") {
    return pathname === "/";
  }
  const matchBase = item.matchPrefix ?? item.to;
  return pathname.startsWith(matchBase);
}

function sidebarLinkClass(isActive: boolean): string {
  const base = "group flex items-start gap-3 rounded-md px-3 py-2.5 text-sm transition-colors";
  if (isActive) {
    return `${base} bg-indigo-500/20 text-indigo-100`;
  }
  return `${base} text-slate-300 hover:bg-white/10 hover:text-white`;
}

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

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [siderCollapsed, setSiderCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [notificationOpen, setNotificationOpen] = useState(false);
  const notificationRef = useRef<HTMLDivElement | null>(null);
  const currentSection = useMemo(() => resolveCurrentSection(location.pathname), [location.pathname]);
  const host = window.location.host;
  const isAdmin = user?.role === "ADMIN";
  const notificationCount = NOTIFICATION_ITEMS.length;

  useEffect(() => {
    setMobileMenuOpen(false);
    setNotificationOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!notificationOpen) {
      return;
    }

    const handleClickOutside = (event: MouseEvent) => {
      if (!notificationRef.current) {
        return;
      }
      if (notificationRef.current.contains(event.target as Node)) {
        return;
      }
      setNotificationOpen(false);
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setNotificationOpen(false);
      }
    };

    window.addEventListener("mousedown", handleClickOutside);
    window.addEventListener("keydown", handleEscape);
    return () => {
      window.removeEventListener("mousedown", handleClickOutside);
      window.removeEventListener("keydown", handleEscape);
    };
  }, [notificationOpen]);

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  const handleNotificationNavigate = (to: string) => {
    setNotificationOpen(false);
    navigate(to);
  };

  return (
    <div className="soy-layout-bg min-h-screen text-slate-900">
      <aside
        className={`fixed inset-y-0 left-0 z-50 w-[228px] border-r border-[#223454] bg-[#001529] shadow-xl transition-transform duration-200 ${
          mobileMenuOpen ? "translate-x-0" : "-translate-x-full"
        } ${
          siderCollapsed ? "lg:-translate-x-full lg:pointer-events-none" : "lg:translate-x-0 lg:pointer-events-auto"
        }`}
      >
        <div className="flex h-14 items-center border-b border-[#223454] px-4">
          <NavLink to="/" className="flex min-w-0 items-center gap-2 text-white">
            <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-indigo-500 text-sm font-bold">
              EH
            </span>
            <span className="truncate text-sm font-semibold">은혜찬양 관리자</span>
          </NavLink>
        </div>

        <div className="soy-subtle-scrollbar flex h-[calc(100%-56px)] flex-col overflow-y-auto px-2 py-2">
          <div className="px-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">메뉴</div>
          <nav className="mt-2 space-y-1">
            {NAV_ITEMS.map((item) => {
              const active = isNavItemActive(item, location.pathname);
              return (
                <NavLink key={item.to} to={item.to} end={item.to === "/"}>
                  <div className={sidebarLinkClass(active)}>
                    <span
                      className={`mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-md text-[11px] font-bold ${
                        active ? "bg-indigo-400/30 text-indigo-100" : "bg-white/10 text-slate-300"
                      }`}
                    >
                      {item.label.slice(0, 1)}
                    </span>
                    <div className="min-w-0">
                      <div className="truncate font-semibold">{item.label}</div>
                      <div className="truncate text-xs text-slate-400">{item.desc}</div>
                    </div>
                  </div>
                </NavLink>
              );
            })}
          </nav>

          <div className="mt-auto px-2 pb-2 pt-3">
            <div className="rounded-md border border-[#2a3f61] bg-[#0b213f] px-3 py-2 text-xs text-slate-300">
              접속 호스트 <span className="font-mono text-slate-100">{host}</span>
            </div>
          </div>
        </div>
      </aside>

      <div className={`min-h-screen transition-[padding-left] duration-200 ${siderCollapsed ? "lg:pl-0" : "lg:pl-[228px]"}`}>
        <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur">
          <div className="flex h-14 items-center justify-between gap-3 px-4 md:px-5">
            <div className="flex min-w-0 items-center gap-2">
              <button
                type="button"
                onClick={() => setMobileMenuOpen((prev) => !prev)}
                className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 lg:hidden"
                aria-label="메뉴 열기/닫기"
              >
                <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4">
                  <path
                    d="M4 7h16M4 12h16M4 17h16"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                  />
                </svg>
              </button>
              <button
                type="button"
                onClick={() => setSiderCollapsed((prev) => !prev)}
                className="hidden h-8 w-8 items-center justify-center rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50 lg:inline-flex"
                aria-label="사이드바 접기/펼치기"
                title={siderCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
              >
                {siderCollapsed ? ">" : "<"}
              </button>
              <div className="min-w-0">
                <h1 className="truncate text-sm font-semibold text-slate-900 md:text-base">{currentSection.label}</h1>
                <p className="truncate text-xs text-slate-500">{currentSection.desc}</p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <div className="relative" ref={notificationRef}>
                <button
                  type="button"
                  onClick={() => setNotificationOpen((prev) => !prev)}
                  className="relative inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50"
                  aria-label="알림"
                  title="알림"
                  aria-haspopup="dialog"
                  aria-expanded={notificationOpen}
                >
                  <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4">
                    <path
                      d="M15 17h5l-1.4-1.4A2 2 0 0 1 18 14.2V11a6 6 0 1 0-12 0v3.2a2 2 0 0 1-.6 1.4L4 17h5m6 0a3 3 0 0 1-6 0"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    />
                  </svg>
                  {notificationCount > 0 && (
                    <span className="absolute -right-1 -top-1 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
                      {notificationCount}
                    </span>
                  )}
                </button>

                {notificationOpen && (
                  <div className="absolute right-0 top-10 z-50 w-80 overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl">
                    <div className="border-b border-slate-200 px-3 py-2">
                      <div className="text-xs font-semibold uppercase tracking-[0.08em] text-slate-500">알림</div>
                      <div className="mt-0.5 text-xs text-slate-400">운영 바로가기 항목</div>
                    </div>
                    <div className="max-h-80 overflow-y-auto">
                      {NOTIFICATION_ITEMS.length === 0 ? (
                        <div className="px-3 py-6 text-center text-xs text-slate-500">새 알림이 없습니다.</div>
                      ) : (
                        NOTIFICATION_ITEMS.map((item) => (
                          <button
                            key={item.id}
                            type="button"
                            onClick={() => handleNotificationNavigate(item.to)}
                            className="block w-full border-b border-slate-100 px-3 py-3 text-left transition hover:bg-slate-50"
                          >
                            <div className="text-sm font-semibold text-slate-800">{item.title}</div>
                            <div className="mt-1 text-xs text-slate-500">{item.desc}</div>
                          </button>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
              <span
                className={`hidden rounded-full px-2 py-1 text-[11px] font-semibold md:inline-flex ${
                  isAdmin ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"
                }`}
              >
                {roleLabel(user?.role)}
              </span>
              <button
                type="button"
                onClick={handleLogout}
                className="rounded-md bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-slate-800"
              >
                로그아웃
              </button>
            </div>
          </div>
        </header>

        <main className="p-4 md:p-5">
          <Outlet />
        </main>
      </div>

      {mobileMenuOpen && (
        <button
          type="button"
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 z-40 bg-slate-900/45 lg:hidden"
          aria-label="메뉴 닫기 배경"
        />
      )}
    </div>
  );
}
