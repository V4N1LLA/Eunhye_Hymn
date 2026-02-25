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
  if (activeAdmins == null) return "관리자 구성 상태를 확인 중입니다.";
  if (activeAdmins <= 1) return "활성 관리자 1명: 단일 장애 위험";
  return "활성 관리자 이중화 상태가 양호합니다.";
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
      setError("일부 지표를 불러오지 못했습니다. 페이지를 새로고침해 주세요.");
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
            <p className="text-xs font-semibold uppercase tracking-[0.12em] text-indigo-600">운영 콘솔</p>
            <h2 className="mt-1 text-xl font-semibold text-slate-900">관리자 대시보드</h2>
            <p className="mt-1 text-sm text-slate-500">환영합니다. 현재 권한: {roleLabel(user?.role)}</p>
          </div>
          <div className="flex gap-2">
            <Link
              to="/users"
              className="rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              사용자 관리
            </Link>
            <Link
              to="/events"
              className="rounded-md bg-indigo-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-indigo-700"
            >
              이벤트 로그
            </Link>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-3 p-4 md:grid-cols-2 xl:grid-cols-4">
          <KpiCard
            title="전체 찬양"
            value={metrics.hymns}
            helper={loading ? "지표 로딩 중..." : "등록된 찬양 수"}
            gradientClass="bg-gradient-to-br from-fuchsia-500 to-purple-600"
          />
          <KpiCard
            title="활성 찬양"
            value={metrics.enabledHymns}
            helper={loading ? "지표 로딩 중..." : "사용자에게 노출 중"}
            gradientClass="bg-gradient-to-br from-cyan-500 to-blue-600"
          />
          <KpiCard
            title="활성 초대코드"
            value={metrics.activeInvites}
            helper={loading ? "지표 로딩 중..." : "회원가입 가능 코드"}
            gradientClass="bg-gradient-to-br from-emerald-500 to-teal-600"
          />
          <KpiCard
            title="최근 7일 이벤트"
            value={metrics.weeklyEvents}
            helper={loading ? "지표 로딩 중..." : "이용 활동 볼륨"}
            gradientClass="bg-gradient-to-br from-orange-500 to-amber-600"
          />
        </div>
      </section>

      <section className="grid grid-cols-1 gap-4 xl:grid-cols-[1.2fr_0.8fr]">
        <div className="soy-card p-4">
          <h3 className="text-base font-semibold text-slate-900">시스템 상태</h3>
          <p className="mt-1 text-sm text-slate-500">계정/권한 운영 안정성을 빠르게 점검합니다.</p>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2">
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">전체 사용자</div>
              <div className="mt-1 text-lg font-semibold text-slate-900">{metrics.users?.toLocaleString("ko-KR") ?? "--"}</div>
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
              <div className="text-xs text-slate-500">활성 관리자</div>
              <div className="mt-1 text-lg font-semibold text-slate-900">{metrics.activeAdmins?.toLocaleString("ko-KR") ?? "--"}</div>
              {activeRatio != null && <div className="text-xs text-slate-500">관리자 비율: {activeRatio}%</div>}
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
          <h3 className="text-base font-semibold text-slate-900">빠른 이동</h3>
          <p className="mt-1 text-sm text-slate-500">자주 사용하는 운영 화면으로 바로 이동합니다.</p>

          <div className="mt-4 grid grid-cols-1 gap-2">
            <Link to="/hymns" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              찬양 카탈로그 관리
            </Link>
            <Link to="/assets/upload" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              에셋 업로드
            </Link>
            <Link to="/invite-codes" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              초대 코드 운영
            </Link>
            <Link to="/profile-change-requests" className="rounded-lg border border-slate-200 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              프로필 변경 승인
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
}
