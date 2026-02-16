import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listInviteCodes } from "../api/adminInviteCodes";
import { listAdminEvents } from "../api/adminEvents";
import { listUsers } from "../api/adminUsers";
import { listHymns } from "../api/hymns";
import { useAuth } from "../auth/AuthContext";
import InsightCard from "../components/InsightCard";

type QuickActionTone = "indigo" | "emerald" | "amber" | "sky" | "violet" | "rose";
type QuickAction = {
  title: string;
  description: string;
  to: string;
  badge: string;
  tone: QuickActionTone;
};

const QUICK_ACTIONS: QuickAction[] = [
  { title: "찬양 관리", description: "목록/검색/활성화/삭제", to: "/hymns", badge: "HM", tone: "indigo" },
  { title: "에셋 업로드", description: "이미지/악보/음원 등록", to: "/assets/upload", badge: "AS", tone: "sky" },
  { title: "초대코드 관리", description: "코드 생성 및 비활성화", to: "/invite-codes", badge: "IC", tone: "emerald" },
  { title: "사용자 관리", description: "역할/상태 점검 및 수정", to: "/users", badge: "US", tone: "amber" },
  { title: "감사 로그/분석", description: "이벤트 조회/CSV 내보내기", to: "/events", badge: "LG", tone: "violet" },
  { title: "도움말", description: "로그인/권한 문제 해결", to: "/help", badge: "HP", tone: "rose" },
];

type DashboardMetrics = {
  totalHymns: number | null;
  enabledHymns: number | null;
  totalUsers: number | null;
  activeAdmins: number | null;
  activeInviteCodes: number | null;
  weeklyEvents: number | null;
};

const EMPTY_METRICS: DashboardMetrics = {
  totalHymns: null,
  enabledHymns: null,
  totalUsers: null,
  activeAdmins: null,
  activeInviteCodes: null,
  weeklyEvents: null,
};

const QUICK_ACTION_TONE_CLASS: Record<
  QuickActionTone,
  { card: string; badge: string; link: string; glow: string }
> = {
  indigo: {
    card: "border-indigo-200 bg-gradient-to-br from-white via-indigo-50 to-indigo-100/70 hover:border-indigo-300",
    badge: "bg-indigo-600/10 text-indigo-700",
    link: "text-indigo-700",
    glow: "bg-indigo-300/70",
  },
  emerald: {
    card: "border-emerald-200 bg-gradient-to-br from-white via-emerald-50 to-emerald-100/70 hover:border-emerald-300",
    badge: "bg-emerald-600/10 text-emerald-700",
    link: "text-emerald-700",
    glow: "bg-emerald-300/70",
  },
  amber: {
    card: "border-amber-200 bg-gradient-to-br from-white via-amber-50 to-amber-100/70 hover:border-amber-300",
    badge: "bg-amber-600/10 text-amber-700",
    link: "text-amber-700",
    glow: "bg-amber-300/70",
  },
  sky: {
    card: "border-sky-200 bg-gradient-to-br from-white via-sky-50 to-sky-100/70 hover:border-sky-300",
    badge: "bg-sky-600/10 text-sky-700",
    link: "text-sky-700",
    glow: "bg-sky-300/70",
  },
  violet: {
    card: "border-violet-200 bg-gradient-to-br from-white via-violet-50 to-violet-100/70 hover:border-violet-300",
    badge: "bg-violet-600/10 text-violet-700",
    link: "text-violet-700",
    glow: "bg-violet-300/70",
  },
  rose: {
    card: "border-rose-200 bg-gradient-to-br from-white via-rose-50 to-rose-100/70 hover:border-rose-300",
    badge: "bg-rose-600/10 text-rose-700",
    link: "text-rose-700",
    glow: "bg-rose-300/70",
  },
};

function QuickActionCard({ title, description, to, badge, tone }: QuickAction) {
  const toneClass = QUICK_ACTION_TONE_CLASS[tone];

  return (
    <Link
      to={to}
      className={`group relative overflow-hidden rounded-2xl border p-4 shadow-sm transition duration-150 hover:-translate-y-0.5 hover:shadow ${toneClass.card}`}
    >
      <div className={`pointer-events-none absolute -right-5 -top-5 h-16 w-16 rounded-full blur-2xl ${toneClass.glow}`} />
      <div className="relative flex items-start justify-between gap-3">
        <div className="text-base font-semibold text-slate-900">{title}</div>
        <span className={`rounded-lg px-2 py-1 text-[10px] font-semibold ${toneClass.badge}`}>{badge}</span>
      </div>
      <div className="relative mt-1 text-sm text-slate-600">{description}</div>
      <div className={`relative mt-3 text-xs font-semibold ${toneClass.link}`}>바로 가기 →</div>
    </Link>
  );
}

function getRatio(part: number | null, total: number | null): number | null {
  if (part == null || total == null) return null;
  if (total <= 0) return 0;
  return part / total;
}

function getAdminRiskText(activeAdminCount: number | null): string {
  if (activeAdminCount == null) return "조회 전";
  if (activeAdminCount <= 0) return "즉시 점검 필요";
  if (activeAdminCount === 1) return "단일 운영자 구성";
  return "복수 운영자 구성";
}

export default function DashboardPage() {
  const { user } = useAuth();
  const host = window.location.host;
  const isAdmin = user?.role === "ADMIN";
  const [metrics, setMetrics] = useState<DashboardMetrics>(EMPTY_METRICS);
  const [metricsLoading, setMetricsLoading] = useState(true);
  const [metricsError, setMetricsError] = useState<string | null>(null);

  const fetchMetrics = useCallback(async () => {
    setMetricsLoading(true);
    setMetricsError(null);

    const [hymnsResult, usersResult, inviteCodesResult, eventsResult] = await Promise.allSettled([
      listHymns(),
      listUsers(),
      listInviteCodes(),
      listAdminEvents({ page: 1, size: 1, summaryDays: 7 }),
    ]);

    const nextMetrics: DashboardMetrics = { ...EMPTY_METRICS };
    let failedRequestCount = 0;

    if (hymnsResult.status === "fulfilled") {
      nextMetrics.totalHymns = hymnsResult.value.length;
      nextMetrics.enabledHymns = hymnsResult.value.filter((item) => item.enabled).length;
    } else {
      failedRequestCount += 1;
    }

    if (usersResult.status === "fulfilled") {
      nextMetrics.totalUsers = usersResult.value.length;
      nextMetrics.activeAdmins = usersResult.value.filter((item) => item.role === "ADMIN" && item.status === "ACTIVE").length;
    } else {
      failedRequestCount += 1;
    }

    if (inviteCodesResult.status === "fulfilled") {
      nextMetrics.activeInviteCodes = inviteCodesResult.value.filter((item) => item.enabled).length;
    } else {
      failedRequestCount += 1;
    }

    if (eventsResult.status === "fulfilled") {
      nextMetrics.weeklyEvents = eventsResult.value.summary.total;
    } else {
      failedRequestCount += 1;
    }

    setMetrics(nextMetrics);
    if (failedRequestCount > 0) {
      setMetricsError(
        failedRequestCount === 4
          ? "지표를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요."
          : "일부 지표를 불러오지 못했습니다. 다시 조회해 주세요.",
      );
    }
    setMetricsLoading(false);
  }, []);

  useEffect(() => {
    void fetchMetrics();
  }, [fetchMetrics]);

  const hymnCoverage = getRatio(metrics.enabledHymns, metrics.totalHymns);
  const adminCoverage = getRatio(metrics.activeAdmins, metrics.totalUsers);

  return (
    <div className="space-y-5">
      <section className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-indigo-600 via-indigo-700 to-violet-700 px-5 py-6 text-white shadow-lg">
        <div className="pointer-events-none absolute -right-20 -top-20 h-64 w-64 rounded-full bg-violet-300/30 blur-3xl" />
        <div className="pointer-events-none absolute -left-16 bottom-0 h-44 w-44 rounded-full bg-sky-300/20 blur-3xl" />
        <div className="relative flex flex-wrap items-start justify-between gap-4">
          <div>
            <div className="text-xs font-semibold uppercase tracking-[0.2em] text-indigo-100">Admin Console</div>
            <h2 className="mt-2 text-2xl font-bold">운영 대시보드</h2>
            <p className="mt-1 text-sm text-indigo-100">
              단일 운영자 기준으로 핵심 지표와 자주 쓰는 작업을 한 화면에서 빠르게 제어합니다.
            </p>
            <div className="mt-3 flex flex-wrap gap-2 text-xs">
              <span className="rounded-full border border-white/30 bg-white/10 px-2.5 py-1 font-mono text-indigo-50">{host}</span>
              <span className="rounded-full border border-white/30 bg-white/10 px-2.5 py-1">{user?.role ?? "-"}</span>
              <span
                className={`rounded-full border px-2.5 py-1 ${isAdmin ? "border-emerald-200/60 bg-emerald-400/20 text-emerald-50" : "border-amber-200/60 bg-amber-400/20 text-amber-50"}`}
              >
                {isAdmin ? "권한 정상" : "권한 점검 필요"}
              </span>
            </div>
          </div>
          <button
            type="button"
            onClick={() => void fetchMetrics()}
            disabled={metricsLoading}
            className="rounded-xl border border-white/40 bg-white/15 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/25 disabled:opacity-60"
          >
            {metricsLoading ? "조회 중..." : "지표 새로고침"}
          </button>
        </div>
        <div className="relative mt-4 rounded-xl border border-white/20 bg-white/10 px-4 py-3 text-sm text-indigo-50">
          로그인 후 각 메뉴에서 "권한 없음"이 보이면, 먼저{" "}
          <Link className="font-semibold underline decoration-white/60 underline-offset-2" to="/help">
            도움말
          </Link>
          의 "세션 초기화 후 재로그인" 절차를 진행하세요.
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3 flex flex-wrap items-end justify-between gap-2">
          <div>
            <h3 className="text-base font-semibold text-slate-900">실시간 운영 지표</h3>
            <p className="text-xs text-slate-500">주요 운영 수치를 시각 카드로 요약합니다.</p>
          </div>
          <div className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-600">
            관리자 리스크: {getAdminRiskText(metrics.activeAdmins)}
          </div>
        </div>
        {metricsError && (
          <div className="mb-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
            {metricsError}
          </div>
        )}
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
          <InsightCard
            title="전체 찬양"
            value={metrics.totalHymns}
            description="등록된 찬양 수"
            badge="HM"
            tone="indigo"
            loading={metricsLoading}
          />
          <InsightCard
            title="활성 찬양"
            value={metrics.enabledHymns}
            description="서비스 노출 중"
            badge="LIVE"
            tone="emerald"
            ratio={hymnCoverage}
            loading={metricsLoading}
          />
          <InsightCard
            title="전체 사용자"
            value={metrics.totalUsers}
            description="가입된 사용자 수"
            badge="USR"
            tone="slate"
            loading={metricsLoading}
          />
          <InsightCard
            title="활성 관리자"
            value={metrics.activeAdmins}
            description="현재 ADMIN + ACTIVE"
            badge="ADM"
            tone="amber"
            ratio={adminCoverage}
            loading={metricsLoading}
          />
          <InsightCard
            title="활성 초대코드"
            value={metrics.activeInviteCodes}
            description="사용 가능한 코드"
            badge="INV"
            tone="sky"
            loading={metricsLoading}
          />
          <InsightCard
            title="최근 7일 이벤트"
            value={metrics.weeklyEvents}
            description="감사 로그 이벤트"
            badge="LOG"
            tone="violet"
            loading={metricsLoading}
          />
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
        <div className="mb-3">
          <h3 className="text-base font-semibold text-slate-900">빠른 작업</h3>
          <p className="text-xs text-slate-500">자주 사용하는 화면으로 즉시 이동합니다.</p>
        </div>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          {QUICK_ACTIONS.map((item) => (
            <QuickActionCard key={item.to} {...item} />
          ))}
        </div>
      </section>
    </div>
  );
}
