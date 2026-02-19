# 현재 사용 가능 범위 정리

- 작성일: 2026-02-17
- 기준 브랜치: `develop` (통합/배포 기준)
- 기준 커밋: `ee49a0e` (2026-02-17 22:57:13 UTC 기준 `develop` HEAD)
- 근거 문서: `README.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`
- 근거 PR: #92, #91, #90, #89, #88, #87, #56, #55, #54, #53, #52, #49, #48, #47, #46, #45, #43, #42, #41, #40, #38, #37, #36, #31

## 1. 요약

현재 시점 기준으로 프로젝트 상태는 다음과 같다.

- 로컬 기능: 관리자 웹(Admin) + 백엔드 API + 모바일 앱(MVP + 모바일 고도화 3건) 사용 가능
- 배포 기능: AWS 스테이징 자동 배포 파이프라인 코드 구성 완료
- CI 기능: API/Admin/Mobile 검증 + Mobile release APK 검증 + Mobile store readiness(수동) 워크플로우 구성 완료
- 스테이징 리허설/롤백/복구 자동 실행 증빙 확보(run `22010284332`, `22010387328`, `22010470389`)
- 현재 우선 과제: 운영 PC 기준 preflight 무스킵 통과 환경 유지 + Admin/Mobile 수동 스모크 정례화
- 모바일 배포 상태: Android signed AAB/iOS no-codesign 수동 readiness 워크플로우 추가, 스토어 업로드 파이프라인은 추가 준비 필요

## 2. 지금 바로 검증 가능한 범위 (로컬)

### 2.1 관리자 웹(Admin)

다음 기능을 UI에서 바로 검증할 수 있다.

- 찬양 관리: 목록/생성/수정/삭제, 검색/필터, 활성화 토글
- 에셋 관리: Presign -> 업로드 -> Confirm 3단계, 에셋 삭제
- 사용자 관리: 생성/조회/수정/삭제(soft-delete), 운영자 잠금 정책(현재 운영자/마지막 활성 관리자 보호)
- 초대코드 관리: 생성/비활성화/만료일 설정 및 표시
- 감사 로그/분석: 이벤트 로그 필터/페이지네이션 조회, 이벤트 타입별 집계(최근 N일, `summaryDays` 1~90 커스텀), 동기 CSV 내보내기 + 비동기 대용량 CSV 작업(요청/상태/다운로드) + 완료/실패 작업 자동 정리
- 인증: Admin ID/PW 로그인(운영), Kakao 소셜 로그인(사용자), Dev 로그인(로컬)
- 토큰: 401 발생 시 Access Token 자동 갱신 후 재시도

관련 파일:

- `apps/admin/src/App.tsx`
- `apps/admin/src/pages/HymnListPage.tsx`
- `apps/admin/src/pages/HymnEditPage.tsx`
- `apps/admin/src/pages/InviteCodePage.tsx`
- `apps/admin/src/pages/AdminEventPage.tsx`
- `apps/admin/src/pages/LoginPage.tsx`
- `apps/admin/src/api/client.ts`
- `apps/admin/src/api/adminEvents.ts`

### 2.2 백엔드 API

다음 도메인 기능이 동작 범위에 포함된다.

- 인증/인가: JWT, Refresh Token 회전, 소셜 로그인(Kakao), Dev 로그인
- 찬양: 공개 조회 + 관리자 CRUD + 삭제(cascade)
- 에셋: 관리자 Presign/Confirm/Delete (AssetType: `PNG`, `MIDI`)
- 사용자/초대코드 관리자 기능(사용자 CRUD + 초대코드 생성/비활성화)
- 관리자 감사 로그: `GET /admin/events` 조회/필터/페이지네이션 + 최근 N일 이벤트 타입 집계(`summaryDays` 1~90)
- 관리자 감사 로그 내보내기: `GET /admin/events/export`(동기 CSV), `POST /admin/events/export-jobs` + `GET /admin/events/export-jobs/{jobId}` + `GET /admin/events/export-jobs/{jobId}/download`(비동기 대용량 CSV), 스케줄러 기반 결과 정리(기본 7일 보관)
- 멤버 기능: 즐겨찾기, 메모, 히스토리, 이벤트 기록

엔드포인트 기준은 `CLAUDE.md` 7장과 현재 컨트롤러/유즈케이스 구현 상태를 따른다.

### 2.3 모바일 앱 (Flutter MVP + 고도화)

다음 기능을 앱에서 바로 검증할 수 있다.

- 로그인: 소셜 SDK 직접 로그인(Kakao 모바일), Kakao 웹/미지원 플랫폼 토큰 입력 fallback, Dev 로그인
- 찬양: 목록 조회/검색, 상세 조회, PNG 에셋 표시, MIDI 앱 내 재생(재생/일시정지/정지/속도)
- 개인화: 즐겨찾기 토글, 메모 조회/저장, 최근 열람 히스토리
- 오프라인: 목록/상세/메모/즐겨찾기/히스토리 캐시 fallback + 오프라인 변경 동기화 큐
- 인증: 토큰 저장, 401 시 자동 refresh 후 재시도

관련 파일:

- `apps/mobile/lib/src/app.dart`
- `apps/mobile/lib/src/features/auth/login_page.dart`
- `apps/mobile/lib/src/features/auth/social_sdk_service.dart`
- `apps/mobile/lib/src/features/hymn/hymn_list_page.dart`
- `apps/mobile/lib/src/features/hymn/hymn_detail_page.dart`
- `apps/mobile/lib/src/features/history/history_page.dart`
- `apps/mobile/lib/src/core/network/api_client.dart`

## 3. 조건부로 검증 가능한 범위 (스테이징)

AWS 스테이징 인프라 및 CI/CD 자동 배포는 코드 기준으로 구성 완료 상태다.

- `develop` push 트리거
- API 테스트 + Admin 타입체크/빌드 + Mobile analyze/test
- Mobile release APK 빌드 검증(`mobile-release-check.yml`)
- Mobile store release readiness 수동 검증(`mobile-store-release.yml`)
- API/Admin Docker 이미지 ECR push
- EC2 SSH 배포 및 헬스체크

관련 파일:

- `.github/workflows/deploy-staging.yml`
- `.github/workflows/mobile-ci.yml`
- `.github/workflows/mobile-release-check.yml`
- `.github/workflows/mobile-store-release.yml`
- `infra/aws/*`
- `apps/admin/Dockerfile`
- `apps/admin/nginx.conf`
- `apps/api/Dockerfile`

실행 상태(2026-02-17 기준):

- [x] AWS 리소스 생성(Terraform)
- [x] GitHub Secrets 설정(`AWS_*`, `ECR_REGISTRY`, `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`, 선택: `ENABLE_AWSLOGS`)
- [x] EC2 접근 가능 상태 및 배포 계정 권한 확인
- [x] 배포 서버 `.env` 준비 (`DEPLOY_ENV_FILE` 기반)
- [x] `develop` 배포 후 헬스체크 통과 (`deploy` verify 단계 성공)
- [x] `develop` 최신 자동 배포 성공 (`deploy-staging.yml`, run `22119056042`, commit `ee49a0e`)
- [x] Android release APK 빌드 검증 성공 (`mobile-release-check.yml`, run `22052482140`, commit `35569af`)
- [x] 운영 사이클 자동 점검 스크립트 추가 및 실행 (`scripts/staging-ops-cycle.ps1`, `docs/staging-smoke-log.md`)
- [ ] preflight 무스킵 통과 회복 필요 (`aws sts get-caller-identity`: session expired)
- [x] `docs/staging-smoke-checklist.md` 및 `docs/staging-rehearsal-log.md`에 결과 기록
- [ ] Admin/Mobile 런타임 수동 스모크(실기기/실계정) 주기 실행

실행 상태 확인:
- 사전 점검: `scripts/staging-preflight.ps1`
- 운영 사이클 점검: `scripts/staging-ops-cycle.ps1`
- 시크릿 동기화: `scripts/staging-sync-secrets.ps1`
- IAM 권한 샘플: `infra/aws/terraform-deployer-iam-policy.json`

## 4. 최근 PR 기준 변경 포인트

### PR #91 (changelog-sync-mobile-20260216)
- PR #89/#90 머지 결과를 `docs/changelog-dev.md`에 반영
- `docs/WORK_CYCLE.md`, `CLAUDE.md` 문서 싱크

### PR #90 (mobile-release-readiness)
- Android release APK 빌드 검증 워크플로우 추가
- `workflow_dispatch` 입력(`api_base_url`) + artifact 업로드 경로 정리

### PR #89 (mobile-friendly-ui-ux)
- 모바일 목록/히스토리 화면 UX 개선(검색 즉시삭제, 필터, 빈 상태, soft error 배너)
- 히스토리 기간 필터 처리 보정(날짜 없는 항목 제외)

### PR #88 (staging-feedback-checklist)
- 스테이징 수동 검수용 체크리스트 문서 추가
- 사이클 종료 시 `Ship/Hold/Rollback` 판단 기록 템플릿 정리

### PR #87 (mobile-ux-multipage-png)
- 다중 페이지 PNG 악보 지원
- 모바일 앱 탐색/표시 UX 보강

### PR #56 (export-csv-security-hardening)
- 동기/비동기 CSV 내보내기 수식 주입 방어(`=`, `+`, `-`, `@`) 적용
- `CsvUtils` 공통 유틸 도입 및 통합 테스트 보강

### PR #55 (event-export-reliability-cycle)
- 비동기 export 내구성 강화
  - 원자적 claim(`QUEUED` -> `RUNNING`) 도입
  - stale `RUNNING` 복구 + `QUEUED` 재디스패치 스케줄러 추가
- export 정합성/성능/운영가드 보강
  - `to` 미지정 시 snapshot 상한 고정
  - 정렬 안정화(`createdAt DESC, id DESC`)
  - 정리 쿼리 인덱스(`V10`) 추가
  - 임계치 기반 운영 경고 스케줄러 추가

### PR #54 (event-export-ops-metrics)
- 비동기 export 운영 지표 API/관리자 UI 반영
- 정리 실행 이력 저장 테이블(`V9`) 및 지표 집계 파이프라인 추가

### PR #53 (event-export-job-retention)
- 비동기 export 결과 정리 배치 추가
- `CleanupEventExportJobsUseCase` + `EventExportJobCleanupScheduler` 도입
- 보관 정책 설정값(`EVENT_EXPORT_JOB_RETENTION_DAYS`, `EVENT_EXPORT_JOB_CLEANUP_*`) 추가

### PR #52 (admin-events-async-export)
- 관리자 감사 로그 비동기 대용량 CSV export 작업 API 추가
- `event_export_jobs` 테이블(`V8`) 및 작업 상태 조회/다운로드 엔드포인트 추가
- Admin 이벤트 화면에 비동기 CSV 요청/상태/다운로드 UX 반영

### PR #38 (mobile-offline-cache-sync)
- 목록/상세/메모/즐겨찾기/히스토리 캐시 fallback, 오프라인 동기화 큐
- 리뷰 반영: 캐시/큐 세션 스코프(`userId`) 분리

### PR #37 (mobile-midi-player-ux)
- 상세 화면 MIDI 재생/일시정지/정지/속도 UI
- 리뷰 반영: paused 상태에서 `resume()` 사용

### PR #36 (mobile-social-sdk)
- Kakao SDK 직접 로그인 흐름 추가
- 리뷰 반영: KakaoTalk 실패 시 `loginWithKakaoAccount()` fallback

### PR #31 (aws-staging)
- AWS Free Tier 스테이징 인프라(Terraform) 추가
- Staging 자동 배포 워크플로우 추가
- Admin Dockerfile + Nginx 설정 추가
- API Dockerfile 보완(헬스체크 관련)

## 5. 미완료 범위 (우선순위)

- 스테이징 실가동 전환
  - 운영 PC 기준 `staging-preflight.ps1` 무스킵 통과 상태 유지 (`aws` 자격증명 + `infra/aws/terraform.tfvars`)
  - 배포/롤백/장애 대응 리허설의 정기 반복 및 증빙 누적
- 운영 문서/절차 실행 검증
  - `docs/runbook.md` + `docs/staging-smoke-checklist.md` 기준 Admin/Mobile 수동 스모크 실행
  - 리허설/스모크 결과를 `docs/changelog-dev.md`에 주기 반영
- 운영 기능 백로그
  - 비동기 export 운영 모니터링 지표(실패율/처리시간/정리량) 정례화 완료
  - 후속 과제: 지표 임계치 기반 알림/대시보드 연동 설계

## 6. 빠른 사용 체크리스트

### 로컬 실행

1. API 실행
   - `cd apps/api`
   - `./gradlew bootRun` (Windows는 `gradlew.bat bootRun`)
2. Admin 실행
   - `cd apps/admin`
   - `npm install`
   - `npm run dev`
3. Mobile 실행
   - `.\scripts\flutterw.ps1 --version`
   - `cd apps/mobile`
   - `..\..\scripts\flutterw.ps1 pub get`
   - `..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1`
4. 접속
   - Admin: `http://localhost:5173`
   - API Base: `http://localhost:8080/api/v1`

### 주의

- `JWT_SECRET`, `JWT_ACCESS_TTL_SECONDS`, `JWT_REFRESH_TTL_SECONDS`, `INVITE_CODE` 미설정 시 API 부팅 실패
- 소셜 로그인 실사용 검증 시 provider 토큰/클라이언트 설정 필요
- 모바일은 현재 문서/실행 가이드 기준으로 `flutter run -d chrome` 경로를 우선 지원

## 7. 검증 로그

### 7.1 2026-02-13 실검증 로그 (명령 기반)

- API 테스트: `./gradlew.bat test --no-daemon --stacktrace` 성공 (58 tests, failed 0, skipped 0)
- Admin 빌드: `npm run build` 성공
- Mobile 검증:
  - `..\..\scripts\flutterw.ps1 analyze` 성공 (`No issues found`)
  - `..\..\scripts\flutterw.ps1 test --reporter expanded` 성공
- Terraform: `terraform -chdir=infra/aws validate` 성공
- Compose:
  - 로컬(`infra/docker/docker-compose.yml`) config 파싱 성공
  - 스테이징(`infra/aws/docker-compose.prod.yml`)은 `.env` 파일 준비 전 실행 불가

상세 결과와 Go/No-Go 판단은 `docs/deployment-readiness-audit.md`를 기준으로 한다.

### 7.2 2026-02-14 문서 동기화
- 스테이징 스모크 체크리스트 문서 추가: `docs/staging-smoke-checklist.md`
- 운영 런북과 스모크 테스트 절차 동기화: `docs/runbook.md`

### 7.3 2026-02-14 실리허설/롤백 검증 로그
- `scripts/staging-rehearsal.ps1 -Ref develop -SkipPreflight`
  - run: `22010284332` (success)
- rollback rehearsal (`tmp/staging-rollback-6fef282`)
  - run: `22010387328` (success)
- develop restore rehearsal
  - run: `22010470389` (success)

