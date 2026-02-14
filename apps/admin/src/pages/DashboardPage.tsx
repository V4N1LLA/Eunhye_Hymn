import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";

function Item({
  title,
  description,
  to,
}: {
  title: string;
  description: string;
  to: string;
}) {
  return (
    <Link
      to={to}
      className="block rounded-xl border border-slate-200 bg-white p-4 shadow-sm hover:border-indigo-300 hover:shadow transition"
    >
      <div className="font-semibold text-slate-900">{title}</div>
      <div className="mt-1 text-sm text-slate-600">{description}</div>
    </Link>
  );
}

export default function DashboardPage() {
  const { user } = useAuth();
  const host = window.location.host;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">홈</h1>
          <p className="mt-1 text-sm text-slate-600">
            운영자 전용 Admin 콘솔입니다. (host: <span className="font-mono">{host}</span>)
          </p>
        </div>
        <div className="text-right">
          <div className="text-sm text-slate-600">Role</div>
          <div className="mt-1 inline-flex items-center rounded-full bg-indigo-50 px-3 py-1 text-sm font-semibold text-indigo-700">
            {user?.role ?? "-"}
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <Item title="찬양 관리" description="목록/검색/활성화/삭제, 편집/추가" to="/hymns" />
        <Item title="에셋 업로드" description="이미지/악보/음원 업로드" to="/assets/upload" />
        <Item title="초대코드 관리" description="초대코드 생성/비활성화/사용량 확인" to="/invite-codes" />
        <Item title="사용자 관리" description="사용자 역할/상태 관리" to="/users" />
        <Item title="감사 로그/분석" description="이벤트 검색/요약/CSV 내보내기" to="/events" />
        <Item title="도움말" description="로그인/토큰/권한 문제 해결" to="/help" />
      </div>
    </div>
  );
}

