# Mobile Store Release Recovery Guide

This guide standardizes recovery steps when `mobile-store-release.yml` fails in `play_upload` or `testflight` mode.

## 1. Quick Start

1. Run preflight checks for the same target/mode.
   - `.\scripts\mobile-store-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -Target android -AndroidDistributionMode play_upload`
   - `.\scripts\mobile-store-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -Target ios -IosDistributionMode testflight`
2. Diagnose a failed workflow run.
   - `.\scripts\mobile-store-diagnose.ps1 -Repo V4N1LLA/Eunhye_Hymn -RunId <run_id>`
3. Apply credential/config fixes from the diagnosis output.
4. Re-run `mobile-store-cycle.ps1` with the same target/mode.

## 2. Failure-Step Mapping

| Failed step | Typical root cause | First recovery action |
|---|---|---|
| `Upload Android AAB to Google Play` | Service account/package/track permission mismatch | Re-issue `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` and verify package/track |
| `Validate Android signing secrets` | Missing or empty Android signing secrets | Re-sync `MOBILE_ANDROID_*` and rerun preflight |
| `Prepare Android keystore` | Corrupted base64 or alias/password mismatch | Re-export keystore base64 and verify alias/password pair |
| `Import Apple code-sign certificate` | Invalid/expired p12 or wrong password | Re-export p12, rotate `MOBILE_IOS_P12_*`, confirm Team ID match |
| `Download App Store provisioning profile` | Bundle id/API key entitlement mismatch | Validate `MOBILE_IOS_BUNDLE_ID` + App Store API key scope |
| `Upload to TestFlight` | App Store Connect auth/metadata mismatch | Verify API key permissions and app record settings |

## 3. Script Responsibilities

- `scripts/mobile-store-preflight.ps1`
  - checks mode-specific required secrets/inputs before dispatch
  - prints per-check recovery hints when validation fails
- `scripts/mobile-store-diagnose.ps1`
  - reads a failed run from GitHub Actions
  - resolves failed job/step and emits recovery actions
- `scripts/mobile-store-cycle.ps1`
  - appends failure diagnosis (`failure_key`, `failed_step`, `recovery_hint`) to log notes on failure

## 4. Evidence and Logging

- Store cycle logs are appended to `docs/mobile-store-release-log.md`.
- Keep failed run URLs and diagnosis results in the same cycle for RCA continuity.
