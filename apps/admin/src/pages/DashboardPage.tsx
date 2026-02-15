import { Link } from "react-router-dom";
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

export default function DashboardPage() {
  const { user } = useAuth();
  const host = window.location.host;
  const isAdmin = user?.role === "ADMIN";

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

