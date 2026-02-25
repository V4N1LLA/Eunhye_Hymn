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
      setSaveSuccess(`관리자 인증 정보를 변경했습니다. 새 로그인 ID: ${result.loginId}`);
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : "관리자 인증 정보 변경에 실패했습니다.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <p className="soy-kicker">지원</p>
        <h1 className="soy-title">관리자 도움말</h1>
        <p className="soy-description">로그인 문제, 권한 오류, 인증 정보 변경 절차를 안내합니다.</p>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">관리자 ID / 비밀번호 변경</h2>
        <p className="soy-description">현재 비밀번호 검증 후 관리자 인증 정보를 교체할 수 있습니다.</p>

        <form className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2" onSubmit={handleCredentialUpdate}>
          <label>
            <span className="soy-label">현재 비밀번호</span>
            <input
              type="password"
              value={currentPassword}
              onChange={(event) => setCurrentPassword(event.target.value)}
              autoComplete="current-password"
              className="soy-input"
              disabled={saving}
            />
          </label>

          <label>
            <span className="soy-label">새 로그인 ID</span>
            <input
              type="text"
              value={newLoginId}
              onChange={(event) => setNewLoginId(event.target.value)}
              autoComplete="username"
              className="soy-input"
              disabled={saving}
            />
          </label>

          <label>
            <span className="soy-label">새 비밀번호</span>
            <input
              type="password"
              value={newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
              autoComplete="new-password"
              className="soy-input"
              disabled={saving}
            />
          </label>

          <label>
            <span className="soy-label">새 비밀번호 확인</span>
            <input
              type="password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              autoComplete="new-password"
              className="soy-input"
              disabled={saving}
            />
          </label>

          <div className="md:col-span-2">
            <button type="submit" disabled={saving} className="soy-btn soy-btn-primary">
              {saving ? "변경 중..." : "인증 정보 변경"}
            </button>
          </div>
        </form>

        {saveError && <div className="mt-3 soy-alert soy-alert-error">{saveError}</div>}
        {saveSuccess && <div className="mt-3 soy-alert soy-alert-success">{saveSuccess}</div>}
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">로그인 기본 규칙</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- 관리자 콘솔은 ID/비밀번호 로그인만 사용합니다.</div>
          <div>- 필수 서버 환경변수: `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`</div>
          <div>- 초대 코드는 사용자 가입용이며 관리자 로그인에는 사용하지 않습니다.</div>
        </div>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">403 오류 빠른 점검</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>1. 세션 초기화 후 다시 로그인</div>
          <div>2. 관리자 계정(ID/비밀번호)으로 로그인했는지 확인</div>
          <div>3. 계속 실패하면 배포된 서버 환경변수 값 확인</div>
        </div>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">자주 보는 API 오류 코드</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- `admin_login_disabled`: 서버에 관리자 인증값 미설정</div>
          <div>- `admin_login_failed`: ID/비밀번호 불일치</div>
          <div>- `admin_password_change_failed`: 현재 비밀번호 검증 실패</div>
          <div>- `unauthorized`: 토큰 만료/무효</div>
          <div>- `forbidden`: 관리자 권한 없는 토큰으로 관리자 API 호출</div>
        </div>
      </section>
    </div>
  );
}
