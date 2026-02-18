# Android QA With Minimal Local Tooling

## Goal

Run Android staging and production QA with CI artifacts so local IDE installs are optional.

## Minimal local setup

- QA operator PC:
  - `git`
  - `gh` (GitHub CLI)
  - optional `adb` (only if you want direct APK install by USB)
- Android tester device:
  - no IDE required
  - install APK from artifact share link or `adb install`

## Staging QA flow

1. Verify latest staging deploy gate.

```powershell
.\scripts\staging-latest-status.ps1 `
  -Repo V4N1LLA/Eunhye_Hymn `
  -RequireSuccess `
  -RequireDeploySuccess `
  -RequireVerifySuccess `
  -MaxAgeMinutes 120
```

2. Build Android staging APK in CI and distribute to testers.

```powershell
gh workflow run mobile-release-check.yml -f api_base_url=http://13.209.200.12
gh run list --workflow mobile-release-check.yml --limit 1
gh run download <run-id> -n mobile-android-release-apk -D .tmp/mobile-apk
```

Optional direct install:

```powershell
adb install -r .tmp/mobile-apk/app-release.apk
```

3. Build Android signed AAB in CI (store readiness).

```powershell
gh workflow run mobile-store-release.yml `
  -f target=android `
  -f android_distribution_mode=build_only `
  -f api_base_url=http://13.209.200.12
```

4. Execute manual QA checklist and record result.

- `docs/staging-smoke-checklist.md`
- `docs/staging-feedback-checklist.md`
- `docs/staging-smoke-log.md`

## Production QA flow

Use the same flow with production API URL and release candidate commit.

Recommended sequence:

1. Run `mobile-release-check.yml` with production `api_base_url` to verify APK release build.
2. Run `mobile-store-release.yml` with production `api_base_url` to generate signed AAB.
3. If you also want Play Console upload automation, run:

```powershell
gh workflow run mobile-store-release.yml `
  -f target=android `
  -f android_distribution_mode=play_upload `
  -f android_track=internal `
  -f android_release_status=draft `
  -f android_changes_not_sent_for_review=true `
  -f api_base_url=https://<prod-domain>
```

## Notes

- Android store signing secrets:
  - `MOBILE_ANDROID_KEYSTORE_BASE64`
  - `MOBILE_ANDROID_KEY_ALIAS`
  - `MOBILE_ANDROID_KEY_PASSWORD`
  - `MOBILE_ANDROID_STORE_PASSWORD`
- Google Play upload secrets:
  - `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
  - `GOOGLE_PLAY_PACKAGE_NAME`
