export default function HelpPage() {
  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">도움말</h1>
        <p className="mt-1 text-sm text-slate-600">운영자 1인 기준 로그인/권한 문제를 빠르게 해결하는 가이드입니다.</p>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">로그인 규칙 (중요)</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- Admin 웹은 OAuth가 아니라 관리자 ID/PW 로그인만 사용합니다.</div>
          <div>- 필요한 서버 환경변수: `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`</div>
          <div>- 초대코드(`stage-...`)는 앱 사용자 가입용이며, Admin 로그인 ID가 아닙니다.</div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">권한 없음(403) 해결 순서</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>1) 로그인 화면에서 "세션 초기화 후 다시 로그인" 버튼 실행</div>
          <div>2) 관리자 ID/PW로 다시 로그인</div>
          <div>3) 여전히 실패하면 서버 환경변수와 배포 반영 여부 확인</div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">사용자 관리 잠금 정책</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- 현재 로그인한 운영자 계정은 역할/상태 변경이 잠깁니다.</div>
          <div>- 마지막 활성 관리자 계정은 USER 변경/비활성화가 잠깁니다.</div>
          <div>- 목적: 단일 운영자 환경에서 관리자 계정 잠금(셀프 락아웃) 방지</div>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">자주 보이는 API 에러 코드</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- `admin_login_disabled`: 서버에 관리자 ID/PW가 비어 있음</div>
          <div>- `admin_login_failed`: 아이디 또는 비밀번호 불일치</div>
          <div>- `unauthorized`: 토큰 만료/누락</div>
          <div>- `forbidden`: ADMIN이 아닌 토큰으로 Admin API 호출</div>
        </div>
      </section>
    </div>
  );
}

