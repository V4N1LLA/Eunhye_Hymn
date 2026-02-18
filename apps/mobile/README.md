# Eunhye Hymn Mobile (Flutter MVP)

`apps/mobile` is the Flutter mobile app for Eunhye Hymn.

## Scope

- Social login
  - Kakao SDK login (mobile, redirect flow)
  - Invite code input (first login only)
  - Hidden dev login switch (`ENABLE_DEV_LOGIN=true`)
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
  - Offline changes are synced when network is restored
- Auth
  - Access/refresh token persistence
  - Automatic refresh + retry on 401

## Run

Use the repo wrapper script `scripts/flutterw.ps1`.

```powershell
# first time from repo root
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get

# web
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1

# android emulator/device (staging)
..\..\scripts\flutterw.ps1 run -d emulator-5554 `
  --dart-define=API_BASE_URL=http://13.209.200.12 `
  --dart-define=KAKAO_NATIVE_APP_KEY=<kakao_native_app_key>
```

Notes:
- `API_BASE_URL` automatically appends `/api/v1` if omitted.
- For physical devices, use the reachable host IP/domain instead of `10.0.2.2`.
- Kakao Android callback scheme is `kakao<KAKAO_NATIVE_APP_KEY>`. If the key is missing/mismatched at run time, Kakao consent can stop at "Continue" without returning to the app.
- `ENABLE_DEV_LOGIN=true` adds hidden local dev login panel on the login screen.

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
