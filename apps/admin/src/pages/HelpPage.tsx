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
      setSaveError("Current password, new login ID, and new password are required.");
      return;
    }

    if (newPassword.length < 8) {
      setSaveError("New password must be at least 8 characters.");
      return;
    }

    if (newPassword !== confirmPassword) {
      setSaveError("New password and confirmation do not match.");
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
      setSaveSuccess(`Admin credential updated. New login ID: ${result.loginId}`);
    } catch (error) {
      setSaveError(error instanceof Error ? error.message : "Failed to update admin credential.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-4">
      <section className="soy-panel">
        <p className="soy-kicker">Support</p>
        <h1 className="soy-title">Admin Help</h1>
        <p className="soy-description">Credential reset and common authorization troubleshooting.</p>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">Change Admin Login ID / Password</h2>
        <p className="soy-description">Verify current password before replacing administrator credentials.</p>

        <form className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-2" onSubmit={handleCredentialUpdate}>
          <label>
            <span className="soy-label">Current Password</span>
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
            <span className="soy-label">New Login ID</span>
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
            <span className="soy-label">New Password</span>
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
            <span className="soy-label">Confirm New Password</span>
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
              {saving ? "Updating..." : "Update Credential"}
            </button>
          </div>
        </form>

        {saveError && <div className="mt-3 soy-alert soy-alert-error">{saveError}</div>}
        {saveSuccess && <div className="mt-3 soy-alert soy-alert-success">{saveSuccess}</div>}
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">Login Rules</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- Admin console uses ID/password login only.</div>
          <div>- Required server envs: `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`.</div>
          <div>- Invite codes are for user sign-up, not admin login.</div>
        </div>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">Fix 403 Quickly</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>1. Use session reset and sign in again.</div>
          <div>2. Confirm you logged in with admin ID/password.</div>
          <div>3. If still failing, verify deployed env credentials.</div>
        </div>
      </section>

      <section className="soy-panel">
        <h2 className="soy-title">Common API Error Codes</h2>
        <div className="mt-2 space-y-1 text-sm text-slate-600">
          <div>- `admin_login_disabled`: admin ID/password not configured on server.</div>
          <div>- `admin_login_failed`: login ID/password mismatch.</div>
          <div>- `admin_password_change_failed`: current password mismatch.</div>
          <div>- `unauthorized`: token expired or invalid.</div>
          <div>- `forbidden`: non-admin token called admin API.</div>
        </div>
      </section>
    </div>
  );
}
