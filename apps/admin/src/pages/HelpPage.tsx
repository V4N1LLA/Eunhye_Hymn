import { FormEvent, useState } from "react";
import { changeAdminPasswordCredential } from "../api/auth";

export default function HelpPage() {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newLoginId, setNewLoginId] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState<string | null>(null);

  const handleCredentialUpdate = async (event: FormEvent) => {
    event.preventDefault();
    setSaveError(null);
    setSaveSuccess(null);

    if (!currentPassword || !newLoginId.trim() || !newPassword) {
      setSaveError("현재 비밀번호, 새 로그인 ID, 새 비밀번호를 모두 입력해 주세요.");
      return;
    }

    if (newPassword.length < 8) {
      setSaveError("새 비밀번호는 8자 이상이어야 합니다.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setSaveError("새 비밀번호와 확인 값이 일치하지 않습니다.");
      return;
    }

    setSaving(true);
    try {
      const result = await changeAdminPasswordCredential({
        currentPassword,
        newLoginId: newLoginId.trim(),
        newPassword,
      });
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSaveSuccess(`관리자 로그인 정보가 갱신되었습니다. 새 로그인 ID: ${result.loginId}`);
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : "관리자 로그인 정보 변경에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h1 className="text-2xl font-bold text-slate-900">도움말</h1>
        <p className="mt-1 text-sm text-slate-600">운영자 1인 기준 로그인/권한 문제를 빠르게 해결하는 가이드입니다.</p>
      </section>

      <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
        <h2 className="text-lg font-semibold text-slate-900">관리자 ID/PW 변경</h2>
        <p className="mt-1 text-sm text-slate-600">
          현재 비밀번호를 확인한 뒤 관리자 로그인 ID와 비밀번호를 바로 교체할 수 있습니다.
        </p>
        <form className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2" onSubmit={handleCredentialUpdate}>
          <label className="text-sm text-slate-700">
            <div className="mb-1 font-medium">현재 비밀번호</div>
            <input
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              autoComplete="current-password"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              disabled={saving}
            />
          </label>

          <label className="text-sm text-slate-700">
            <div className="mb-1 font-medium">새 로그인 ID</div>
            <input
              type="text"
              value={newLoginId}
              onChange={(e) => setNewLoginId(e.target.value)}
              autoComplete="username"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              disabled={saving}
            />
          </label>

          <label className="text-sm text-slate-700">
            <div className="mb-1 font-medium">새 비밀번호</div>
            <input
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              disabled={saving}
            />
          </label>

          <label className="text-sm text-slate-700">
            <div className="mb-1 font-medium">새 비밀번호 확인</div>
            <input
              type="password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              autoComplete="new-password"
              className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
              disabled={saving}
            />
          </label>

          <div className="md:col-span-2">
            <button
              type="submit"
              disabled={saving}
              className="rounded-lg bg-indigo-600 px-4 py-2 text-sm font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {saving ? "변경 중..." : "로그인 정보 변경"}
            </button>
          </div>
        </form>

        {saveError && (
          <div className="mt-3 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{saveError}</div>
        )}
        {saveSuccess && (
          <div className="mt-3 rounded-lg border border-emerald-200 bg-emerald-50 px-3 py-2 text-sm text-emerald-700">
            {saveSuccess}
          </div>
        )}
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
        <h2 className="text-lg font-semibold text-slate-900">자주 보이는 API 에러 코드</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- `admin_login_disabled`: 서버에 관리자 ID/PW가 비어 있음</div>
          <div>- `admin_login_failed`: ID/비밀번호 불일치</div>
          <div>- `admin_password_change_failed`: 현재 비밀번호 검증 실패</div>
          <div>- `unauthorized`: 토큰 만료/누락</div>
          <div>- `forbidden`: ADMIN이 아닌 토큰으로 Admin API 호출</div>
        </div>
      </section>
    </div>
  );
}
