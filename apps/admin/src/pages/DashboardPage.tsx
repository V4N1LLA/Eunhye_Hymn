import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { listInviteCodes } from "../api/adminInviteCodes";
import { listAdminEvents } from "../api/adminEvents";
import { listUsers } from "../api/adminUsers";
import { listHymns } from "../api/hymns";
import { useAuth } from "../auth/AuthContext";

type QuickAction = {
  title: string;
  description: string;
  to: string;
};

const QUICK_ACTIONS: QuickAction[] = [
  { title: "찬양 관리", description: "목록/검색/활성화/삭제", to: "/hymns" },
  { title: "에셋 업로드", description: "찬양별 이미지/악보/음원 등록", to: "/assets/upload" },
  { title: "초대코드 관리", description: "앱 사용자 초대코드 생성 및 비활성화", to: "/invite-codes" },
  { title: "사용자 관리", description: "역할/상태 점검 및 수정", to: "/users" },
  { title: "감사 로그/분석", description: "이벤트 조회/CSV 내보내기", to: "/events" },
  { title: "도움말", description: "로그인/권한 문제 빠른 해결", to: "/help" },
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

function QuickActionCard({ title, description, to }: QuickAction) {
  return (
    <Link
      to={to}
      className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:border-indigo-300 hover:shadow"
    >
      <div className="text-base font-semibold text-slate-900">{title}</div>
      <div className="mt-1 text-sm text-slate-600">{description}</div>
      <div className="mt-3 text-xs font-semibold text-indigo-600">바로 가기</div>
    </Link>
  );
}

function MetricCard({
  title,
  value,
  description,
  accentClassName,
}: {
  title: string;
  value: number | null;
  description: string;
  accentClassName: string;
}) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="text-xs font-semibold text-slate-500">{title}</div>
      <div className={`mt-2 text-2xl font-bold ${accentClassName}`}>{value == null ? "-" : value.toLocaleString("ko-KR")}</div>
      <div className="mt-1 text-xs text-slate-500">{description}</div>
    </div>
  );
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

  return (
    <div className="space-y-5">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-2xl font-bold text-slate-900">운영 대시보드</h2>
            <p className="mt-1 text-sm text-slate-600">
              단일 운영자 기준 어드민 콘솔입니다. 현재 호스트: <span className="font-mono">{host}</span>
            </p>
          </div>
          <div className="grid grid-cols-2 gap-2 text-right text-sm">
            <div className="rounded-lg bg-slate-100 px-3 py-2">
              <div className="text-xs text-slate-500">권한</div>
              <div className="font-semibold text-slate-900">{user?.role ?? "-"}</div>
            </div>
            <div className={`rounded-lg px-3 py-2 ${isAdmin ? "bg-emerald-50" : "bg-amber-50"}`}>
              <div className={`text-xs ${isAdmin ? "text-emerald-700" : "text-amber-700"}`}>접근 상태</div>
              <div className={`font-semibold ${isAdmin ? "text-emerald-800" : "text-amber-800"}`}>
                {isAdmin ? "정상" : "권한 점검 필요"}
              </div>
            </div>
          </div>
        </div>
        <div className="mt-4 rounded-xl border border-indigo-100 bg-indigo-50 px-4 py-3 text-sm text-indigo-900">
          로그인 후 각 메뉴에서 "권한 없음"이 보이면, 먼저 <Link className="font-semibold underline" to="/help">도움말</Link>의
          "세션 초기화 후 재로그인" 절차를 진행하세요.
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <h3 className="text-base font-semibold text-slate-800">실시간 운영 지표</h3>
          <button
            type="button"
            onClick={() => void fetchMetrics()}
            disabled={metricsLoading}
            className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100 disabled:opacity-50"
          >
            {metricsLoading ? "조회 중..." : "지표 새로고침"}
          </button>
        </div>
        {metricsError && (
          <div className="mt-3 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
            {metricsError}
          </div>
        )}
        <div className="mt-3 grid grid-cols-2 gap-3 xl:grid-cols-3">
          <MetricCard
            title="전체 찬양"
            value={metrics.totalHymns}
            description="등록된 찬양 수"
            accentClassName="text-indigo-700"
          />
          <MetricCard
            title="활성 찬양"
            value={metrics.enabledHymns}
            description="서비스 노출 중"
            accentClassName="text-emerald-700"
          />
          <MetricCard
            title="전체 사용자"
            value={metrics.totalUsers}
            description="가입된 사용자 수"
            accentClassName="text-slate-900"
          />
          <MetricCard
            title="활성 관리자"
            value={metrics.activeAdmins}
            description="현재 ADMIN + ACTIVE"
            accentClassName="text-amber-700"
          />
          <MetricCard
            title="활성 초대코드"
            value={metrics.activeInviteCodes}
            description="사용 가능한 코드"
            accentClassName="text-sky-700"
          />
          <MetricCard
            title="최근 7일 이벤트"
            value={metrics.weeklyEvents}
            description="감사 로그 이벤트"
            accentClassName="text-violet-700"
          />
        </div>
      </section>

      <section>
        <h3 className="mb-3 text-base font-semibold text-slate-800">자주 쓰는 작업</h3>
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-3">
          {QUICK_ACTIONS.map((item) => (
            <QuickActionCard key={item.to} {...item} />
          ))}
        </div>
      </section>
    </div>
  );
}

