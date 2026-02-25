import { useEffect, useMemo, useState } from "react";
import { NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

type NavItem = {
  to: string;
  label: string;
  desc: string;
  matchPrefix?: string;
};

const NAV_ITEMS: NavItem[] = [
  { to: "/", label: "Dashboard", desc: "Overview and key metrics" },
  { to: "/hymns", label: "Hymns", desc: "Catalog and settings", matchPrefix: "/hymns" },
  { to: "/assets/upload", label: "Assets", desc: "Score and media upload", matchPrefix: "/assets" },
  { to: "/invite-codes", label: "Invite Codes", desc: "Create and revoke access", matchPrefix: "/invite-codes" },
  { to: "/users", label: "Users", desc: "Church / profile / role control", matchPrefix: "/users" },
  { to: "/profile-change-requests", label: "Profile Requests", desc: "Approve and reject", matchPrefix: "/profile-change-requests" },
  { to: "/events", label: "Audit Events", desc: "Activity query and exports", matchPrefix: "/events" },
  { to: "/ai/recommendations", label: "AI Recommendation", desc: "Situation-based hymn picks", matchPrefix: "/ai" },
  { to: "/help", label: "Help", desc: "Session and authorization guide", matchPrefix: "/help" },
];

function resolveCurrentSection(pathname: string): NavItem {
  if (pathname === "/") return NAV_ITEMS[0];
  return NAV_ITEMS.find((item) => item.matchPrefix && pathname.startsWith(item.matchPrefix)) ?? NAV_ITEMS[0];
}

function sidebarLinkClass(isActive: boolean): string {
  const base = "group flex items-start justify-between rounded-lg border px-3 py-2.5 text-sm transition";
  if (isActive) {
    return `${base} border-indigo-200 bg-indigo-50 text-indigo-900`;
  }
  return `${base} border-transparent text-slate-600 hover:border-slate-200 hover:bg-slate-50 hover:text-slate-900`;
}

export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [siderCollapsed, setSiderCollapsed] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const currentSection = useMemo(() => resolveCurrentSection(location.pathname), [location.pathname]);
  const host = window.location.host;
  const isAdmin = user?.role === "ADMIN";

  useEffect(() => {
    setMobileMenuOpen(false);
  }, [location.pathname]);

  const handleLogout = async () => {
    await logout();
    navigate("/login", { replace: true });
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
          <div className="flex h-14 items-center border-b border-slate-200 px-3">
            <NavLink to="/" className="flex min-w-0 items-center gap-2 overflow-hidden">
              <span className="inline-flex h-8 w-8 shrink-0 items-center justify-center rounded-md bg-indigo-100 text-sm font-bold text-indigo-700">
                EH
              </span>
              {!siderCollapsed && (
                <span className="truncate text-sm font-semibold text-slate-900">Eunhye Admin</span>
              )}
            </NavLink>
          </div>

          <div className="soy-subtle-scrollbar h-[calc(100%-56px)] overflow-y-auto px-2 py-2">
            <div className="mb-2 px-2 text-[11px] font-semibold uppercase tracking-[0.12em] text-slate-400">
              Workspace
            </div>
            <nav className="space-y-1">
              {NAV_ITEMS.map((item) => (
                <NavLink key={item.to} to={item.to} end={item.to === "/"}>
                  {({ isActive }) => (
                    <div className={sidebarLinkClass(isActive)}>
                      <div className="min-w-0">
                        <div className="truncate font-semibold">{siderCollapsed ? item.label.slice(0, 1) : item.label}</div>
                        {!siderCollapsed && <div className="mt-0.5 truncate text-xs text-slate-500">{item.desc}</div>}
                      </div>
                    </div>
                  )}
                </NavLink>
              ))}
            </nav>

            {!siderCollapsed && (
              <div className="mt-3 rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 text-xs text-slate-500">
                Host: <span className="font-mono text-slate-700">{host}</span>
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
                  aria-label="Toggle menu"
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
                  aria-label="Toggle sidebar width"
                >
                  {siderCollapsed ? ">" : "<"}
                </button>
                <div className="min-w-0">
                  <h1 className="truncate text-sm font-semibold text-slate-900 md:text-base">{currentSection.label}</h1>
                  <p className="truncate text-xs text-slate-500">{currentSection.desc}</p>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <span
                  className={`hidden rounded-full px-2 py-1 text-[11px] font-semibold md:inline-flex ${
                    isAdmin ? "bg-emerald-50 text-emerald-700" : "bg-amber-50 text-amber-700"
                  }`}
                >
                  {user?.role ?? "UNKNOWN"}
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="rounded-md bg-slate-900 px-3 py-1.5 text-xs font-semibold text-white hover:bg-slate-800"
                >
                  Logout
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
          aria-label="Close menu backdrop"
        />
      )}
    </div>
  );
}
