# Eunhye Hymn Mobile (Flutter MVP)

`apps/mobile` is the Flutter mobile app for Eunhye Hymn.

## Scope

- Social login
  - Kakao SDK login (mobile, redirect flow)
  - Invite code input (first login only)
  - Account signup/login (`POST /auth/signup`, `POST /auth/login`)
- Hymn
  - List and search
  - Detail view
  - PNG asset display (multi-page per hymn)
  - MIDI playback controls (play/pause/stop/speed)
- Personalization
  - Favorites toggle
  - Notes read/write
  - Recent history
- Offline
  - Local cache fallback for list/detail/note/favorites/history
  - Hymn score/MIDI downloads are cached per session user and rejected when content-type or size checks fail
  - Offline changes are synced when network is restored
- Auth
  - Access/refresh token persistence
  - Automatic refresh + retry on 401

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
- Store release readiness (Android only for now): `.github/workflows/mobile-store-release.yml` (manual)
  - signed AAB build (`flutter build appbundle --release`)
  - `android_distribution_mode=build_only`: build artifact only
  - `android_distribution_mode=play_upload`: Google Play upload after AAB build
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

See also:
- Minimal-tooling QA guide: `docs/mobile/qa-minimal-tooling.md`
