# 개발 변경 이력

작업 단위별 핵심 변경만 기록한다. 상세 구현은 각 PR 본문과 커밋 로그를 참고한다.

## 2026-02-14

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
  - Google/Kakao 소셜 SDK 로그인 추가
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
