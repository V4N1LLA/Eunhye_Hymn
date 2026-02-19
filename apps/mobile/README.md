# Eunhye Hymn Mobile (Flutter)

`apps/mobile` is the Flutter client for Eunhye Hymn.

## Scope (Current)

- Login
  - Kakao SDK login + invite code input
  - Local verification account login path (currently UUID-based dev-login verification)
- Onboarding
  - First-login profile form (church/name/group)
  - Stored locally via SharedPreferences
- Hymn
  - List/search/filter
  - Detail view
  - PNG asset display (multi-page)
  - MIDI playback controls (play/pause/stop/speed)
- Personalization
  - Favorites toggle
  - Notes read/write
  - Recent history
- Offline
  - Cache fallback for list/detail/note/favorites/history
  - Pending actions sync when network returns
- Auth
  - Access/refresh token persistence
  - Automatic refresh + retry on 401

## Run

Preferred reproducible flow from repo root:

```powershell
.\scripts\local-bootstrap.ps1
.\scripts\run-mobile-emulator.ps1 -DeviceId emulator-5554
```

Manual flow:

```powershell
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get

# web
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1

# android emulator/device
..\..\scripts\flutterw.ps1 run -d emulator-5554 `
  --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1 `
  --dart-define=KAKAO_NATIVE_APP_KEY=<kakao_native_app_key>
```

Notes:
- `API_BASE_URL` auto-appends `/api/v1` if omitted.
- For physical devices, use reachable host IP/domain instead of `10.0.2.2`.
- Kakao callback scheme uses `kakao<KAKAO_NATIVE_APP_KEY>`.

## Structure

```text
lib/
  main.dart
  src/
    app.dart
    core/
      config/app_config.dart
      network/api_client.dart
      storage/token_storage.dart
      storage/onboarding_storage.dart
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

- Store release readiness: `.github/workflows/mobile-store-release.yml` (manual)
  - Android:
    - `android_distribution_mode=build_only`: signed AAB build artifact only
    - `android_distribution_mode=play_upload`: Google Play upload
  - iOS:
    - `ios_distribution_mode=build_only`: no-codesign release build
    - `ios_distribution_mode=testflight`: signed IPA + TestFlight upload
  - Current policy (2026-02-19): staging validation first, production publish only after approval.

See also:
- `docs/mobile/README.md`
- `docs/mobile/qa-minimal-tooling.md`
- `docs/TEAM_LOCAL_DEVELOPMENT.md`
