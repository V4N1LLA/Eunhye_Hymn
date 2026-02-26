import { type ReactNode, useEffect, useMemo, useRef, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type NavItem = {
  to: string;
  label: string;
  desc: string;
  matchPrefix?: string;
  icon: ReactNode;
};

type NotificationItem = {
  id: string;
  title: string;
  message: string;
  timeLabel: string;
  to: string;
  read: boolean;
};

function NavIcon({ path }: { path: string }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="h-4 w-4">
      <path
        d={path}
        fill="none"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

const NAV_ITEMS: NavItem[] = [
  {
    to: "/",
    label: "대시보드",
    desc: "운영 현황과 핵심 지표",
    icon: <NavIcon path="M4 11.5L12 5l8 6.5V20H4zM9.5 20v-5h5V20" />,
  },
  {
    to: "/hymns",
    label: "찬양 관리",
    desc: "카탈로그와 노출 상태",
    matchPrefix: "/hymns",
    icon: <NavIcon path="M9 18V7l9-2v11M9 12l9-2M9 18a2 2 0 1 1-4 0 2 2 0 0 1 4 0zm9-2a2 2 0 1 1-4 0 2 2 0 0 1 4 0z" />,
  },
  {
    to: "/assets/upload",
    label: "에셋 업로드",
    desc: "악보/미디어 등록",
    matchPrefix: "/assets",
    icon: <NavIcon path="M12 15V5m0 0l-4 4m4-4l4 4M5 15v4h14v-4" />,
  },
  {
    to: "/invite-codes",
    label: "초대 코드",
    desc: "코드 발급 및 비활성화",
    matchPrefix: "/invite-codes",
    icon: <NavIcon path="M8 7h8l3 3-8 8-3-3 1.8-1.8a2 2 0 0 0 0-2.8A2 2 0 0 0 6.8 8.6L8 7zM14 10l1 1" />,
  },
  {
    to: "/users",
    label: "사용자 관리",
    desc: "교회/역할/프로필 관리",
    matchPrefix: "/users",
    icon: <NavIcon path="M16.5 10a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5zM7.5 11A3.5 3.5 0 1 0 7.5 4a3.5 3.5 0 0 0 0 7zM3.5 20a4.5 4.5 0 0 1 8 0M13 20a3.5 3.5 0 0 1 7 0" />,
  },
  {
    to: "/profile-change-requests",
    label: "프로필 요청",
    desc: "변경 요청 승인/반려",
    matchPrefix: "/profile-change-requests",
    icon: <NavIcon path="M8 4h8v4h4v12H8zM16 4v4h4M11 13h6M11 16h6M11 10h3" />,
  },
  {
    to: "/events",
    label: "감사 이벤트",
    desc: "사용 로그 조회/내보내기",
    matchPrefix: "/events",
    icon: <NavIcon path="M5 6h14M5 12h14M5 18h10M3 6h.01M3 12h.01M3 18h.01" />,
  },
  {
    to: "/ai/recommendations",
    label: "AI 추천",
    desc: "상황 기반 찬양 추천",
    matchPrefix: "/ai",
    icon: <NavIcon path="M12 3l1.5 4.5L18 9l-4.5 1.5L12 15l-1.5-4.5L6 9l4.5-1.5L12 3zm6 12l.8 2.2L21 18l-2.2.8L18 21l-.8-2.2L15 18l2.2-.8L18 15zM6 14l.7 1.8L8.5 16l-1.8.7L6 18.5l-.7-1.8L3.5 16l1.8-.7L6 14z" />,
  },
  {
    to: "/help",
    label: "도움말",
    desc: "권한 및 로그인 가이드",
    matchPrefix: "/help",
    icon: <NavIcon path="M12 20a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM9.7 9a2.3 2.3 0 1 1 3.6 1.9c-.9.6-1.3 1.1-1.3 2.1M12 16h.01" />,
  },
];

const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: "ops-health",
    title: "운영 상태 점검",
    message: "이번 주 운영 사이클 로그가 갱신되었습니다. 점검 결과를 확인해 주세요.",
    timeLabel: "5분 전",
    to: "/events",
    read: false,
  },
  {
    id: "profile-requests",
    title: "프로필 요청",
    message: "검토 대기 중인 프로필 변경 요청이 있습니다.",
    timeLabel: "23분 전",
    to: "/profile-change-requests",
    read: false,
  },
  {
    id: "ai-recommend",
    title: "AI 추천",
    message: "최근 추천 응답 지표가 기록되었습니다.",
    timeLabel: "1시간 전",
    to: "/ai/recommendations",
    read: true,
  },
];

const NOTIFICATION_STORAGE_KEY = "adminNotifications";

function isNotificationItem(value: unknown): value is NotificationItem {
  if (!value || typeof value !== "object") return false;
  const row = value as NotificationItem;
  return (
    typeof row.id === "string" &&
    typeof row.title === "string" &&
    typeof row.message === "string" &&
    typeof row.timeLabel === "string" &&
    typeof row.to === "string" &&
    typeof row.read === "boolean"
  );
}

function loadNotificationsFromStorage(): NotificationItem[] {
  if (typeof window === "undefined") {
    return INITIAL_NOTIFICATIONS;
  }
  try {
    const raw = window.localStorage.getItem(NOTIFICATION_STORAGE_KEY);
    if (!raw) {
      return INITIAL_NOTIFICATIONS;
    }
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return INITIAL_NOTIFICATIONS;
    }
    const normalized = parsed.filter(isNotificationItem);
    if (normalized.length !== parsed.length) {
      return INITIAL_NOTIFICATIONS;
    }
    return normalized;
  } catch {
    return INITIAL_NOTIFICATIONS;
  }
}

function resolveCurrentSection(pathname: string): NavItem {
  if (pathname === "/") return NAV_ITEMS[0];
  return NAV_ITEMS.find((item) => item.matchPrefix && pathname.startsWith(item.matchPrefix)) ?? NAV_ITEMS[0];
}

function sidebarLinkClass(isActive: boolean): string {
  const base = "group flex items-center gap-2.5 rounded-lg border px-3 py-2.5 text-sm transition";
  if (isActive) {
    return `${base} border-indigo-200 bg-indigo-50 text-indigo-900`;
  }
  return `${base} border-transparent text-slate-600 hover:border-slate-200 hover:bg-slate-50 hover:text-slate-900`;
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
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>(() => loadNotificationsFromStorage());
  const notificationLayerRef = useRef<HTMLDivElement | null>(null);
  const currentSection = useMemo(() => resolveCurrentSection(location.pathname), [location.pathname]);
  const host = window.location.host;
  const isAdmin = user?.role === "ADMIN";
  const unreadNotificationCount = useMemo(
    () => notifications.filter((item) => !item.read).length,
    [notifications],
  );

  useEffect(() => {
    setMobileMenuOpen(false);
    setNotificationsOpen(false);
  }, [location.pathname]);

  useEffect(() => {
    if (!notificationsOpen) {
      return;
    }
    const handleClickOutside = (event: MouseEvent) => {
      if (!notificationLayerRef.current) {
        return;
      }
      if (event.target instanceof Node && !notificationLayerRef.current.contains(event.target)) {
        setNotificationsOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [notificationsOpen]);

  useEffect(() => {
    try {
      window.localStorage.setItem(NOTIFICATION_STORAGE_KEY, JSON.stringify(notifications));
    } catch {
      // ignore storage failures
    }
  }, [notifications]);

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
  };

  const dismissNotification = (targetId: string) => {
    setNotifications((prev) => prev.filter((item) => item.id !== targetId));
  };

  const openNotification = (target: NotificationItem) => {
    setNotifications((prev) =>
      prev.map((item) => (item.id === target.id ? { ...item, read: true } : item)),
    );
    setNotificationsOpen(false);
    navigate(target.to);
  };

  const markAllNotificationsRead = () => {
    setNotifications((prev) =>
      prev.map((item) => (item.read ? item : { ...item, read: true })),
    );
  };

  const siderWidthClass = siderCollapsed ? "lg:w-16" : "lg:w-[220px]";

  return (
    <div className="soy-layout-bg min-h-screen text-slate-900">
      <div className="min-h-screen lg:flex">
        <aside
          className={`fixed inset-y-0 left-0 z-40 w-[220px] border-r border-slate-200 bg-white shadow-[var(--sb-sider-shadow)] transition-transform lg:static ${siderWidthClass} ${
            mobileMenuOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
          }`}
        >
          <div className="flex h-14 items-center justify-end border-b border-slate-200 px-3">
            <button
              type="button"
              onClick={() => setSiderCollapsed((prev) => !prev)}
              className="inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50"
              aria-label="사이드바 너비 전환"
              title={siderCollapsed ? "사이드바 펼치기" : "사이드바 접기"}
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
          </div>

          <div className="soy-subtle-scrollbar h-[calc(100%-56px)] overflow-y-auto px-2 py-2">
            <div className="mb-2 px-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">
              메뉴
            </div>
            <nav className="space-y-1">
              {NAV_ITEMS.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.to === "/"}>
                  {({ isActive }) => (
                    <div
                      className={`${sidebarLinkClass(isActive)} ${siderCollapsed ? "justify-center px-2.5" : ""}`}
                      title={siderCollapsed ? item.label : undefined}
                    >
                      <span
                        className={`inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md transition ${
                          isActive
                            ? "bg-indigo-100 text-indigo-700"
                            : "bg-slate-100 text-slate-500 group-hover:bg-slate-200 group-hover:text-slate-700"
                        }`}
                      >
                        {item.icon}
                      </span>
                      {!siderCollapsed && (
                        <div className="min-w-0">
                          <div className="truncate font-semibold">{item.label}</div>
                          <div className="mt-0.5 truncate text-xs text-slate-500">{item.desc}</div>
                        </div>
                      )}
                    </div>
                  )}
                </NavLink>
              ))}
            </nav>

            {!siderCollapsed && (
              <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">
                접속 호스트: <span className="font-mono text-slate-700">{host}</span>
              </div>
            )}
          </div>
        </aside>

        <div className="min-h-screen min-w-0 flex-1">
          <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 shadow-[var(--sb-header-shadow)] backdrop-blur">
            <div className="flex h-14 items-center justify-between gap-3 px-3 md:px-5">
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
                <div className="min-w-0">
                  <h1 className="truncate text-sm font-semibold text-slate-900 md:text-base">{currentSection.label}</h1>
                  <p className="truncate text-xs text-slate-500">{currentSection.desc}</p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <div ref={notificationLayerRef} className="relative">
                  <button
                    type="button"
                    onClick={() => setNotificationsOpen((prev) => !prev)}
                    className="relative inline-flex h-8 w-8 items-center justify-center rounded-md border border-slate-200 text-slate-600 hover:bg-slate-50"
                    aria-label="알림"
                    aria-haspopup="menu"
                    aria-expanded={notificationsOpen}
                    title="알림"
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
                    {unreadNotificationCount > 0 && (
                      <span className="absolute -right-1 -top-1 inline-flex h-4 min-w-4 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-semibold text-white">
                        {unreadNotificationCount}
                      </span>
                    )}
                  </button>
                  {notificationsOpen && (
                    <div className="absolute right-0 top-10 z-40 w-[330px] overflow-hidden rounded-xl border border-slate-200 bg-white shadow-xl">
                      <div className="flex items-center justify-between border-b border-slate-200 px-3 py-2">
                        <p className="text-sm font-semibold text-slate-900">알림</p>
                        <button
                          type="button"
                          onClick={markAllNotificationsRead}
                          disabled={unreadNotificationCount === 0}
                          className="text-xs font-semibold text-indigo-600 disabled:cursor-not-allowed disabled:text-slate-400"
                        >
                          모두 읽음
                        </button>
                      </div>
                      {notifications.length === 0 ? (
                        <div className="px-3 py-5 text-center text-sm text-slate-500">표시할 알림이 없습니다.</div>
                      ) : (
                        <ul className="max-h-80 overflow-y-auto">
                          {notifications.map((item) => (
                            <li key={item.id} className="flex items-start gap-2 border-b border-slate-100 px-2 py-2 last:border-b-0">
                              <button
                                type="button"
                                onClick={() => openNotification(item)}
                                className={`min-w-0 flex-1 rounded-lg px-2 py-2 text-left transition ${
                                  item.read
                                    ? "bg-slate-50 text-slate-500 opacity-60"
                                    : "bg-indigo-50/40 text-slate-700 hover:bg-indigo-50"
                                }`}
                              >
                                <div className="flex items-center gap-2">
                                  <span className="truncate text-sm font-semibold">{item.title}</span>
                                  {!item.read && <span className="inline-block h-1.5 w-1.5 shrink-0 rounded-full bg-indigo-500" />}
                                </div>
                                <p className="mt-1 text-xs leading-relaxed">{item.message}</p>
                                <p className="mt-1 text-[11px] text-slate-400">{item.timeLabel}</p>
                              </button>
                              <button
                                type="button"
                                onClick={(event) => {
                                  event.stopPropagation();
                                  dismissNotification(item.id);
                                }}
                                className="inline-flex h-7 w-7 shrink-0 items-center justify-center rounded-md text-slate-400 hover:bg-slate-100 hover:text-slate-700"
                                aria-label={`${item.title} 알림 삭제`}
                                title="알림 삭제"
                              >
                                ×
                              </button>
                            </li>
                          ))}
                        </ul>
                      )}
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

          <main className="p-3 md:p-5">
            <Outlet />
          </main>
        </div>
      </div>

      {mobileMenuOpen && (
        <button
          type="button"
          onClick={() => setMobileMenuOpen(false)}
          className="fixed inset-0 z-30 bg-slate-900/40 lg:hidden"
          aria-label="메뉴 닫기 배경"
        />
      )}
    </div>
  );
}
