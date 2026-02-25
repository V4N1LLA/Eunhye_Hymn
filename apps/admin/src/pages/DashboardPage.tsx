import { useCallback, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { listInviteCodes } from "../api/adminInviteCodes";
import { listAdminEvents } from "../api/adminEvents";
import { listUsers } from "../api/adminUsers";
import { listHymns } from "../api/hymns";
import { useAuth } from "../auth/AuthContext";

type MetricState = {
  hymns: number | null;
  enabledHymns: number | null;
  users: number | null;
  activeAdmins: number | null;
  activeInvites: number | null;
  weeklyEvents: number | null;
};

const EMPTY_METRIC: MetricState = {
  hymns: null,
  enabledHymns: null,
  users: null,
  activeAdmins: null,
  activeInvites: null,
  weeklyEvents: null,
};

type KpiCardProps = {
  title: string;
  value: number | null;
  suffix?: string;
  gradientClass: string;
  helper: string;
};

function KpiCard({ title, value, suffix, gradientClass, helper }: KpiCardProps) {
  const display = value == null ? "--" : value.toLocaleString("ko-KR");

  return (
    <div className={`soy-kpi-gradient rounded-[8px] px-4 py-3 text-white ${gradientClass}`}>
      <div className="text-xs font-medium text-white/80">{title}</div>
      <div className="mt-2 flex items-end gap-1">
        <strong className="text-3xl leading-none">{display}</strong>
        {suffix && <span className="pb-0.5 text-sm text-white/80">{suffix}</span>}
      </div>
      <div className="mt-2 text-xs text-white/75">{helper}</div>
    </div>
  );
}

function systemRiskLabel(activeAdmins: number | null): string {
  if (activeAdmins == null) return "Checking admin status...";
  if (activeAdmins <= 1) return "Single-point admin risk";
  return "Admin redundancy is healthy";
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [metrics, setMetrics] = useState<MetricState>(EMPTY_METRIC);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMetrics = useCallback(async () => {
    setLoading(true);
    setError(null);

    const [hymnResult, userResult, inviteResult, eventResult] = await Promise.allSettled([
      listHymns(),
      listUsers(),
      listInviteCodes(),
      listAdminEvents({ page: 1, size: 1, summaryDays: 7 }),
    ]);

    const next: MetricState = { ...EMPTY_METRIC };
    let failures = 0;

    if (hymnResult.status === "fulfilled") {
      next.hymns = hymnResult.value.length;
      next.enabledHymns = hymnResult.value.filter((item) => item.enabled).length;
    } else {
      failures += 1;
    }

    if (userResult.status === "fulfilled") {
      next.users = userResult.value.length;
      next.activeAdmins = userResult.value.filter((item) => item.role === "ADMIN" && item.status === "ACTIVE").length;
    } else {
      failures += 1;
    }

    if (inviteResult.status === "fulfilled") {
      next.activeInvites = inviteResult.value.filter((item) => item.enabled).length;
    } else {
      failures += 1;
    }

    if (eventResult.status === "fulfilled") {
      next.weeklyEvents = eventResult.value.summary.total;
    } else {
      failures += 1;
    }

    setMetrics(next);
    if (failures > 0) {
      setError("Some metrics could not be loaded. You can still continue operations.");
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    void fetchMetrics();
  }, [fetchMetrics]);

  const activeRatio = useMemo(() => {
    if (metrics.users == null || metrics.users <= 0 || metrics.activeAdmins == null) return null;
    return Math.round((metrics.activeAdmins / metrics.users) * 100);
  }, [metrics.users, metrics.activeAdmins]);

  return (
    <div className="space-y-4">
      <section className="soy-card overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 px-4 py-3">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-600">Soybean-style Console</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">Operations Dashboard</h2>
            <p className="mt-1 text-sm text-slate-500">Welcome back, {user?.role ?? "ADMIN"} operator.</p>
          </div>
          <div className="flex gap-2">
            <Link
              to="/users"
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              Manage Users
            </Link>
            <Link
              to="/events"
              className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-indigo-700"
            >
              Open Events
            </Link>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
          <KpiCard
            title="Total Hymns"
            value={metrics.hymns}
            helper={loading ? "Loading metrics..." : "Registered in catalog"}
            gradientClass="bg-gradient-to-br from-fuchsia-500 to-purple-600"
          />
          <KpiCard
            title="Available Hymns"
            value={metrics.enabledHymns}
            helper={loading ? "Loading metrics..." : "Enabled for users"}
            gradientClass="bg-gradient-to-br from-cyan-500 to-blue-600"
          />
          <KpiCard
            title="Active Invite Codes"
            value={metrics.activeInvites}
            helper={loading ? "Loading metrics..." : "Ready for onboarding"}
            gradientClass="bg-gradient-to-br from-emerald-500 to-teal-600"
          />
          <KpiCard
            title="7-day Events"
            value={metrics.weeklyEvents}
            helper={loading ? "Loading metrics..." : "Recent activity volume"}
            gradientClass="bg-gradient-to-br from-orange-500 to-amber-600"
          />
        </div>
      </section>

      <section className="grid grid-cols-1 gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="soy-card p-4">
          <h3 className="text-base font-semibold text-slate-900">System Health</h3>
          <p className="mt-1 text-sm text-slate-500">Quick check for account and access stability.</p>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">Total Users</div>
              <div className="mt-1 text-lg font-semibold text-slate-900">{metrics.users?.toLocaleString("ko-KR") ?? "--"}</div>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">Active Admins</div>
              <div className="mt-1 text-lg font-semibold text-slate-900">{metrics.activeAdmins?.toLocaleString("ko-KR") ?? "--"}</div>
              {activeRatio != null && <div className="text-xs text-slate-500">Admin ratio: {activeRatio}%</div>}
            </div>
          </div>

          <div className="mt-3 rounded-lg border border-indigo-100 bg-indigo-50 px-3 py-2 text-sm text-indigo-800">
            {systemRiskLabel(metrics.activeAdmins)}
          </div>

          {error && (
            <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-700">{error}</div>
          )}
        </div>

        <div className="soy-card p-4">
          <h3 className="text-base font-semibold text-slate-900">Quick Links</h3>
          <p className="mt-1 text-sm text-slate-500">Direct access to frequent operational tasks.</p>

          <div className="mt-4 grid grid-cols-1 gap-2">
            <Link to="/hymns" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hymn Catalog Management
            </Link>
            <Link to="/assets/upload" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Asset Upload
            </Link>
            <Link to="/invite-codes" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Invite Code Operations
            </Link>
            <Link to="/profile-change-requests" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Profile Change Approval
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
