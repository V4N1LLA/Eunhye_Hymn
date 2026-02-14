export default function HelpPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">도움말</h1>
        <p className="mt-1 text-sm text-slate-600">
          운영자 1인 기준으로, Admin 로그인/권한 이슈 해결 가이드입니다.
        </p>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">로그인 방식</h2>
        <div className="text-sm text-slate-600 space-y-1">
          <div>- Admin 웹은 ID/PW 로그인(`auth/admin/login`)을 사용합니다.</div>
          <div>- 서버에 `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`가 설정되어 있어야 로그인됩니다.</div>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">권한이 없다고 나올 때</h2>
        <div className="text-sm text-slate-600 space-y-1">
          <div>- 현재 토큰 role이 `USER`이면 Admin API는 403입니다.</div>
          <div>- 로그아웃 후 재로그인하세요.</div>
          <div>- 계속 실패하면 브라우저 `sessionStorage`의 `accessToken`, `refreshToken`을 지우고 다시 로그인하세요.</div>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">API 에러 코드</h2>
        <div className="text-sm text-slate-600 space-y-1">
          <div>- `admin_login_disabled`: 서버에 ID/PW 설정이 없음</div>
          <div>- `admin_login_failed`: 아이디 또는 비밀번호 불일치</div>
          <div>- `unauthorized`: 로그인 토큰 없음/만료</div>
          <div>- `forbidden`: USER 토큰으로 admin endpoint 접근</div>
        </div>
      </div>
    </div>
  );
}

