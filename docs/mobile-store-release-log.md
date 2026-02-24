# Mobile Store Release Log

Operational log for mobile store release workflow cycles.

| UTC Time | Repo | Ref | Target | AndroidMode | iOSMode | Preflight | RunId | Result | URL | Notes |
|----------|------|-----|--------|-------------|---------|-----------|-------|--------|-----|-------|
| 2026-02-20T03:39:55Z | V4N1LLA/Eunhye_Hymn | develop | android | build_only | build_only | PASS | 22210175274 | SUCCESS | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22210175274 | conclusion=success, head_sha=50520da1d88e6a22672c6a1778329422bf89016f |
| 2026-02-20T03:47:44Z | V4N1LLA/Eunhye_Hymn | develop | android | build_only | build_only | PASS | 22210322592 | SUCCESS | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22210322592 | conclusion=success, head_sha=50520da1d88e6a22672c6a1778329422bf89016f |
| 2026-02-23T23:24:42Z | V4N1LLA/Eunhye_Hymn | develop | android | play_upload | build_only | PASS | 22329008651 | FAILED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22329008651 | conclusion=failure, head_sha=561d6804433f39939224476bb903409f900c6edf |
| 2026-02-23T23:26:28Z | V4N1LLA/Eunhye_Hymn | develop | ios | build_only | testflight | PASS | 22329248773 | FAILED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22329248773 | conclusion=failure, head_sha=561d6804433f39939224476bb903409f900c6edf |
| 2026-02-24T12:57:05Z | V4N1LLA/Eunhye_Hymn | production | android | play_upload | build_only | PASS | 22351537681 | FAILED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22351537681 | conclusion=failure, head_sha=6e7f99301eaa877268d0f5726ecb69942df949bf, failure_key=android_play_upload, failed_step=Upload Android AAB to Google Play, recovery_hint=Verify GOOGLE_PLAY_SERVICE_ACCOUNT_JSON has Play Console release permissions for the package. |
| 2026-02-24T12:58:53Z | V4N1LLA/Eunhye_Hymn | production | ios | build_only | testflight | PASS | 22351821815 | FAILED | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22351821815 | conclusion=failure, head_sha=6e7f99301eaa877268d0f5726ecb69942df949bf, failure_key=ios_codesign_certificate_import, failed_step=Import Apple code-sign certificate, recovery_hint=Regenerate MOBILE_IOS_P12_BASE64 from a valid Apple Distribution certificate and verify MOBILE_IOS_P12_PASSWORD. |
| 2026-02-24T13:07:22Z | V4N1LLA/Eunhye_Hymn | production | both | build_only | build_only | PASS | 22351890816 | SUCCESS | https://github.com/V4N1LLA/Eunhye_Hymn/actions/runs/22351890816 | conclusion=success, head_sha=6e7f99301eaa877268d0f5726ecb69942df949bf |
