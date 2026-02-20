# Secrets Management Guide

## 1) Core principles
- Never commit real secrets to Git (private repo included).
- Keep `.env` files local-only; commit only templates (`.env.example`).
- Inject secrets at runtime (or CI job runtime), not in source files.

## 2) Classify values first
1. Public config (safe to commit)
   - Example: feature flags, non-sensitive `API_BASE_URL`, `APP_ENV`.
2. Sensitive config (treat as secret by policy)
   - Example: third-party app keys that can be abused (`KAKAO_NATIVE_APP_KEY`).
3. Real secrets (must never be committed)
   - Example: `JWT_SECRET`, DB password, cloud credentials, signing keys.
   - SMS provider credentials (`SMS_TWILIO_ACCOUNT_SID`, `SMS_TWILIO_AUTH_TOKEN`).

## 3) Environment separation policy
- local
  - Use local `.env` / `apps/mobile/.env` only.
  - Non-production values only.
  - Keep `SMS_TWILIO_ENABLED=false` unless local end-to-end SMS test is required.
- staging
  - Use GitHub Actions environment secrets + managed secret store.
  - Separate credentials from production.
- release (production)
  - Use dedicated production secret store and strict least-privilege access.
  - No shared credentials with local/staging.

## 4) Current repo conventions
- Versioned (committed): `apps/mobile/env/local.json`, `staging.json`, `release.json`
  - Store non-secret app config only (`APP_ENV`, `API_BASE_URL`, `ENABLE_DEV_LOGIN`).
- Non-versioned (ignored): `.env`, `apps/mobile/.env`
  - Store local-only secrets like `KAKAO_NATIVE_APP_KEY`.
- CI/CD secret source: GitHub Actions `Settings > Secrets and variables > Actions`
  - Split by environment (`staging`, `production`) and by purpose.

## 5) Rotation and leak response
- Rotate immediately when leak is suspected.
- Periodically rotate high-impact secrets:
  - `JWT_SECRET`
  - DB credentials
  - cloud access keys
  - mobile signing credentials
- Leak response steps:
  1. Revoke/rotate leaked secret.
  2. Audit access logs and token usage.
  3. Purge leaked value from history if needed.
  4. Record follow-up in `docs/changelog-dev.md`.

## 6) PR/review checklist
- No real keys/tokens/passwords in changed files.
- No secrets in logs/screenshots/PR body/comments.
- `.env` files are not tracked by Git.
- `.env.example` keeps placeholders or local-safe defaults only.

## 7) Mobile release secrets (GitHub Actions)
Android signing (`.github/workflows/mobile-store-release.yml`):
- `MOBILE_ANDROID_KEYSTORE_BASE64`
- `MOBILE_ANDROID_KEY_ALIAS`
- `MOBILE_ANDROID_KEY_PASSWORD`
- `MOBILE_ANDROID_STORE_PASSWORD`

Android Google Play upload (`.github/workflows/mobile-store-release.yml`):
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `GOOGLE_PLAY_PACKAGE_NAME`

iOS TestFlight upload (`.github/workflows/mobile-store-release.yml`):
- `MOBILE_IOS_BUNDLE_ID`
- `MOBILE_IOS_TEAM_ID`
- `MOBILE_IOS_P12_BASE64`
- `MOBILE_IOS_P12_PASSWORD`
- `MOBILE_IOS_APPSTORE_ISSUER_ID`
- `MOBILE_IOS_APPSTORE_API_KEY_ID`
- `MOBILE_IOS_APPSTORE_API_PRIVATE_KEY`
