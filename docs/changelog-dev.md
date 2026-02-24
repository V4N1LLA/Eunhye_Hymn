# 개발 변경 이력

작업 단위별 핵심 변경만 기록한다. 상세 구현은 각 PR 본문과 커밋 로그를 참고한다.

## 2026-02-24

### Review-comment refactor follow-up
- Script reliability hardening
  - `scripts/ops-health-cycle.ps1`
  - `scripts/staging-ops-cycle.ps1`
  - `scripts/secrets-rotation-cycle.ps1`
  - Switched nested script invocation to cross-platform PowerShell command resolution (`powershell.exe`/`pwsh`)
- Failure-category precision update
  - `scripts/ops-health-cycle.ps1`
  - Narrowed manual-smoke recency detection to explicit recency failure signals only
- Safety guard for skill sync
  - `scripts/sync-skills.ps1`
  - Added source/destination overlap guard to prevent destructive self-sync path combinations
- Workflow input correctness
  - `.github/workflows/secrets-rotation-scheduled.yml`
  - Preserved explicit `false` for `include_mobile_release_secrets` on manual dispatch
- Skill reference sync
  - `skills/secrets-rotation-auditor/references/commands.md`
  - Added required parameters to the `staging-sync-secrets.ps1` command example

### Ops script compatibility follow-up
- Cross-platform PowerShell invocation hardening
  - `scripts/mobile-store-cycle.ps1`
  - `scripts/staging-rehearsal.ps1`
  - `scripts/run-mobile-emulator.ps1`
  - Replaced remaining direct `powershell.exe` script-launch usage with `powershell.exe`/`pwsh` resolution
- Rehearsal run-detection reliability
  - `scripts/staging-rehearsal.ps1`
  - Added baseline run-id tracking and earliest-post-dispatch selection to reduce concurrent workflow run misattribution
- Workflow dispatch boolean hardening
  - `.github/workflows/deploy-staging.yml`
  - `.github/workflows/ops-health-scheduled.yml`
  - Preserved explicit `false` values for workflow_dispatch boolean inputs (`enable_awslogs`, `include_mobile_release_secrets`, `wait_for_completion`)

### Preflight failure diagnostics hardening
- `scripts/mobile-store-cycle.ps1`
  - Added first-useful-line extraction for preflight failures and propagated detail into log notes/exception message
- `scripts/staging-rehearsal.ps1`
  - Captured preflight output and surfaced first-useful-line detail in log notes/exception message

### Ops log dedup hardening
- `scripts/staging-ops-cycle.ps1`
  - Added log dedup window (`-LogDedupWindowMinutes`, default 30) to skip repeated writes for the same run/gate decision within a short interval
- `scripts/ops-health-cycle.ps1`
  - Added log dedup window (`-LogDedupWindowMinutes`, default 30) for repeated consolidated health rows
  - Passed dedup window through to nested `staging-ops-cycle.ps1` invocation

### Admin test gate baseline (P1)
- Added minimal unit test runner and first test target
  - `apps/admin/package.json`
  - `apps/admin/vitest.config.ts`
  - `apps/admin/src/auth/tokenStore.test.ts`
  - Added `npm run test` (Vitest + jsdom), and validated token migration/storage behavior with unit tests
- Extended Admin CI quality gate
  - `.github/workflows/admin-ci.yml`
  - Added test step: `npm ci` -> `npm run test` -> `npx tsc --noEmit` -> `npm run build`
- Synced command/docs references for Admin checks
  - `AGENTS.md`
  - `docs/dev-guide.md`
  - `docs/LOCAL_SETUP.md`
  - `CLAUDE.md`

### API complex use case test hardening (P1)
- Added exception-path coverage for event export job use case
  - `apps/api/src/test/java/com/eunhyehymn/application/usecases/AdminEventExportJobUseCaseTest.java`
  - Added tests for:
    - failed processing path with long error-message truncation (500 chars)
    - `getDownload` failure mapping (`FAILED` -> `409 export_job_failed`)
    - `getDownload` corruption guard (`COMPLETED` with missing payload -> `500 export_job_corrupted`)
- Added validation/error-path coverage for AI recommendation use case
  - `apps/api/src/test/java/com/eunhyehymn/application/usecases/RecommendHymnsUseCaseTest.java`
  - Added tests for:
    - blank situation validation (`400 validation_error`)
    - external AI exception mapping (`503 ai_unavailable`)
    - result clamp/sanitization behavior (maxResults clamp, duplicate/unknown recommendation filtering, reason length cap)
- Verification
  - `cd apps/api && ./gradlew test --tests "*AdminEventExportJobUseCaseTest*" --tests "*RecommendHymnsUseCaseTest*" --no-daemon --stacktrace`

### PR gate Admin test enforcement follow-up
- Fixed CI gap where PR gate did not execute Admin unit tests
  - `.github/workflows/pr-gate.yml`
  - Updated `admin_build` job flow to: `npm ci` -> `npm run test` -> `npx tsc --noEmit` -> `npm run build`

### Doc sync gate automation
- Added changed-file-based doc sync checker
  - `scripts/check-doc-sync.ps1`
  - Enforces `docs/changelog-dev.md` + `CLAUDE.md` updates when non-doc files are changed
  - Supports `-ChangedFiles` input and `git diff` mode via `-BaseRef/-HeadRef`
- Integrated checker into PR gate
  - `.github/workflows/pr-gate.yml`
  - Added `doc_sync` job and included its result in the final `gate` decision
- Workflow lint coverage update
  - `.github/workflows/workflow-lint.yml`
  - Added syntax check target for `scripts/check-doc-sync.ps1`
- Docs sync
  - `AGENTS.md`
  - `CLAUDE.md`

### Mobile store failure diagnosis + recovery runbook
- Added failed-run diagnosis script
  - `scripts/mobile-store-diagnose.ps1`
  - Extracts failed job/step from GitHub Actions run and maps to recovery actions
- Enhanced mobile store cycle/preflight diagnostics
  - `scripts/mobile-store-cycle.ps1`
    - Appends failure diagnosis (`failure_key`, `failed_step`, `recovery_hint`) to log notes when run fails
  - `scripts/mobile-store-preflight.ps1`
    - Prints check-specific recovery hints for missing/invalid prerequisites
- CI syntax-check coverage update
  - `.github/workflows/pr-gate.yml`
  - `.github/workflows/workflow-lint.yml`
  - Added `scripts/mobile-store-diagnose.ps1` to PowerShell syntax validation target list
- Added recovery documentation
  - `docs/mobile-store-recovery.md`
  - `docs/mobile/README.md`
  - `apps/mobile/README.md`
  - `docs/runbook.md`

## 2026-02-23

### Staging manual smoke recency gate + log automation
- 스크립트 개선
  - `scripts/staging-ops-cycle.ps1`
  - 수동 스모크 최신성 게이트 추가 (`-ManualSmokeMaxAgeDays`, 기본 7일)
  - 최신 수동 스모크(PASS/FAIL) 기록이 없거나 오래된 경우 `Manual Smoke=OVERDUE` + `HOLD` 판정
  - 수동 스모크 결과 자동 기록 파라미터 추가:
    - `-ManualSmokeResult PASS|FAIL`
    - `-ManualSmokeEvidence <URL>`
    - `-ManualSmokeNotes "<요약>"`
- 운영 문서 동기화
  - `docs/runbook.md`
  - `docs/staging-smoke-checklist.md`
  - `infra/aws/README.md`
  - `docs/current-usable-scope.md`

### Secrets rotation cycle log automation
- 스크립트 추가
  - `scripts/secrets-rotation-cycle.ps1`
  - `staging-secret-rotation-audit.ps1` 결과(JSON)를 수집해 `PASS/HOLD` 판정
  - 결과 요약(`OK/STALE/MISSING/UNKNOWN`)을 Markdown 로그에 자동 누적
- 로그 문서 추가
  - `docs/secrets-rotation-log.md`
- 운영 문서 동기화
  - `docs/SECRETS_MANAGEMENT.md`
  - `docs/runbook.md`
  - `infra/aws/README.md`
  - `docs/current-usable-scope.md`

### Ops health cycle + exception-aware monitoring hardening
- 스크립트 추가
  - `scripts/ops-health-cycle.ps1`
  - staging 게이트(`staging-ops-cycle`) + 시크릿 점검(`secrets-rotation-cycle`)을 통합 실행
  - 실패 원인 분류(`FailureCategory`) 및 통합 판정(`PASS/HOLD/ERROR`) 로그 자동 누적
- 운영 로그 문서 추가
  - `docs/ops-health-log.md`
- 예외/디버깅 하드닝
  - `scripts/staging-ops-cycle.ps1`: `-AsJson` 추가, status 조회 실패/JSON 파싱 실패 시 구조화 에러 출력 지원
  - `scripts/secrets-rotation-cycle.ps1`: `-AsJson` 추가, 집계 결과/세부 목록 JSON 출력 지원
- 운영 문서 동기화
  - `docs/runbook.md`
  - `docs/SECRETS_MANAGEMENT.md`
  - `infra/aws/README.md`
  - `docs/current-usable-scope.md`

### Ops full-cycle follow-up (8 tasks executed)
- 스크립트 하드닝
  - 로그 경로 자동 생성 보강:
    - `scripts/ops-health-cycle.ps1`
    - `scripts/staging-ops-cycle.ps1`
    - `scripts/secrets-rotation-cycle.ps1`
    - `scripts/staging-rehearsal.ps1`
    - `scripts/mobile-store-cycle.ps1`
- HOLD/ERROR 알림 연동
  - `scripts/ops-health-issue-alert.ps1` 추가
  - `scripts/ops-health-cycle.ps1`에 `-AlertOnFailure`/`-AlertRepo`/`-AlertDryRun` 추가
- 스케줄 자동화
  - `.github/workflows/ops-health-scheduled.yml` 추가 (주간)
  - `.github/workflows/secrets-rotation-scheduled.yml` 추가 (월간)
- 운영 로그 증빙
  - `docs/secrets-rotation-log.md`: `MISSING=0`, `PASS` 기록
  - `docs/staging-smoke-log.md`: `ManualSmoke=PASS` 기록으로 recency 복구
  - `docs/ops-health-log.md`: 통합 `PASS` 기록

### Mobile store publish-path verification
- preflight 결과
  - Android `play_upload`: PASS
  - iOS `testflight`: PASS
- 사이클 실행 결과
  - Android `play_upload`: run `22329008651` 실패 (`Upload Android AAB to Google Play`)
  - iOS `testflight`: run `22329248773` 실패 (`Import Apple code-sign certificate`)
- 실행 로그 문서
  - `docs/mobile-store-release-log.md`
  - `docs/mobile/README.md`
  - `apps/mobile/README.md`

### AI recommendation ops metrics baseline
- 코드 반영
  - `apps/api/src/main/java/com/eunhyehymn/presentation/controllers/AiController.java`
    - `ai_recommend_requests_total`
    - `ai_recommend_latency_seconds`
    - `ai_recommend_fallback_total`
    - `ai_recommend_candidate_count`
    - `ai_recommend_response_items`
  - `apps/api/src/main/java/com/eunhyehymn/application/usecases/RecommendHymnsUseCase.java`
    - `Result.fallbackUsed` 추가
- 테스트 추가
  - `apps/api/src/test/java/com/eunhyehymn/application/usecases/RecommendHymnsUseCaseTest.java`
- 문서 동기화
  - `docs/usecases/ai-hymn-recommendations.md`
  - `docs/dev-guide.md`

## 2026-02-22

### Staging secret rotation audit automation
- 스크립트 추가
  - `scripts/staging-secret-rotation-audit.ps1`
  - GitHub Actions repo secrets의 `updatedAt` 기준으로 `OK/STALE/MISSING/UNKNOWN` 판정
  - 기본 스테이징 필수 시크릿 점검 + `-IncludeMobileReleaseSecrets` 옵션으로 모바일 배포 시크릿 확장
  - `-MaxAgeDays` 임계값 기반으로 초과 시 실패(exit 1) 처리
- 운영 문서 동기화
  - `docs/runbook.md`
  - `docs/SECRETS_MANAGEMENT.md`
  - `infra/aws/README.md`

## 2026-02-20

### Auth invite-gated login sync + `/me/account` withdraw endpoint
- API/Auth
  - `UserPasswordSignupUseCase` 로그인 ID 검증에 이메일 형식(3~100자) 허용
  - `MeController`에 `DELETE /me/account` 추가 (기존 `POST /auth/withdraw`와 동일 탈퇴 처리)
  - `AuthController` social login 예외 매핑 정리 (`account_disabled` 유지)
- Mobile auth UX
  - 로그인 화면에서 이메일/카카오 진입 분리 (`login_page.dart` + `email_login_page.dart`)
  - 이메일 회원가입 시 초대코드 확인을 별도 단계(`invite_gate_page.dart`)로 분리
  - 온보딩 화면에 "회원/교회 정보 1회 입력" 안내 문구 추가
  - `AuthRepository.withdraw()`가 `/me/account` 호출 후 로컬 토큰 정리
- 스크립트
  - `scripts/run-mobile-emulator.ps1`가 `scripts/flutterw.ps1` 래퍼 경유로 실행되도록 변경
- 검증
  - `./gradlew.bat test --no-daemon --tests "com.eunhyehymn.presentation.controllers.UserPasswordAuthApiTest" --tests "com.eunhyehymn.presentation.controllers.MeProfileApiTest"`
  - `..\..\scripts\flutterw.ps1 analyze`
  - `..\..\scripts\flutterw.ps1 test`

### Documentation full sync (env/API/data-model)
- 문서 기준선 재정렬
  - `README.md`, `docs/current-usable-scope.md`, `current_update.md`
  - `docs/api-contract.md`, `docs/data-model.md`, `docs/requirements.md`
  - `docs/LOCAL_SETUP.md`, `docs/dev-guide.md`, `docs/SECRETS_MANAGEMENT.md`
  - `docs/admin/README.md`, `docs/usecases/README.md`
  - `infra/docker/README.md`, `infra/aws/README.md`, `docs/runbook.md`
- 반영 내용
  - 인증 플로우(초대코드/SMS/탈퇴), 개인정보 변경 요청, AI 추천 엔드포인트 문서화
  - AI Gemini Flash-Lite 저비용 기본값 및 `AI_*` 환경변수 반영
  - Flyway 마이그레이션(`V1`~`V16`) 기준 데이터 모델 최신화

### Mobile store release cycle automation + Android build-only verification
- 스크립트 추가
  - `scripts/mobile-store-preflight.ps1`
    - target/mode 기준으로 필수 시크릿과 입력값 사전 점검
  - `scripts/mobile-store-cycle.ps1`
    - preflight + `mobile-store-release.yml` workflow_dispatch + run watch + 로그 적재 자동화
- 실행 로그 문서 추가
  - `docs/mobile-store-release-log.md`
- 운영 검증
  - Android build_only 워크플로우 2회 성공
    - run `22210175274`
    - run `22210322592`
  - iOS testflight preflight는 필수 시크릿(`MOBILE_IOS_*`) 미구성으로 실패 확인
- 문서 동기화
  - `apps/mobile/README.md`, `docs/mobile/README.md`, `docs/current-usable-scope.md`, `CLAUDE.md`

### Staging preflight 자동 복구 + 운영 사이클 게이트 보강
- 스크립트 개선
  - `scripts/staging-preflight.ps1`
    - `-AutoLogin` 옵션 추가
    - `aws sts get-caller-identity` 실패 시(`session expired`/`credentials missing`) `aws sso login` 자동 재시도 지원
    - 후속 보강: `sso_start_url` 미설정 환경에서도 `aws login` fallback 재시도 지원
    - 자동 복구 결과를 체크 테이블(`aws session recovery`)에 기록
  - `scripts/staging-ops-cycle.ps1`
    - `-AutoLogin` 옵션 추가(내부 preflight로 전달)
    - `-WaitForCompletion` 사용 시 진행 중 최신 run 완료 대기 경로를 실제 활성화
  - `scripts/staging-rehearsal.ps1`
    - `-AutoLogin` 옵션 추가(내부 preflight로 전달)
  - `scripts/staging-latest-status.ps1`
    - `-AsJson/-AsMarkdown + -Wait` 조합에서 `gh run watch` 출력이 JSON/Markdown 파싱을 깨지 않도록 출력 분리
    - `gh run watch` 실패 시 종료코드 기반 예외 처리 추가
- 운영 실행 증빙
  - `scripts/staging-ops-cycle.ps1 -AutoLogin -WaitForCompletion` 실행
  - run `22206920873` 기준 deploy/verify PASS, preflight FAIL(session recovery unavailable)로 `HOLD` 기록
  - `aws logout --profile default` 후 `staging-preflight.ps1 -AutoLogin` 재실행으로 `aws login` fallback 자동 복구 PASS 확인
  - run `22207213127` 기준 preflight/deploy/verify PASS, `CONDITIONAL_GO` 갱신
  - 반영 문서: `docs/staging-smoke-log.md`, `docs/staging-smoke-checklist.md`
- 문서 동기화
  - `docs/runbook.md`, `infra/aws/README.md`, `docs/current-usable-scope.md`, `CLAUDE.md`
  - AutoLogin/WaitForCompletion 표준 명령 및 주간 운영 사이클 규칙 반영

### Mobile IA refresh + profile approval workflow + verification flow hardening
- Mobile app UX refresh
  - Replaced bottom tab layout with top-left drawer navigation and removed redundant top section headers.
  - Added home back-press guard: first back shows a bottom message, second back within the window exits app.
  - Login CTA updated to Kakao icon + `카카오로 시작하기`; email login entry restored.
  - Settings copy rewritten to plain language and profile sub-pages split into focused screens.
- Member profile workflow
  - Added gender selection in onboarding/profile flow with `UNKNOWN` compatibility state for existing accounts.
  - Added member-side profile change request flow:
    - `POST /me/profile-change-requests`
    - `GET /me/profile-change-requests/latest`
  - Added admin review flow:
    - `GET /admin/profile-change-requests`
    - `PATCH /admin/profile-change-requests/{id}`
  - Added admin page wiring for profile change request handling.
- Verification + recommendation extensions
  - Added invite+SMS verification steps in mobile auth flow (`invite -> phone -> sms`).
  - Added AI hymn recommendation endpoint/client wiring and admin/mobile UI entry points.
- Runtime/error fixes
  - Fixed `SegmentedButton` assertion by ensuring gender selection set is never empty and mapping legacy missing gender to `UNKNOWN`.
  - Confirmed mobile analyze/test and API/admin build checks pass locally before staging deployment.

## 2026-02-17

### Staging 운영 사이클 자동화 + Mobile store readiness
- 스테이징 운영 사이클 자동 점검 스크립트 추가
  - `scripts/staging-ops-cycle.ps1`
  - preflight + 최신 deploy 게이트 + 로그 기록(`docs/staging-smoke-log.md`) 일괄 실행
- 스크립트 안정화
  - `scripts/staging-preflight.ps1`: AWS 세션 만료 케이스를 `session expired`로 명확히 안내
  - `scripts/staging-latest-status.ps1`: 진행 중 job의 `completedAt=0001-01-01` 처리로 음수 duration 방지
- 스모크/운영 문서 동기화
  - `docs/staging-smoke-checklist.md`, `docs/staging-feedback-checklist.md`, `docs/runbook.md`, `infra/aws/README.md`
  - 실행 기록: `deploy-staging` run `22119056042` 게이트 PASS, preflight는 AWS 세션 만료로 HOLD
- 모바일 배포 준비도 보강
  - Android release signing 설정(`apps/mobile/android/app/build.gradle.kts`) 개선
  - `apps/mobile/android/key.properties.example` 추가
  - `.github/workflows/mobile-store-release.yml` 추가 (manual: android signed AAB / ios no-codesign)
  - PR 리뷰 코멘트 반영: Android keystore 생성 경로를 `android/app/keystore`로 수정
  - 모바일 문서 동기화 (`apps/mobile/README.md`, `docs/mobile/README.md`)
- 기준 문서 동기화
  - `README.md`, `docs/current-usable-scope.md`, `docs/deployment-readiness-audit.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`

### 문서 최신화 동기화
- 기준 범위 문서 최신화
  - `docs/current-usable-scope.md` 기준 커밋을 `c578c3f`로 갱신
  - 근거 PR에 #91/#90/#89/#88/#87 반영
  - 최신 실행 증빙(run `22052664286`, `22052482140`) 반영
- 배포 준비도 점검 리포트 최신화
  - `docs/deployment-readiness-audit.md` 작성일/점검 브랜치/최신 Actions 실행 근거 갱신
  - 잔여 리스크(수동 스모크 정례화 필요) 기준 명확화
- 루트 문서 정합성 보강
  - `README.md`의 CI/CD 설명에 Mobile release check + Staging 자동 배포 반영
- 기준 문서 동기화
  - `CLAUDE.md`, `docs/WORK_CYCLE.md` 업데이트

## 2026-02-16

### Mobile UI/UX 개선 (PR #89)
- Hymn 목록/History 화면의 탐색 UX 개선
  - 검색어 즉시 삭제 버튼
  - 태그/기간 필터
  - 필터 초기화 액션
  - 빈 상태 가이드/soft error 배너
- 반영 파일
  - `apps/mobile/lib/src/features/hymn/hymn_list_page.dart`
  - `apps/mobile/lib/src/features/history/history_page.dart`
  - `docs/mobile/README.md`
  - `CLAUDE.md`
  - `docs/WORK_CYCLE.md`
- merge: `0520074` (`feat(mobile): improve hymn list and history usability (#89)`)

### Mobile release readiness (PR #90)
- Android release APK 빌드 검증 워크플로우 추가
  - `.github/workflows/mobile-release-check.yml`
  - PR/develop push + workflow_dispatch 트리거
  - `app-release.apk` artifact 업로드
- 문서/기준 동기화
  - `docs/mobile/README.md` (Release 패키징 준비 섹션)
  - `CLAUDE.md` (CI/미완료 상태 반영)
  - `docs/WORK_CYCLE.md` (cycle #34 반영)
- 저장소 정리
  - `apps/mobile/android/.gitignore`에 `/.kotlin` 추가
- merge: `35569af` (`ci(mobile): add android release build readiness check (#90)`)
## 2026-02-15

### Admin CRUD 예외 하드닝 + 운영자 UX 개선
- API
  - 사용자/찬양 관리 요청의 입력 정규화 및 빈 PATCH(`empty_update`) 방어 추가
  - 사용자 생성 이름 길이 제한(64자), 사용자 삭제/수정 트랜잭션 보강
  - Admin principal 파싱 실패 시 `invalid_principal`로 명시적 401 반환
- Admin UI
  - 사용자/찬양 관리에 통계 카드, 새로고침/재시도, 성공/실패 피드백, 필터 초기화 추가
  - 사용자 관리의 잠금 사유(현재 운영자/마지막 활성 관리자) 가시성 강화
  - 찬양 번호 입력을 문자열 기반으로 정리(서식 보존), 생성/수정 폼 입력 검증 강화
- 테스트
  - `AdminUserApiTest`, `AdminHymnApiTest`에 empty payload/정규화/잘못된 principal 케이스 추가

### 스테이징 수동 검수 문서 업데이트
- `docs/staging-smoke-checklist.md`
  - 관리자 웹 스모크 단계에 사용자 CRUD/찬양 CRUD 상세 점검 항목 추가
  - 관리자 로그인 방식 참조 문서(`docs/staging-admin-login.md`) 연결
- `README.md`, `docs/api-contract.md`, `docs/current-usable-scope.md`
  - 사용자 관리 API 범위(`POST/DELETE /admin/users`) 및 현재 검증 가능 범위 설명 동기화

## 2026-02-14

### Login token input clarification for staging
- `apps/admin/src/pages/LoginPage.tsx`
  - Login token label is now provider-specific:
    - Kakao: `Access Token`
  - Added token guidance text to prevent Kakao ID-token misuse.
  - Dev login section is now shown only on localhost (`localhost`, `127.0.0.1`) to avoid staging confusion.

### 스테이징 상태 조회 스크립트 게이트 강화(27차)
- `scripts/staging-latest-status.ps1` 옵션 확장
  - `-RequireDeploySuccess` 추가: `deploy` job 결론이 `success`가 아니면 실패 처리
  - `-RequireVerifySuccess` 추가: `Verify deployment` step 결론이 `success`가 아니면 실패 처리
  - `-MaxAgeMinutes` 추가: 최신 run이 허용 시간보다 오래된 경우 실패 처리
  - run 요약에 `RunAgeMinutes` 추가, Markdown 출력에 `AgeMin` 컬럼 추가
- 운영 문서 반영
  - `docs/runbook.md`, `docs/staging-smoke-checklist.md`, `infra/aws/README.md`에 강화된 상태 확인 명령 반영
- 효과
  - 스모크 테스트 진입 전에 "성공 여부 + 검증 단계 성공 + 실행 신선도"를 종료코드 기반으로 강제 가능
  - 오래된 성공 run을 최신 상태로 오인해 검수를 진행하는 리스크 감소

### 스테이징 상태 조회 스크립트 Markdown 출력 추가(26차)
- `scripts/staging-latest-status.ps1` 옵션 확장
  - `-AsMarkdown` 추가: run 요약 + job 상태를 Markdown 테이블 형태로 출력
  - `-AsJson`과 동시 사용 시 오류 처리(출력 모드 충돌 방지)
- 운영 문서 반영
  - `docs/runbook.md`: `-AsMarkdown` 옵션 안내 추가
  - `docs/staging-smoke-checklist.md`: 실행 전 준비 항목에 Markdown 출력 옵션 안내 추가
  - `infra/aws/README.md`: 리허설 가이드에 Markdown 출력 옵션 안내 추가
- 효과
  - 스테이징 점검 결과를 문서/티켓에 붙여 넣을 때 형식 정리가 쉬워져
    검수 기록 작성 시간을 줄임

### 스테이징 상태 조회 스크립트 필터 확장(25차)
- `scripts/staging-latest-status.ps1` 옵션 확장
  - `-Branch`(기본 `develop`) 추가: 대상 브랜치 기준 최신 run 조회
  - `-Event` 추가: `push`/`workflow_dispatch` 등 이벤트 유형 필터링 조회 지원
- 운영 문서 반영
  - `docs/runbook.md`: 브랜치/이벤트 필터 옵션 안내 추가
  - `infra/aws/README.md`: 리허설 run 확인 시 `-Branch`, `-Event` 사용 가이드 추가
- 효과
  - 자동 배포(push)와 리허설(workflow_dispatch) run을 목적별로 분리 조회할 수 있어,
    스테이징 상태 확인 결과의 맥락 혼동을 줄임

### 스테이징 최신 상태 조회 스크립트 추가(24차)
- 신규 스크립트: `scripts/staging-latest-status.ps1`
  - 최신 `deploy-staging.yml` run의 상태/결론/커밋/URL 출력
  - `deploy` job과 `Verify deployment` step 결론을 함께 요약
  - `-RequireSuccess` 옵션으로 최신 run 성공 여부를 종료코드로 강제
  - `-AsJson` 옵션으로 자동화 파이프라인에서 소비 가능한 JSON 출력 지원
- 운영 문서 반영
  - `docs/runbook.md`: 배포 체크리스트/절차에 상태 조회 명령 추가
  - `docs/staging-smoke-checklist.md`: 실행 전 준비 항목에 상태 조회 명령 추가
  - `infra/aws/README.md`: 리허설 가이드 섹션에 상태 조회 스크립트 추가
- 효과
  - 스모크 테스트 시작 전에 최신 배포의 성공 여부와 verify 통과 여부를 빠르게 확인할 수 있어
    스테이징 수동 검수 진입 판단이 단순해짐

### API CI 트리거/동시성 정비(23차)
- `api-ci.yml` 트리거 보강
  - `apps/api/**` 변경 외에 `.github/workflows/api-ci.yml` 변경 시에도 API CI가 실행되도록 path 추가
- `api-ci.yml` 동시성 제어 추가
  - `concurrency.group: api-ci-${{ github.ref }}`
  - `cancel-in-progress: true`로 동일 브랜치의 이전 API CI를 자동 취소
- 효과
  - API CI 워크플로우 자체 수정이 검증에서 누락되는 케이스를 차단하고,
    연속 push 시 불필요한 중복 실행을 줄여 피드백 시간을 단축

### API Gradle 캐시 안정화(22차)
- `deploy-staging.yml`의 API 테스트 단계 캐시 전략 변경
  - `gradle/actions/setup-gradle@v3` 대신 `actions/setup-java@v4`의 `cache: gradle` 사용
  - `cache-dependency-path`를 `apps/api` Gradle 파일/Wrapper 기준으로 명시
- `api-ci.yml`의 API CI 테스트 단계 캐시 전략 동일화
  - 배포 파이프라인과 동일하게 `setup-java` 내장 Gradle 캐시로 통일
- 효과
  - `setup-gradle` restore 시 간헐적으로 발생하던 캐시 400 경고 노이즈를 줄이고,
    API 테스트 캐시 설정을 두 워크플로우에서 동일하게 유지

### 스테이징 verify SSH 키 경로 hotfix(21차)
- `infra/aws/verify-staging.sh` 경로 정규화 수정
  - `~`/`~/...` 입력을 `HOME` 기준 절대 경로로 정확히 치환하도록 `expand_home_path` 로직 보정
  - 치환 후 키 파일 존재 여부 확인 로직은 유지
- `deploy-staging.yml` verify 단계 환경 변수 수정
  - `STAGING_SSH_KEY`를 `~/.ssh/deploy_key` 대신 `/home/runner/.ssh/deploy_key`로 명시
- 효과
  - verify 단계가 SSH 키 경로를 오인식해 즉시 실패하던 회귀를 제거하고, 배포 검증 단계 안정성을 복구

### 스테이징 검증 스크립트 분리/하드닝(20차)
- `deploy-staging.yml` 검증 단계 리팩토링
  - 인라인 verify 로직을 `infra/aws/verify-staging.sh`로 분리
  - 워크플로우는 `STAGING_HOST`, `STAGING_SSH_KEY`, `EXPECTED_IMAGE_TAG`를 주입해 스크립트를 호출
- `deploy-staging.yml` 배포 안정성 보강
  - `deploy` job에 `timeout-minutes: 30` 추가
  - 배포 전 원격 `/home/ec2-user/app` 디렉터리를 `mkdir -p`로 보장
- `workflow-lint.yml` 검증 범위 확장
  - path filter에 `infra/aws/verify-staging.sh` 추가
  - `bash -n infra/aws/verify-staging.sh` 문법 검증 추가
- 효과
  - 배포 검증 로직을 단일 스크립트로 재사용 가능하게 정리해 변경 추적성과 유지보수성을 높이고,
    신규/재생성 EC2에서도 경로 누락으로 인한 배포 실패 가능성을 낮춤

### 스테이징 SHA 고정 배포(19차)
- `docker-compose.prod.yml` 이미지 태그 전략 변경
  - API/Admin 이미지를 `:latest` 고정 대신 `${IMAGE_TAG:-latest}`로 사용
- `deploy-staging.yml` 배포 환경 변수 보강
  - deploy 시 `IMAGE_TAG=${{ github.sha }}`를 EC2 deploy 스크립트로 전달
- `deploy-staging.yml` 검증 보강
  - 배포 후 `docker inspect`로 `eunhye-api`, `eunhye-nginx`의 실제 이미지 태그가 해당 SHA인지 확인
- `infra/aws/deploy.sh` 출력/동작 정합성
  - `IMAGE_TAG`를 기본값 `latest`로 지원하고, 배포 로그에 현재 태그를 명시
- 효과
  - 동시 push 상황에서도 스테이징이 항상 해당 워크플로우 SHA 이미지로 배포되어 재현성과 추적성이 향상

### 스테이징 컨테이너 상태 검증 추가(18차)
- `deploy-staging.yml`의 `Verify deployment` 단계 보강
  - EC2 원격에서 `docker inspect`로 `eunhye-api` 상태(`running healthy`) 검증 추가
  - `eunhye-nginx` 상태(`running`) 검증 추가
  - 재시도 소진 시 명시적으로 배포 실패 처리
- 효과
  - 외부 HTTP 체크뿐 아니라 실제 컨테이너 런타임 상태까지 포함한 배포 완료 판정 가능

### 스테이징 배포 검증 강화(17차)
- `deploy-staging.yml`의 `Verify deployment` 단계 강화
  - `/api/v1/ping` 응답 본문에서 `"ok": true`를 retry 기반으로 검증
  - Admin 루트(`/`)의 HTTP 상태(200/301/302) 검증 추가
  - 인증 보호 API(`/api/v1/admin/hymns`)가 401/403을 반환하는지 검증 추가
- 효과
  - 배포 완료 판정 시 헬스체크뿐 아니라 Admin 라우팅/인증 가드까지 자동 확인 가능

### CI workflow lint 추가(16차)
- 신규 워크플로: `.github/workflows/workflow-lint.yml`
  - `rhysd/actionlint`로 GitHub Actions workflow 정적 검증
  - `bash -n infra/aws/deploy.sh`로 배포 스크립트 문법 검증
- lint 기준 반영
  - `deploy-staging.yml`의 Docker build/push 명령에서 이미지 태그 변수를 quote 처리(SC2086 대응)
- 트리거 범위
  - `pull_request`/`push(develop)`에서 `.github/workflows/**`, `infra/aws/deploy.sh` 변경 시 실행
- 효과
  - workflow/배포 스크립트 변경이 API/Admin 경로 필터에 가려 검증 누락되는 리스크를 감소

### 스테이징 deploy 직렬화 가드(15차)
- `deploy-staging.yml` 개선
  - `deploy` job에 GitHub Actions concurrency group(`staging-ec2-deploy`) 추가
  - `cancel-in-progress: false`로 설정해 동시 배포를 취소 대신 직렬 대기
- 효과
  - 동일 EC2 대상 deploy job이 겹쳐 실행되지 않아 compose/prune 경합 가능성을 사전에 차단

### 스테이징 deploy prune 경합 하드닝(14차)
- `infra/aws/deploy.sh` 개선
  - `docker image prune -f`를 `cleanup_unused_images` 함수로 감쌈
  - `prune operation is already running` 오류 시 재시도(최대 3회) 후 경고 처리
  - prune 실패를 배포 실패로 전파하지 않도록 non-fatal 처리
- 효과
  - 배포 완료 후 이미지 정리 단계의 일시적 경합으로 전체 deploy job이 실패하지 않음

### 로컬 아티팩트 ignore 정리(13차)
- 작업 트리 노이즈 제거
  - 루트 `.gitignore`에 `.tmp/` 추가
  - `infra/aws/.gitignore`에 `tfplan*` 추가
- 효과
  - 로컬 임시파일/terraform plan 파일이 기본 `git status` 결과를 오염시키지 않음

### 기준 범위 문서 최신화(12차)
- `docs/current-usable-scope.md` 기준점 정합성 업데이트
  - 기준 커밋을 최신 `develop` HEAD(`5b08dc0`)로 갱신
  - 근거 PR 목록에 #56/#55/#54/#53 반영
  - 최근 PR 변경 포인트 섹션을 최신 기능(보안/내구성/운영지표) 중심으로 보강

### CSV export 보안 하드닝(11차)
- CSV 수식 주입 방어 추가
  - 동기/비동기 CSV 내보내기 모두에서 수식 시작 문자열(`=`, `+`, `-`, `@`)을 이스케이프
  - 공통 유틸 `CsvUtils.toSafeCsvCell(...)`로 통합 적용
- 테스트 보강
  - `CsvUtilsTest` 신규
  - `AdminEventApiTest`에 동기/비동기 CSV 수식 주입 방어 통합 테스트 추가
- 문서 동기화
  - `docs/events.md` CSV 보안 정책 명시

### 비동기 export 안정화/운영 사이클(10차)
- 내구성 복구 경로 추가
  - `EventExportJobRepository.claimQueued(...)` 도입으로 작업 실행 claim을 원자화
  - `EventExportJobRecoveryScheduler` 추가: stale `RUNNING` 재큐잉 + `QUEUED` 재디스패치
  - 큐 포화 시 요청 실패 대신 `QUEUED` 유지(후속 복구 사이클에서 재처리)
- 결과 일관성 강화
  - `POST /admin/events/export-jobs`에서 `to` 미지정 시 생성 시각 snapshot 고정
  - 이벤트 조회 정렬을 `createdAt DESC, id DESC`로 안정화
- 성능 보강
  - `V10__event_export_jobs_cleanup_index.sql` 추가
  - 정리 쿼리 최적화 인덱스: `idx_event_export_jobs_status_completed_at`
- 운영 알림 베이스라인 추가
  - `EventExportJobOpsAlertScheduler` 추가
  - 실패율/p95/queued 임계치 초과 시 WARN 로그 출력
- 테스트 확장
  - `AdminEventExportJobUseCaseTest` 신규(스냅샷/claim-fail/배치 제한)
  - `EventExportJobRecoverySchedulerTest` 신규(재디스패치/queue rejection)
  - `EventExportJobOpsAlertSchedulerTest` 신규(설정 기반 평가 호출)
  - `AdminEventControllerTest` 신규(queue rejection 시 202 유지)
  - `AdminEventApiTest` 보강(미완료 다운로드 `409`, snapshot 결과, 안정 정렬)

### 비동기 export 운영 지표 정례화(9차)
- 백엔드 운영 지표 API 추가
  - `GET /api/v1/admin/events/export-jobs/metrics`
  - 기간(`days`, 1~90일) 기준으로 실패율/처리시간/정리량 집계 반환
- 정리량 집계 이력 저장 추가
  - `V9__event_export_job_cleanup_runs.sql`
  - 스케줄러 실행마다 `event_export_job_cleanup_runs`에 삭제량/보관일수 기록
- 관리자 UI 반영
  - `apps/admin/src/pages/AdminEventPage.tsx`에 운영 지표 카드(작업량/실패율/평균·p95 처리시간/정리량) 추가
  - `apps/admin/src/api/adminEvents.ts`에 운영 지표 API 클라이언트 추가
- 테스트 보강
  - `GetEventExportOpsMetricsUseCaseTest` 신규 추가
  - `AdminEventApiTest`에 운영 지표 엔드포인트 통합 테스트 추가
- 문서 동기화
  - `docs/events.md`, `docs/api-contract.md`, `docs/current-usable-scope.md`, `docs/WORK_CYCLE.md`, `CLAUDE.md`

### 비동기 export 결과 정리 배치(8차)
- 백엔드 정리 정책 추가
  - `CleanupEventExportJobsUseCase` 추가 (완료/실패 작업 보관 기한 기준 삭제)
  - `EventExportJobCleanupScheduler` 추가 (기본 매일 03:15 UTC 실행)
  - 저장소 삭제 메서드 추가: 완료/실패 + `completed_at` 기준 일괄 삭제
- 설정 추가
  - `EVENT_EXPORT_JOB_RETENTION_DAYS` (기본 7)
  - `EVENT_EXPORT_JOB_CLEANUP_CRON` (기본 `0 15 3 * * *`)
  - `EVENT_EXPORT_JOB_CLEANUP_ZONE` (기본 `UTC`)
- 테스트 보강
  - `CleanupEventExportJobsUseCaseTest` (정리 기준 계산/정규화 단위 테스트)
  - `CleanupEventExportJobsUseCaseIntegrationTest` (완료/실패만 삭제되는 통합 테스트)
  - 테스트 격리를 위한 cleanup integration test `tearDown` 추가
- 문서 동기화
  - `docs/events.md`, `docs/current-usable-scope.md`, `docs/WORK_CYCLE.md`, `CLAUDE.md`

### 감사 로그 대용량 비동기 export(7차)
- 백엔드 비동기 export job 추가
  - `POST /api/v1/admin/events/export-jobs`: 작업 생성(202 Accepted)
  - `GET /api/v1/admin/events/export-jobs/{jobId}`: 상태 조회
  - `GET /api/v1/admin/events/export-jobs/{jobId}/download`: 완료 작업 다운로드
  - 신규 테이블: `event_export_jobs` (`V8__event_export_jobs.sql`)
  - 신규 도메인/유스케이스: `EventExportJob`, `EventExportJobStatus`, `AdminEventExportJobUseCase`
- 관리자 UI 반영
  - `apps/admin/src/pages/AdminEventPage.tsx`
  - 비동기 CSV 요청 버튼, 작업 상태 카드, 완료 후 다운로드 버튼 추가
  - 최대 50,000건 요청 프리셋(백엔드 상한 100,000) 적용
- API 테스트 보강
  - `AdminEventApiTest`에 비동기 export 생성/완료/다운로드 및 요청자 격리 검증 추가
- 문서 동기화
  - `docs/events.md`, `docs/api-contract.md`, `docs/usecases/admin-list-events.md`, `docs/current-usable-scope.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`

### 감사 로그 집계 기간 커스텀(6차)
- 관리자 이벤트 화면 개선
  - `apps/admin/src/pages/AdminEventPage.tsx`
  - 집계 기간 입력을 고정 select(1/7/30)에서 숫자 입력(1~90일) + 프리셋(1/7/30/60/90)으로 확장
  - `from/to` 필터 사용 시 집계 제목을 "지정 기간 이벤트 집계"로 표시
- API 검증 강화
  - `apps/api/src/test/java/com/eunhyehymn/presentation/controllers/AdminEventApiTest.java`
  - `summaryDays=45` 같은 비프리셋 값이 실제 집계에 반영되는 통합 테스트 추가
- 문서 동기화
  - `docs/events.md`, `docs/current-usable-scope.md`, `CLAUDE.md`
  - 백로그 항목에서 "집계 기간 커스텀" 완료 반영

### 스테이징 준비도 동기화 + preflight 하드닝(5차)
- `scripts/staging-preflight.ps1` 개선
  - `aws sts get-caller-identity` 실패 시 즉시 예외 종료 대신 원인(자격증명/프로필/응답) 표준 출력
  - 자격증명 미설정 환경에서도 preflight 결과를 표 형태로 확인 가능
- 상태 문서 최신화
  - `docs/current-usable-scope.md`: 기준 커밋/근거 PR/실리허설 결과/남은 과제 동기화
  - `docs/deployment-readiness-audit.md`: 2026-02-14 리허설/롤백/복구 실행 결과 반영

### 스테이징 실가동 리허설 실행(4차)
- 요청된 사이클 1~3 실제 수행
  - 1) `develop` 리허설 배포 성공: run `22010284332`
  - 2) 롤백 리허설 성공: 임시 브랜치(`tmp/staging-rollback-6fef282`) 기준 run `22010387328`
  - 3) 최신 `develop` 재배포(복구) 성공: run `22010470389`
- 실행 결과 문서 반영
  - `docs/staging-rehearsal-log.md` 실행 로그 3건 추가
  - `docs/staging-smoke-checklist.md` 실행 기록 섹션(2026-02-14) 추가
- 스크립트/문서 보강
  - `scripts/staging-rehearsal.ps1`: commit SHA ref 입력 시 즉시 가이드 에러 처리 (`workflow_dispatch` branch/tag 제약 명시)
  - `docs/runbook.md`, `infra/aws/README.md`: SHA 기반 롤백 리허설 시 임시 브랜치 사용 절차 추가

### 스테이징 리허설 자동화(3차)
- 리허설 실행 스크립트 추가
  - `scripts/staging-rehearsal.ps1`
  - preflight 실행 → `deploy-staging.yml` `workflow_dispatch` 트리거 → run 완료 대기 → 결과 로그 기록 자동화
- 리허설 이력 문서 추가
  - `docs/staging-rehearsal-log.md`
- 운영 문서 동기화
  - `docs/runbook.md`, `docs/staging-smoke-checklist.md`, `infra/aws/README.md`
  - 리허설 자동 실행 경로/증빙 문서 반영
  - `CLAUDE.md`, `docs/WORK_CYCLE.md` 업데이트

### 스테이징 배포 안정화(2차)
- `deploy-staging.yml` 개선
  - `workflow_dispatch` 트리거 추가 (`enable_awslogs` 입력)
  - `preflight-secrets` job을 선행해 필수 Secrets 누락 시 조기 실패
- `deploy.sh` 안정화
  - awslogs 드라이버 미지원 시 자동 fallback
  - awslogs 모드 재기동 실패 시 기본 logging(`json-file`)으로 자동 재시도
- 운영 문서 보강
  - `infra/aws/README.md`에 수동 검증 실행(workflow_dispatch) 루트 추가
  - `docs/runbook.md` 배포 절차에 운영 반영/리허설 실행 경로 분리 명시
  - `CLAUDE.md` CI/CD 섹션 최신화

### 스테이징 실가동 준비 문서 사이클
- 스테이징 스모크 테스트 기준 문서 추가
  - `docs/staging-smoke-checklist.md`
- 운영 런북에 배포 후 스모크/롤백 절차를 구체화
  - `docs/runbook.md`
- 기준 범위 문서 최신화
  - `docs/current-usable-scope.md` 기준 커밋/우선 과제/선행 체크리스트 갱신
- 개발 가이드 충돌 해소
  - `docs/dev-guide.md`에서 Gradle 실행 기준을 wrapper(`./gradlew`)로 통일
- 데이터 모델 문서 보완
  - `docs/data-model.md` events 인덱스(`V7__events_admin_indexes.sql`) 반영
- 상위 문서 동기화
  - `README.md` (스모크 체크리스트 링크 추가)
  - `docs/mobile/README.md` (운영 연계 체크포인트 반영)
  - `docs/WORK_CYCLE.md`, `CLAUDE.md` 동기화

### 문서 검증
- `rg -n "Placeholder|PDF/AUDIO|PDF/MP3|소셜 토큰 입력 방식|미완료: 모바일 고도화|시스템 Gradle" docs README.md CLAUDE.md`
- `rg -n "<<<<<<<|>>>>>>>" docs README.md CLAUDE.md`

## 2026-02-13

### 스테이징 실가동 전환 준비(진행중)
- 스테이징 사전 점검 스크립트 추가
  - `scripts/staging-preflight.ps1`
- Terraform 출력 기반 시크릿 동기화 스크립트 추가
  - `scripts/staging-sync-secrets.ps1`
- Terraform 배포 IAM 정책 샘플 추가
  - `infra/aws/terraform-deployer-iam-policy.json`
- 운영 체크
  - 시크릿/권한 최신 상태는 `scripts/staging-preflight.ps1` 실행 결과를 기준으로 관리

### 시크릿 관리 가이드 정리
- `.env.example` 템플릿(루트/API/AWS) 주석과 기본값을 정리해 오해 가능성을 줄임
- 시크릿 관리 기준 문서 추가
  - `docs/SECRETS_MANAGEMENT.md`
- 사이클 기준서에 시크릿 노출 점검 항목 추가
  - `docs/WORK_CYCLE.md`
- `.gitignore`에 민감 파일 패턴 추가
  - `.envrc`, `.secrets/`, `*.pem`, `*.key`, `*.p12`, `*.pfx`

### 문서/배포 준비도 검수
- 배포 준비도 점검 리포트 추가
  - `docs/deployment-readiness-audit.md`
- 기준 문서 최신화
  - `README.md`
  - `docs/current-usable-scope.md`
  - `docs/mobile/README.md`
  - `CLAUDE.md`

### PR 본문 포맷 가드레일 추가
- PR 본문 작성 시 `--body-file` 우선 사용 규칙을 문서화
- 반영 후 `gh pr view`로 줄바꿈/포맷 렌더링을 확인하는 검증 절차 추가
- 반영 문서:
  - `docs/WORK_CYCLE.md`
  - `CLAUDE.md`

### Admin 토큰 저장 하드닝(완료)
- Admin 웹 토큰 저장소를 `localStorage`에서 `sessionStorage` 기반으로 전환
- 구버전 `localStorage` 토큰은 최초 로드시 `sessionStorage`로 마이그레이션 후 삭제
- 적용 파일:
  - `apps/admin/src/auth/tokenStore.ts`
  - `apps/admin/src/auth/AuthContext.tsx`
  - `apps/admin/src/api/client.ts`
  - `apps/admin/src/api/adminEvents.ts`

### 이벤트 조회 성능 인덱스(완료)
- 관리자 이벤트 조회/CSV 패턴 최적화 인덱스 추가
  - `idx_events_created_at_desc`
  - `idx_events_event_type_created`
  - `idx_events_hymn_created`
- 마이그레이션 추가
  - `apps/api/src/main/resources/db/migration/V7__events_admin_indexes.sql`
- 운영 문서 업데이트
  - `docs/events.md`에 조회 파라미터, 인덱스, 점검 항목 반영

### 모바일 기능 3건 머지
- PR #36 `feat/mobile-social-sdk`
  - Kakao 소셜 SDK 로그인 추가
  - KakaoTalk 실패 시 Kakao 계정 로그인 fallback 추가
- PR #37 `feat/mobile-midi-player-ux`
  - 상세 화면 MIDI 재생/일시정지/정지/속도 UI 추가
  - paused 상태에서 `play()` 재시작 대신 `resume()`으로 동작 수정
- PR #38 `feat/mobile-offline-cache-sync`
  - 오프라인 캐시/동기화 큐 추가
  - 캐시 및 큐를 사용자 세션(`userId`) 기준으로 분리

### 문서 정합성 정리
- `README.md`, `CLAUDE.md`, `docs/mobile/README.md` 최신 상태 반영
- 작업 사이클 기본 규칙 문서화
  - `docs/WORK_CYCLE.md`
  - 작업 요청 시 기본 동작: 구현 -> 검증 -> 커밋 -> PR

### 관리자 감사 로그 기능(완료)
- API
  - `GET /api/v1/admin/events`
  - `GET /api/v1/admin/events/export`
- Admin UI
  - `/events` 페이지에서 필터/집계/페이지네이션/CSV 내보내기 제공
- 테스트
  - `AdminEventApiTest` 추가

## 검증 로그

- API
  - `./gradlew.bat test --no-daemon --stacktrace`
- Admin
  - `npm run build` (`apps/admin`)
- Mobile
  - `..\..\scripts\flutterw.ps1 analyze` (`apps/mobile`)
  - `..\..\scripts\flutterw.ps1 test --reporter expanded` (`apps/mobile`)
- Terraform
  - `terraform -chdir=infra/aws init -backend=false -input=false`
  - `terraform -chdir=infra/aws validate`

## 운영 참고

- 로컬 API 실행 보조 스크립트
  - `scripts/start-wsl-postgres.ps1`
  - `scripts/run-api-local-wsl-db.ps1`
