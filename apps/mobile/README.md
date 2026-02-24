# Eunhye Hymn Mobile (Flutter MVP)

`apps/mobile` is the Flutter mobile app for Eunhye Hymn.

## Scope

- Social login
  - Kakao SDK login (mobile, redirect flow)
  - ID/PW login and signup
  - Invite code verification (`POST /auth/invite/validate`)
  - Phone number + SMS code verification (`POST /auth/sms/request`, `POST /auth/sms/verify`)
  - Withdrawal from verification screen (`POST /auth/withdraw`)
- Hymn
  - List and search
  - AI recommendation by situation (`POST /ai/hymn-recommendations`)
  - Detail view
  - PNG asset display (multi-page per hymn)
  - MIDI playback controls (play/pause/stop/speed)
- Personalization
  - Profile read/update (`GET/PUT /me/profile`)
  - Profile change request (`POST /me/profile-change-requests`, `GET /me/profile-change-requests/latest`)
  - Favorites toggle
  - Notes read/write
  - Recent history
- Offline
  - Local cache fallback for list/detail/note/favorites/history
  - Hymn score/MIDI downloads are cached per session user and rejected when content-type or size checks fail
  - Offline changes are synced when network is restored
- Auth
  - Login flow: `Login -> InviteCode -> PhoneNumber -> SmsCode -> Home`
  - Access/refresh token persistence
  - Automatic refresh + retry on 401
  - `/me/profile` verification flags (`inviteVerified`, `phoneVerified`, `verified`)

## Run

Use environment profiles under `apps/mobile/env/`:
- `local.json`
- `staging.json`
- `release.json`

```powershell
# first time from repo root
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get

# local emulator run
cd ..\..
.\scripts\run-mobile-emulator.ps1 -Environment local -DeviceId emulator-5554

# staging emulator run
.\scripts\run-mobile-emulator.ps1 -Environment staging -DeviceId emulator-5554
```

Notes:
- `run-mobile-emulator.ps1` always injects `KAKAO_NATIVE_APP_KEY` from `apps/mobile/.env` (fallback: repo `.env`) unless overridden.
- `API_BASE_URL` automatically appends `/api/v1` if omitted.
- For physical devices, use the reachable host IP/domain instead of `10.0.2.2`.
- Kakao Android callback scheme is `kakao<KAKAO_NATIVE_APP_KEY>`. If the key is missing/mismatched at run time, Kakao consent can stop at "Continue" without returning to the app.
- Keep secrets (`KAKAO_NATIVE_APP_KEY`, signing keys) out of Git. See `docs/SECRETS_MANAGEMENT.md`.

## Structure

```text
lib/
  main.dart
  src/
    app.dart
    core/
      config/app_config.dart
      network/api_client.dart
      network/api_exception.dart
      storage/token_storage.dart
    features/
      auth/
      hymn/
      history/
```

## CI

- Workflow: `.github/workflows/mobile-ci.yml`
- Trigger: PR and `develop` push (`apps/mobile/**`)
- Steps:
  - `flutter pub get`
  - `flutter analyze`
  - `flutter test`

## Release Readiness

- Android release check: `.github/workflows/mobile-release-check.yml`
  - Builds `app-release.apk` and uploads artifact.
- Local APK build helper: `scripts/build-mobile-apk.ps1`
  - debug APK (staging): `.\scripts\build-mobile-apk.ps1 -Environment staging -BuildMode debug -Install`
  - release APK: `.\scripts\build-mobile-apk.ps1 -Environment release -BuildMode release -ApiBaseUrl https://<prod-domain>/api/v1`
- Store release readiness: `.github/workflows/mobile-store-release.yml` (manual)
  - signed AAB build (`flutter build appbundle --release`)
  - `android_distribution_mode=build_only`: build artifact only
  - `android_distribution_mode=play_upload`: Google Play upload after AAB build
  - `ios_distribution_mode=build_only`: unsigned iOS release artifact build
  - `ios_distribution_mode=testflight`: signed IPA build + TestFlight upload
  - required inputs for play upload:
    - `android_track`: `internal | alpha | beta | production`
    - `android_release_status`: `draft | completed | inProgress | halted`
    - `android_changes_not_sent_for_review`: `true | false`
  - optional input:
    - `android_package_name` (empty -> `GOOGLE_PLAY_PACKAGE_NAME` secret)
  - common input: `api_base_url`
  - current ops policy (2026-02-18): run staging validation only; keep production publish ready but not executed

Android signing config:
- Copy `apps/mobile/android/key.properties.example` to `apps/mobile/android/key.properties`.
- Fill `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.
- `key.properties` and keystore files are ignored by git.

Google Play upload secrets:
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `GOOGLE_PLAY_PACKAGE_NAME`

iOS TestFlight secrets:
- `MOBILE_IOS_BUNDLE_ID`
- `MOBILE_IOS_TEAM_ID`
- `MOBILE_IOS_P12_BASE64`
- `MOBILE_IOS_P12_PASSWORD`
- `MOBILE_IOS_APPSTORE_ISSUER_ID`
- `MOBILE_IOS_APPSTORE_API_KEY_ID`
- `MOBILE_IOS_APPSTORE_API_PRIVATE_KEY`

Store release cycle helpers:
- `scripts/mobile-store-preflight.ps1` (mode-based secret/input checks)
- `scripts/mobile-store-cycle.ps1` (preflight + workflow_dispatch + run watch + log append)
- `scripts/mobile-store-diagnose.ps1` (failed step diagnosis + recovery actions)
- `docs/mobile-store-release-log.md` (execution evidence log)
- `docs/mobile-store-recovery.md` (recovery runbook for failed publish paths)
- latest publish-path verification (2026-02-23):
  - Android `play_upload`: run `22329008651` failed at Google Play upload step
  - iOS `testflight`: run `22329248773` failed at Apple certificate import step
  - next action: replace bootstrap placeholders with real store credentials and rerun both paths

See also:
- Minimal-tooling QA guide: `docs/mobile/qa-minimal-tooling.md`
