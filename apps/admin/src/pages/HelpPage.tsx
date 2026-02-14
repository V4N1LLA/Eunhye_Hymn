export default function HelpPage() {
  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">도움말</h1>
        <p className="mt-1 text-sm text-slate-600">
          운영자 1인 기준으로, 스테이징에서 자주 막히는 포인트를 정리했습니다.
        </p>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">카카오 로그인 입력값</h2>
        <div className="text-sm text-slate-700">
          <div className="font-semibold">Kakao Access Token</div>
          <div className="mt-1 text-slate-600">
            카카오 OAuth <span className="font-semibold">Access Token</span> 문자열입니다.{" "}
            <span className="font-mono">Bearer</span> 접두사는 붙이지 마세요.
          </div>
        </div>
        <div className="text-sm text-slate-700">
          <div className="font-semibold">Invite Code</div>
          <div className="mt-1 text-slate-600">
            신규 계정 생성이 필요할 때만 입력합니다. 운영자 allowlist가 설정된 환경이면 초대코드 없이도
            운영자 계정 생성/로그인이 가능합니다.
          </div>
        </div>
      </div>

      <div className="rounded-xl border border-slate-200 bg-white p-4 shadow-sm space-y-2">
        <h2 className="text-lg font-semibold text-slate-900">권한이 없다고 나올 때</h2>
        <div className="text-sm text-slate-600 space-y-1">
          <div>- 현재 토큰의 role이 <span className="font-mono">USER</span>이면 Admin API는 모두 403이 납니다.</div>
          <div>- 해결: 로그아웃 → 다시 로그인(또는 토큰 갱신)해서 role이 <span className="font-mono">ADMIN</span>인지 확인하세요.</div>
          <div>- 운영자 1인만 쓰는 환경이면, 서버에서 운영자 allowlist를 설정해 “나만 로그인 가능”하게 잠그는 편이 안전합니다.</div>
        </div>
      </div>
    </div>
  );
}

