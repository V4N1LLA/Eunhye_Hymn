# Handover Update (2026-02-20)

## 1) Snapshot
- Captured at: 2026-02-20 (local timezone)
- Repo: `V4N1LLA/Eunhye_Hymn`
- Working branch for sync: `handover/2026-02-20-auth-qa-sync`
- Base branch: `develop`
- Purpose: hand over latest auth QA changes, recent PR context, and reproducible run steps for another machine.

## 2) Latest PRs Checked (2026-02-20)

| PR | State | Title | Head Branch | Key Points | URL |
|---|---|---|---|---|---|
| 112 | OPEN | fix: select newest matching workflow run in mobile cycle | `fix/pr111-review-followup` | Follow-up to PR #111. Run selection now prefers newest matching run after dispatch timestamp to avoid attaching to another operator run. | https://github.com/V4N1LLA/Eunhye_Hymn/pull/112 |
| 111 | MERGED | fix: harden run detection window in mobile store cycle | `fix/pr110-run-selection-window` | Added stricter dispatch window, branch filtering, and improved retry behavior for run detection in `scripts/mobile-store-cycle.ps1`. | https://github.com/V4N1LLA/Eunhye_Hymn/pull/111 |
| 110 | MERGED | feat(ops): automate mobile store release cycle and logging | `feat/mobile-store-cycle-automation` | Introduced mobile store preflight + cycle scripts and release log documentation. | https://github.com/V4N1LLA/Eunhye_Hymn/pull/110 |
| 109 | MERGED | feat(ops): recover preflight sessions via aws login fallback | `feat/staging-preflight-aws-login-fallback` | Added `aws login` fallback path when staging preflight session is expired/missing. | https://github.com/V4N1LLA/Eunhye_Hymn/pull/109 |
| 108 | MERGED | feat(ops): auto-recover staging preflight session and harden wait gate | `feat/staging-cycle-autoreauth` | Added `-AutoLogin` flow and hardened wait behavior in staging ops scripts. | https://github.com/V4N1LLA/Eunhye_Hymn/pull/108 |

## 3) Local Work Included In This Sync

### API/Auth behavior changes
- Added account withdrawal use case and endpoint:
  - `DELETE /me/account`
  - Disables account (`UserStatus.DISABLED`) and revokes active refresh tokens.
- Added disabled-account login block:
  - Social login and password login now return `account_disabled` when user is disabled.
  - JWT auth filter now checks user status for each access token request and clears auth context if user is disabled/missing.
- Expanded signup login ID validation to support email login IDs:
  - Existing legacy pattern still allowed.
  - Email format accepted if length is within 3..100.
  - Validation message updated accordingly.

### Mobile auth flow changes
- Login page UX changed:
  - Removed invite code input from first screen.
  - Changed entry button from inline account form to separate `EmailLoginPage`.
  - Label now uses email login wording.
- Added invite gate page after login/signup:
  - Social and email signup/login can be forced into invite-code step via `invalid_invite_code`.
  - Added small `withdraw` action in invite gate for no-code scenario.
- Moved first-use guidance text:
  - `Member/church info is first-use only` moved from login screen to onboarding profile input screen.
- Added mobile repository withdrawal call:
  - `AuthRepository.withdraw()` calls `/me/account` and clears local tokens.

### Tooling/script changes
- `scripts/run-mobile-emulator.ps1` now executes via `scripts/flutterw.ps1` wrapper instead of direct `flutter`.
- This keeps Android SDK/Flutter home context consistent across machines and avoids device detection mismatch.

## 4) File-Level Change Map

### API
- `apps/api/src/main/java/com/eunhyehymn/application/usecases/WithdrawMyAccountUseCase.java`
- `apps/api/src/main/java/com/eunhyehymn/application/usecases/SocialLoginUseCase.java`
- `apps/api/src/main/java/com/eunhyehymn/application/usecases/UserPasswordLoginUseCase.java`
- `apps/api/src/main/java/com/eunhyehymn/application/usecases/UserPasswordSignupUseCase.java`
- `apps/api/src/main/java/com/eunhyehymn/common/config/AuthConfig.java`
- `apps/api/src/main/java/com/eunhyehymn/common/config/UseCaseConfig.java`
- `apps/api/src/main/java/com/eunhyehymn/domain/repository/RefreshTokenRepository.java`
- `apps/api/src/main/java/com/eunhyehymn/infrastructure/persistence/RefreshTokenJpaRepository.java`
- `apps/api/src/main/java/com/eunhyehymn/infrastructure/persistence/RefreshTokenRepositoryAdapter.java`
- `apps/api/src/main/java/com/eunhyehymn/infrastructure/security/JwtAuthenticationFilter.java`
- `apps/api/src/main/java/com/eunhyehymn/presentation/controllers/AuthController.java`
- `apps/api/src/main/java/com/eunhyehymn/presentation/controllers/MeController.java`

### API tests
- `apps/api/src/test/java/com/eunhyehymn/presentation/controllers/MeProfileApiTest.java`
- `apps/api/src/test/java/com/eunhyehymn/presentation/controllers/UserPasswordAuthApiTest.java`

### Mobile
- `apps/mobile/lib/src/features/auth/auth_repository.dart`
- `apps/mobile/lib/src/features/auth/login_page.dart`
- `apps/mobile/lib/src/features/auth/onboarding_page.dart`
- `apps/mobile/lib/src/features/auth/sign_up_page.dart`
- `apps/mobile/lib/src/features/auth/email_login_page.dart`
- `apps/mobile/lib/src/features/auth/invite_gate_page.dart`

### Ops/docs/scripts
- `scripts/run-mobile-emulator.ps1`
- `docs/mobile-store-release-log.md`

## 5) Verification Executed

All commands below were run on 2026-02-20:

```powershell
# API targeted tests
./gradlew.bat test --no-daemon --tests "com.eunhyehymn.presentation.controllers.UserPasswordAuthApiTest" --tests "com.eunhyehymn.presentation.controllers.MeProfileApiTest"

# Mobile static check
..\..\scripts\flutterw.ps1 analyze

# Mobile tests
..\..\scripts\flutterw.ps1 test
```

Observed results:
- API test command: `BUILD SUCCESSFUL`
- Flutter analyze: `No issues found`
- Flutter test: `All tests passed`

## 6) Known Environment Status (Important)

- Staging API currently still returns old login ID validation for email signup.
  - Endpoint checked: `http://13.209.200.12/api/v1/auth/signup`
  - Returned code: `invalid_credential_format`
- Local updated API returns invite-code validation instead (expected for email format acceptance).
  - Endpoint checked: `http://localhost:18080/api/v1/auth/signup`
  - Returned code: `invalid_invite_code`

Interpretation:
- Mobile changes are ready, but staging backend deployment is required for end-to-end email-signup behavior in staging.

## 7) Fast Start On Another Computer

```powershell
# 1) Clone and checkout synced branch
git clone https://github.com/V4N1LLA/Eunhye_Hymn.git
cd Eunhye_Hymn
git fetch origin
git switch handover/2026-02-20-auth-qa-sync

# 2) Bootstrap local dependencies (if needed)
.\scripts\local-bootstrap.ps1

# 3) Start local API with WSL postgres helper
.\scripts\run-api-local-wsl-db.ps1

# 4) Start emulator app against local API from staging profile
.\scripts\run-mobile-emulator.ps1 -Environment staging -DeviceId emulator-5554 -ApiBaseUrl http://10.0.2.2:18080/api/v1
```

Optional keyboard usability command (emulator):

```powershell
.\.flutter-home\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 shell settings put secure show_ime_with_hard_keyboard 1
```

## 8) Recommended Next Steps
1. Deploy API changes to staging and re-run email signup QA there.
2. Open/update PR for this handover branch into `develop`.
3. After merge, run one more staging smoke cycle and append evidence logs.
