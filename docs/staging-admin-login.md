# Staging Admin Login

Staging URL:
- `http://13.209.200.12/`

## Recommended (Operator 1-person): ID/PW Login

Admin login can use dedicated credentials configured by environment variables:
- `ADMIN_LOGIN_ID`
- `ADMIN_LOGIN_PASSWORD`

Important:
- Invite codes (for example `stage-xxxxxx`) are for app user onboarding.
- Invite codes are **not** admin login IDs.

API endpoint:
- `POST /api/v1/auth/admin/login`

Credential rotation endpoint (admin token required):
- `POST /api/v1/admin/auth/password`

Request body:
```json
{
  "loginId": "your-admin-id",
  "password": "your-admin-password"
}
```

If login succeeds, API issues normal access/refresh tokens with `ADMIN` role.

You can rotate admin ID/password from Admin web `/help` page.

## Optional: Social Login Allowlist Mode

If you still want Kakao social login for admin:
- `ADMIN_KAKAO_SUBJECTS`: comma-separated allowed Kakao user IDs
- `ADMIN_ENFORCE_ADMIN_ONLY=true`: block non-allowlisted users

This mode is optional and can be disabled once ID/PW login is fully adopted.

## Troubleshooting

- `admin_login_disabled`:
  - `ADMIN_LOGIN_ID` or `ADMIN_LOGIN_PASSWORD` is empty in server env.
- `admin_login_failed`:
  - ID or password mismatch.
- `forbidden` on admin pages after login:
  - clear browser `sessionStorage` tokens and log in again.
  - ensure you used `ADMIN_LOGIN_ID` / `ADMIN_LOGIN_PASSWORD`, not an invite code.

