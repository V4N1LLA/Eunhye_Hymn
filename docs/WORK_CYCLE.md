# 작업 사이클 기준서

반복 설명 없이 동일한 방식으로 작업을 이어가기 위한 기준 문서다.

## 0. 기본 동작 선언

- 사용자가 입력한 내용이 **작업 요청**으로 해석되면, 에이전트는 별도 확인 없이 기본적으로 작업 사이클을 시작한다.
- 기본 완료 기준은 **코드/문서 반영 + 검증 + 커밋 + PR 생성**이다.
- 사용자가 명시적으로 범위를 제한하거나 PR 생성을 제외하라고 지시한 경우에만 예외를 적용한다.

## 1. 표준 작업 사이클

1. 요구사항 확정
- 목표/범위/완료 기준을 1문장으로 고정한다.

2. 브랜치 전략
- `develop` 기준으로 기능별 `feat/*` 브랜치를 만든다.
- 서로 충돌 가능한 작업은 별도 브랜치(필요 시 worktree)로 분리한다.

3. 구현
- 최소 변경으로 목표 기능을 먼저 동작시킨다.
- 기존 규칙(Clean Architecture, API base `/api/v1`)을 유지한다.

4. 자체 검수
- 리뷰 코멘트/리스크를 우선 반영한다.
- 필요 시 리팩토링 후 재검증한다.

5. 검증
- 가능한 범위에서 `analyze/test` 또는 빌드 검증을 실행한다.
- 실패 시 원인과 재시도 결과를 기록한다.

6. 문서 동기화
- 기능/운영 방식이 바뀌면 `README.md`, `docs/*`, `CLAUDE.md`를 같이 갱신한다.
- 문서 충돌 잔여 마커(`<<<<<<<`, `>>>>>>>`) 유무를 확인한다.
- 시크릿 노출 여부(`.env`, key/token/password 하드코딩) 점검 결과를 포함한다.

7. 커밋/푸시/PR
- 의미 단위로 커밋하고 원격 푸시한다.
- PR 본문에 변경 요약/검증 명령/리스크를 명시한다.
- PR 본문 작성 시 `gh pr create/edit --body` 인라인 문자열 사용은 지양한다. (PowerShell에서 `\n` 이스케이프가 문자열로 들어갈 수 있음)
- PR 본문은 UTF-8 `.md` 파일을 만든 뒤 `gh pr create/edit --body-file <file>`로 반영한다.
- 반영 직후 `gh pr view <번호>`로 실제 렌더링(줄바꿈/코드블록/경로 백슬래시)을 확인한다.
- 임시 본문 파일은 반영 확인 후 삭제한다.

8. 리뷰 반영
- 리뷰 코멘트를 반영하고 재검증 후 푸시한다.
- 각 리뷰 코멘트 thread에 처리 결과를 반드시 답글로 남긴다. (무엇을 변경했는지, 어떤 검증을 했는지)
- 코드 변경이 없는 코멘트도 답글로 사유를 남긴다. (질문 응답, 이미 반영됨, 보류/비적용 근거)
- 답글 작성 후 PR 코멘트 목록에서 누락 thread가 없는지 확인한다.

9. 머지 및 후속 정리
- PR 머지 후 `develop` 동기화.
- 문서 누락 체크 및 다음 작업 우선순위 제안.

## 2. 이번 사이클 기록 (2026-02-13)

### 목표
- 모바일 고도화 3건을 각각 독립 PR로 완료하고 리뷰 반영 후 머지.

### 작업 단위
1. 소셜 SDK 직접 로그인
- 브랜치: `feat/mobile-social-sdk`
- PR: #36
- 핵심: Kakao SDK 로그인 흐름 추가, KakaoTalk 실패 시 계정 로그인 fallback

2. MIDI 앱 내 플레이어 UX
- 브랜치: `feat/mobile-midi-player-ux`
- PR: #37
- 핵심: 상세 화면 MIDI 재생/일시정지/정지/속도, pause 후 resume 동작 수정

3. 오프라인 캐시/동기화
- 브랜치: `feat/mobile-offline-cache-sync`
- PR: #38
- 핵심: 목록/상세/메모/즐겨찾기/히스토리 캐시 fallback, pending queue 동기화
- 리뷰 반영: 캐시/큐를 사용자 세션(`userId`) 기준으로 분리

### 리뷰 반영 요약
- #36: KakaoTalk 로그인 실패 시 `loginWithKakaoAccount()` fallback 추가
- #37: paused 상태에서 `play()` 재시작 대신 `resume()` 사용
- #38: 오프라인 캐시/큐의 계정 간 데이터 오염 방지(세션 스코프 키)

### 병합 결과
- #36 merged: 2026-02-13 08:09:34Z
- #37 merged: 2026-02-13 08:14:46Z
- #38 merged: 2026-02-13 08:16:28Z

### 사이클 종료 후 문서 정리
- `README.md` 모바일 기능 최신화
- `docs/mobile/README.md` 최신화 및 포맷 깨짐 복구
- `CLAUDE.md` 진행 상태 최신화 및 포맷 깨짐 복구

## 3. 이번 사이클 기록 (2026-02-14)

### 목표
- 문서 정합성 동기화 + 스테이징 실가동 체크리스트 확정

### 범위
- 포함: 운영/배포/개발 문서 정합성 정리, 스모크 테스트 기준 확정
- 제외: 신규 기능 개발, AWS 리소스 실제 생성/변경

### 수행 작업
1. 스테이징 스모크 테스트 체크리스트 신설
- `docs/staging-smoke-checklist.md`

2. 운영 런북 강화
- 배포 후 스모크 테스트 수행 절차 반영
- 롤백 절차 구체화
- `docs/runbook.md`

3. 문서 충돌 해소
- `docs/dev-guide.md`: Gradle 정책을 wrapper 기준으로 통일
- `docs/data-model.md`: events 인덱스(`V7__events_admin_indexes.sql`) 반영
- `docs/current-usable-scope.md`: 기준 커밋/선행 체크리스트/우선 과제 갱신

4. 문서 동기화
- `README.md`: 스모크 체크리스트 링크 추가
- `docs/mobile/README.md`: 운영 연계 체크포인트 반영
- `docs/changelog-dev.md`, `CLAUDE.md` 동기화

### 검증
- `rg -n "Placeholder|PDF/AUDIO|PDF/MP3|소셜 토큰 입력 방식|미완료: 모바일 고도화|시스템 Gradle" docs README.md CLAUDE.md`
- `rg -n "<<<<<<<|>>>>>>>" docs README.md CLAUDE.md`

## 4. 다음 사이클 시작 템플릿 (작성 완료: 2026-02-14)

아래 4줄을 채워 바로 시작한다.

- 목표: 스테이징 실가동 전환 (Terraform apply + Secrets 설정 + 첫 자동 배포 + 스모크/롤백 리허설)
- 범위 제외: 신규 기능 개발
- 완료 기준: `docs/runbook.md` + `docs/staging-smoke-checklist.md` 기준으로 Go/No-Go 판단 근거 확보
- 검증 명령: `rg -n "No-Go|필수 체크리스트|스모크|롤백" docs/runbook.md docs/staging-smoke-checklist.md docs/current-usable-scope.md docs/deployment-readiness-audit.md`

## 5. 이번 사이클 기록 (2026-02-14, 2차)

### 목표
- 스테이징 배포 실패 원인(awslogs 옵션) 차단 + 브랜치 단위 배포 검증 경로 확보

### 범위
- 포함: deploy workflow/스크립트 안정화, 운영 문서 동기화
- 제외: AWS 리소스 실제 생성/변경

### 수행 작업
1. 배포 워크플로 개선
- `workflow_dispatch` 추가 (`enable_awslogs` 입력)
- 필수 시크릿 검증 `preflight-secrets` 선행

2. 배포 스크립트 안정화
- `infra/aws/deploy.sh`에 awslogs 드라이버 감지
- awslogs 실패 시 기본 로깅으로 자동 fallback 재시도

3. 운영 문서 동기화
- `infra/aws/README.md`: 수동 검증 실행 경로 추가
- `docs/runbook.md`: 운영 반영/리허설 경로 분리 명시
- `docs/changelog-dev.md`, `CLAUDE.md` 업데이트

### 검증
- `bash -n infra/aws/deploy.sh`
- `rg -n "<<<<<<<|>>>>>>>" .github/workflows/deploy-staging.yml infra/aws/deploy.sh infra/aws/README.md docs/runbook.md`

## 6. 이번 사이클 기록 (2026-02-14, 3차)

### 목표
- 스테이징 리허설 실행/기록 자동화로 Go/No-Go 근거 수집 시간을 단축

### 범위
- 포함: 리허설 자동 실행 스크립트 추가, 운영 문서/로그 템플릿 동기화
- 제외: AWS 리소스 실제 생성/변경

### 수행 작업
1. 리허설 자동화 스크립트 추가
- `scripts/staging-rehearsal.ps1`
- preflight 실행, `deploy-staging.yml` workflow_dispatch 트리거, run 완료 대기, 결과 로그 자동 기록

2. 리허설 로그 문서 추가
- `docs/staging-rehearsal-log.md`
- UTC 시간/브랜치/run URL/결과를 표 형식으로 누적 기록

3. 운영 문서 동기화
- `docs/runbook.md`: 자동 리허설 경로 및 로그 기록 위치 반영
- `docs/staging-smoke-checklist.md`: 실행 전 자동 리허설 로그 확인 항목 반영
- `infra/aws/README.md`: 스크립트 사용법 추가
- `docs/changelog-dev.md`, `CLAUDE.md` 업데이트

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-rehearsal.ps1 -DryRun -SkipPreflight` (의존성/파라미터 경로 확인용)
- `rg -n "staging-rehearsal|staging-rehearsal-log" docs/runbook.md docs/staging-smoke-checklist.md infra/aws/README.md CLAUDE.md`
- `rg -n "^(<<<<<<<|>>>>>>>|=======)$" scripts/staging-rehearsal.ps1 docs/staging-rehearsal-log.md docs/runbook.md docs/staging-smoke-checklist.md infra/aws/README.md docs/changelog-dev.md docs/WORK_CYCLE.md CLAUDE.md`

## 7. 이번 사이클 기록 (2026-02-14, 4차)

### 목표
- 사용자 요청 기준으로 사이클 1~3(리허설 실행 → 스모크 결과 기록 → 롤백 리허설) 완료

### 범위
- 포함: 실배포 리허설 실행, 롤백 리허설/복구 실행, 결과 문서화, 스크립트 UX 보강
- 제외: AWS 리소스 스펙 변경

### 수행 작업
1. 리허설 실행 (사이클 1)
- `scripts/staging-rehearsal.ps1 -Ref develop -SkipPreflight`
- run `22010284332` 성공

2. 스모크 결과 기록 (사이클 2)
- `docs/staging-rehearsal-log.md`에 실행 로그 누적
- `docs/staging-smoke-checklist.md`에 2026-02-14 실행 기록 추가

3. 롤백 리허설 + 복구 (사이클 3)
- `workflow_dispatch`가 SHA ref를 지원하지 않아 임시 브랜치 생성 후 실행
  - rollback run `22010387328` 성공
  - recover run `22010470389` 성공
- 임시 브랜치 정리
  - `git push origin --delete tmp/staging-rollback-6fef282`
  - `git branch -D tmp/staging-rollback-6fef282`

4. 개선 반영
- `scripts/staging-rehearsal.ps1`에 SHA ref 가드 추가(명확한 에러 메시지)
- `docs/runbook.md`, `infra/aws/README.md`에 SHA 리허설 절차 반영
- `docs/changelog-dev.md`, `CLAUDE.md` 동기화

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -SkipTerraformPlan` (로컬 `aws`/`terraform` 미설치로 실패 확인)
- `powershell -NoProfile -File .\\scripts\\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -SkipPreflight`
- `powershell -NoProfile -File .\\scripts\\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref tmp/staging-rollback-6fef282 -SkipPreflight`
- `gh run view 22010284332 --json conclusion,jobs,url`
- `gh run view 22010387328 --json conclusion,jobs,url`
- `gh run view 22010470389 --json conclusion,jobs,url`

## 8. 이번 사이클 기록 (2026-02-14, 5차)

### 목표
- 다음 우선순위 작업 수행: preflight 실행성 개선 + 준비도 문서 상태 최신화

### 범위
- 포함: preflight 스크립트 오류 처리 보강, 상태 문서 동기화
- 제외: 애플리케이션 기능 개발

### 수행 작업
1. 환경 보정
- 로컬 `aws`/`terraform` CLI 설치
- preflight 무스킵 실행 시도

2. preflight 하드닝
- `scripts/staging-preflight.ps1`에서 `aws sts get-caller-identity` 실패 처리 개선
- 자격증명/프로필 문제를 표 형태로 명시해 즉시 원인 파악 가능하게 수정

3. 문서 동기화
- `docs/current-usable-scope.md`: 기준 커밋/근거 PR/스테이징 실행 상태 최신화
- `docs/deployment-readiness-audit.md`: 2026-02-14 리허설/롤백/복구 결과 반영
- `docs/changelog-dev.md`, `CLAUDE.md` 업데이트

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn`
- `aws sts get-caller-identity` (현재 환경: credential 미설정 확인)
- `rg -n "22010284332|22010387328|22010470389|Conditional Go|preflight" docs/current-usable-scope.md docs/deployment-readiness-audit.md docs/changelog-dev.md CLAUDE.md`

## 9. 이번 사이클 기록 (2026-02-14, 6차)

### 목표
- 기능 백로그 일부 완료: 관리자 감사 로그 집계 기간 커스텀(1~90일)

### 범위
- 포함: Admin UI 입력 확장, API 통합 테스트 추가, 문서/백로그 동기화
- 제외: 대용량 비동기 export 구현

### 수행 작업
1. Admin UI 개선
- `apps/admin/src/pages/AdminEventPage.tsx`
- 집계 기간 입력을 숫자 입력(1~90)으로 전환하고 프리셋 버튼(1/7/30/60/90) 추가
- 날짜 필터 존재 시 집계 제목을 "지정 기간 이벤트 집계"로 표기

2. API 검증 보강
- `apps/api/src/test/java/com/eunhyehymn/presentation/controllers/AdminEventApiTest.java`
- `summaryDays=45` 집계 반영 통합 테스트 추가

3. 문서 동기화
- `docs/events.md`: `summaryDays` 커스텀 범위 명시
- `docs/current-usable-scope.md`, `CLAUDE.md`: 기능/백로그 상태 갱신
- `docs/changelog-dev.md` 업데이트

### 검증
- `./gradlew.bat test --tests "com.eunhyehymn.presentation.controllers.AdminEventApiTest" --no-daemon --stacktrace` (`apps/api`)
- `npm ci` + `npx tsc --noEmit` + `npm run build` (`apps/admin`)
- `rg -n "summaryDays|1~90|대용량 비동기 export" docs/events.md docs/current-usable-scope.md CLAUDE.md docs/changelog-dev.md`

## 10. 이번 사이클 기록 (2026-02-14, 7차)

### 목표
- 기능 백로그 완료: 관리자 감사 로그 대용량 비동기 export 구현

### 범위
- 포함: 비동기 export API/DB/UI 구현, 통합 테스트, 문서 동기화
- 제외: 비동기 export 결과 만료/자동 정리 배치

### 수행 작업
1. 백엔드 비동기 export job 도입
- `apps/api/src/main/resources/db/migration/V8__event_export_jobs.sql`
- `EventExportJob`/`EventExportJobStatus` 도메인 모델 및 저장소 추가
- `AdminEventExportJobUseCase`로 작업 생성/처리/다운로드 검증 구현
- `AdminEventController`에 아래 엔드포인트 추가
  - `POST /admin/events/export-jobs`
  - `GET /admin/events/export-jobs/{jobId}`
  - `GET /admin/events/export-jobs/{jobId}/download`

2. 관리자 UI 연동
- `apps/admin/src/api/adminEvents.ts`
  - 비동기 작업 생성/조회/다운로드 API 추가
- `apps/admin/src/pages/AdminEventPage.tsx`
  - "비동기 CSV 요청" 버튼 추가
  - 작업 상태(QUEUED/RUNNING/COMPLETED/FAILED) 카드 + 완료 다운로드 버튼 추가

3. 테스트 및 안정화
- `AdminEventApiTest`에 비동기 export 통합 테스트 추가
- 테스트 간 백그라운드 레이스를 방지하도록 요청자 격리 테스트 종료 전에 작업 완료 대기 처리

4. 문서 동기화
- `docs/events.md`, `docs/api-contract.md`, `docs/usecases/admin-list-events.md`
- `docs/current-usable-scope.md`, `docs/changelog-dev.md`, `CLAUDE.md`

### 검증
- `./gradlew.bat test --tests "com.eunhyehymn.presentation.controllers.AdminEventApiTest" --no-daemon --stacktrace` (`apps/api`)
- `./gradlew.bat test --no-daemon --stacktrace` (`apps/api`)
- `npm ci` + `npx tsc --noEmit` + `npm run build` (`apps/admin`)

## 11. 이번 사이클 기록 (2026-02-14, 8차)

### 목표
- 기능 백로그 완료: 비동기 export 결과 보관 정책(만료/정리) 배치 도입

### 범위
- 포함: 백엔드 정리 유스케이스/스케줄러, 테스트 보강, 문서 동기화
- 제외: 운영 대시보드/알람 지표 자동 수집

### 수행 작업
1. 백엔드 정리 정책 구현
- `CleanupEventExportJobsUseCase` 추가
  - 완료/실패 상태 작업을 보관 일수 기준으로 삭제
  - 기본 보관 일수 `7`일(최소 `1`일)
- 저장소 삭제 메서드 추가
  - `EventExportJobRepository.deleteCompletedOrFailedBefore(...)`
  - `EventExportJobJpaRepository.deleteByStatusInAndCompletedAtBefore(...)`
- 스케줄러 추가
  - `EventExportJobCleanupScheduler`
  - 기본 실행: 매일 03:15 UTC (`events.export.jobs.cleanup-cron`)

2. 설정 추가
- `application.yml`
  - `events.export.jobs.retention-days`
  - `events.export.jobs.cleanup-cron`
  - `events.export.jobs.cleanup-zone`
- `application-test.yml`
  - 테스트 환경에서 스케줄러 비활성화 (`spring.task.scheduling.enabled=false`)

3. 테스트 보강
- 단위 테스트: `CleanupEventExportJobsUseCaseTest`
- 통합 테스트: `CleanupEventExportJobsUseCaseIntegrationTest`
- 전체 API 테스트 시 격리 이슈를 막기 위해 integration test에 `tearDown` 정리 추가

4. 문서 동기화
- `docs/events.md`, `docs/current-usable-scope.md`, `docs/changelog-dev.md`, `CLAUDE.md`

### 검증
- `./gradlew.bat test --tests "com.eunhyehymn.application.usecases.CleanupEventExportJobsUseCaseTest" --tests "com.eunhyehymn.application.usecases.CleanupEventExportJobsUseCaseIntegrationTest" --no-daemon --stacktrace` (`apps/api`)
- `./gradlew.bat test --no-daemon --stacktrace` (`apps/api`)
- `npx tsc --noEmit` + `npm run build` (`apps/admin`)

## 12. 이번 사이클 기록 (2026-02-14, 9차)

### 목표
- 기능 백로그 완료: 비동기 export 운영 지표(실패율/처리시간/정리량) 정례화

### 범위
- 포함: 운영 지표 API/저장소/정리 이력 저장, Admin UI 지표 카드, 테스트/문서 동기화
- 제외: 지표 기반 알림(CloudWatch/SNS) 자동 연동

### 수행 작업
1. 백엔드 운영 지표 조회 기능 추가
- `GetEventExportOpsMetricsUseCase` 추가
- `GET /admin/events/export-jobs/metrics` 엔드포인트 추가
- 작업 상태별 집계, 실패율, 평균/95퍼센타일 처리시간 계산

2. 정리량 집계 이력 저장 추가
- `V9__event_export_job_cleanup_runs.sql` 추가
- `EventExportJobCleanupRun` 도메인/저장소/어댑터 추가
- `EventExportJobCleanupScheduler`가 실행 시 정리 결과를 이력 테이블에 저장

3. 관리자 UI 운영 지표 카드 추가
- `apps/admin/src/api/adminEvents.ts`: 운영 지표 API 클라이언트 추가
- `apps/admin/src/pages/AdminEventPage.tsx`: 기간 프리셋(1/7/30/60/90일) 기반 지표 카드 추가

4. 테스트 및 문서 동기화
- 단위 테스트: `GetEventExportOpsMetricsUseCaseTest`
- 통합 테스트: `AdminEventApiTest` 운영 지표 검증 케이스 추가
- 문서: `docs/events.md`, `docs/api-contract.md`, `docs/current-usable-scope.md`, `docs/changelog-dev.md`, `CLAUDE.md`

### 검증
- `./gradlew.bat test --tests "com.eunhyehymn.application.usecases.GetEventExportOpsMetricsUseCaseTest" --tests "com.eunhyehymn.presentation.controllers.AdminEventApiTest" --no-daemon --stacktrace` (`apps/api`)
- `./gradlew.bat test --no-daemon --stacktrace` (`apps/api`)
- `npx tsc --noEmit` + `npm run build` (`apps/admin`)

## 13. 이번 사이클 기록 (2026-02-14, 10차)

### 목표
- 기능 백로그 완료: 비동기 export 내구성/정합성/성능/알림/테스트 강화

### 범위
- 포함: 복구 스케줄러, 원자적 claim, snapshot 정합성, 정리 인덱스, 알림 스케줄러, 테스트 확장, 문서 동기화
- 제외: 외부 알림 채널(CloudWatch/SNS/PagerDuty) 직접 연동

### 수행 작업
1. 내구성 복구 경로 구현
- `EventExportJobRepository.claimQueued(...)` 추가로 작업 실행 claim 원자화
- `EventExportJobRecoveryScheduler` 추가
  - stale `RUNNING` 재큐잉
  - `QUEUED` 작업 배치 재디스패치
- `AdminEventController`에서 executor reject 시 `QUEUED` 유지(요청은 `202 Accepted`)

2. export 결과 정합성 강화
- `to` 미지정 시 작업 생성 시각을 `toExclusive`로 고정(snapshot)
- 이벤트 조회 정렬을 `createdAt DESC, id DESC`로 안정화

3. 정리 배치 성능 보강
- `V10__event_export_jobs_cleanup_index.sql` 추가
- 인덱스: `idx_event_export_jobs_status_completed_at (status, completed_at)`

4. 운영 알림 베이스라인 추가
- `EventExportJobOpsAlertScheduler` 추가
- 실패율/p95/queued 임계치 초과 시 WARN 로그 출력
- 알림/복구 스케줄러 설정 키를 `application.yml`에 추가

5. 테스트 확장
- 신규: `AdminEventExportJobUseCaseTest`
- 신규: `EventExportJobRecoverySchedulerTest`
- 신규: `EventExportJobOpsAlertSchedulerTest`
- 신규: `AdminEventControllerTest` (queue rejection 시 `202`)
- 보강: `AdminEventApiTest`
  - 미완료 다운로드 `409`
  - snapshot 결과 검증
  - 동일 시각 데이터 안정 정렬 검증

### 검증
- `./gradlew.bat test --no-daemon --stacktrace` (`apps/api`)
- `npm run build` (`apps/admin`)

## 14. 이번 사이클 기록 (2026-02-14, 11차)

### 목표
- 보안 리스크 완화: CSV export 수식 주입(Spreadsheet Formula Injection) 방어

### 범위
- 포함: 백엔드 CSV 셀 이스케이프 공통화, 동기/비동기 경로 검증 테스트, 문서 동기화
- 제외: 파일 포맷 변경(xlsx 전환), 외부 AV/콘텐츠 스캐너 연동

### 수행 작업
1. 공통 CSV 보안 유틸 도입
- `CsvUtils.toSafeCsvCell(...)` 추가
- 수식 시작 문자열(`=`, `+`, `-`, `@`) 및 선행 공백 우회 케이스를 방어하도록 prefix 처리

2. 동기/비동기 export 적용
- `AdminEventController` 동기 CSV 경로 적용
- `AdminEventExportJobUseCase` 비동기 CSV 경로 적용

3. 테스트 확장
- `CsvUtilsTest` 신규
- `AdminEventApiTest`에 동기/비동기 CSV 수식 이스케이프 통합 테스트 추가

4. 문서 동기화
- `docs/events.md`
- `docs/changelog-dev.md`

### 검증
- `./gradlew.bat test --tests "com.eunhyehymn.common.util.CsvUtilsTest" --tests "com.eunhyehymn.presentation.controllers.AdminEventApiTest" --no-daemon --stacktrace` (`apps/api`)
- `./gradlew.bat test --no-daemon --stacktrace` (`apps/api`)
- `npm run build` (`apps/admin`)

## 15. 이번 사이클 기록 (2026-02-14, 12차)

### 목표
- 기준 문서 정합성 유지: 최신 `develop`/PR 이력과 사용 가능 범위 문서 일치

### 범위
- 포함: `docs/current-usable-scope.md` 기준 커밋/근거 PR/최근 변경 포인트 최신화
- 제외: 기능 코드 변경

### 수행 작업
1. 기준 커밋/근거 PR 갱신
- 기준 커밋을 최신 `develop` HEAD(`5b08dc0`)로 수정
- 근거 PR 목록에 #56/#55/#54/#53 반영

2. 최근 PR 변경 포인트 보강
- #56 CSV 보안 하드닝
- #55 비동기 export 내구성/정합성/성능/알림
- #54 운영 지표 파이프라인
- #53 결과 정리 배치

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md`에 문서 최신화 사이클(12차) 추가

### 검증
- `rg -n "기준 커밋|근거 PR|PR #56|PR #55|PR #54|PR #53" docs/current-usable-scope.md`

## 16. 이번 사이클 기록 (2026-02-14, 13차)

### 목표
- 작업 생산성 개선: 로컬 임시 아티팩트가 `git status`를 오염시키지 않도록 ignore 정리

### 범위
- 포함: `.tmp/`, `infra/aws/tfplan*` ignore 규칙 추가
- 제외: 임시파일 실삭제, Terraform 실행 로직 변경

### 수행 작업
1. 루트 ignore 보강
- `.gitignore`에 `.tmp/` 추가

2. AWS 인프라 ignore 보강
- `infra/aws/.gitignore`에 `tfplan*` 추가

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `git status -sb`에서 `.tmp/`, `infra/aws/tfplan*` 미노출 확인

## 17. 이번 사이클 기록 (2026-02-14, 14차)

### 목표
- 스테이징 배포 안정성 강화: `docker image prune` 동시 실행 충돌로 deploy 실패하는 케이스 차단

### 범위
- 포함: `infra/aws/deploy.sh` cleanup 단계 재시도/경고 처리
- 제외: Docker prune 정책 자체 변경, ECR lifecycle 설정 변경

### 수행 작업
1. prune cleanup 함수화
- `cleanup_unused_images` 함수 추가
- `docker image prune -f`를 함수 호출로 대체

2. 경합 에러 하드닝
- `prune operation is already running` 감지 시 5초 간격 최대 3회 재시도
- 재시도 후에도 경합이면 경고 출력 후 배포는 성공 처리
- 기타 prune 오류도 경고로 기록하고 배포 진행(정리 단계 non-fatal)

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `bash -n infra/aws/deploy.sh`

## 18. 이번 사이클 기록 (2026-02-14, 15차)

### 목표
- 스테이징 배포 안정성 강화: 동일 EC2 대상 deploy job 동시 실행 차단

### 범위
- 포함: `deploy-staging.yml`에 deploy job concurrency 제어 추가
- 제외: deploy 스크립트 로직 변경, 인프라 리소스 변경

### 수행 작업
1. deploy job 직렬화
- `deploy` job에 concurrency group(`staging-ec2-deploy`) 추가
- `cancel-in-progress: false`로 설정해 진행 중 배포를 중단하지 않고 대기 처리

2. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh workflow view deploy-staging.yml --yaml`

## 19. 이번 사이클 기록 (2026-02-14, 16차)

### 목표
- CI 누락 방지: workflow/배포 스크립트 변경에도 자동 검증이 항상 실행되도록 보강

### 범위
- 포함: workflow lint 파이프라인 추가, 문서 동기화
- 제외: 애플리케이션 기능 코드 변경

### 수행 작업
1. workflow lint 파이프라인 추가
- `.github/workflows/workflow-lint.yml` 신규 추가
- `rhysd/actionlint`로 GitHub Actions YAML/표현식 lint 수행
- `bash -n infra/aws/deploy.sh`로 배포 스크립트 문법 검증 수행
- lint 기준을 통과하도록 `deploy-staging.yml`의 Docker build/push 태그 변수를 quote 처리(SC2086 대응)

2. 트리거 경로 최적화
- `.github/workflows/**` 또는 `infra/aws/deploy.sh` 변경 시에만 실행되도록 path filter 적용

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh api repos/V4N1LLA/Eunhye_Hymn/contents/.github/workflows/workflow-lint.yml?ref=ci/workflow-lint --jq .sha`

## 20. 이번 사이클 기록 (2026-02-14, 17차)

### 목표
- 스테이징 실서비스 유사 검증 강화: 배포 완료 판정 조건을 헬스체크 단일 항목에서 핵심 경로 다중 항목으로 확장

### 범위
- 포함: `deploy-staging.yml` verify 단계 강화, 문서 동기화
- 제외: 애플리케이션 기능 코드 변경

### 수행 작업
1. 배포 verify 단계 강화
- `/api/v1/ping` 응답 본문 `"ok": true`를 재시도 기반으로 검증
- Admin 루트(`/`)의 HTTP 200/301/302 응답 검증 추가
- 보호 API(`/api/v1/admin/hymns`)가 비인증 상태에서 401/403을 반환하는지 검증 추가

2. 실패 감지 정책 명확화
- 각 검증 항목에 최대 시도 횟수/대기 간격을 부여해 일시적인 기동 지연은 허용
- 재시도 소진 시 `::error::`로 명확히 실패 처리

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh api repos/V4N1LLA/Eunhye_Hymn/contents/.github/workflows/deploy-staging.yml?ref=chore/staging-smoke-verify-steps --jq .sha`

## 21. 이번 사이클 기록 (2026-02-14, 18차)

### 목표
- 스테이징 배포 완료 기준 고도화: 외부 HTTP 응답뿐 아니라 EC2 컨테이너 런타임 상태까지 자동 검증

### 범위
- 포함: `deploy-staging.yml` verify 단계에 원격 컨테이너 상태 체크 추가, 문서 동기화
- 제외: 애플리케이션 기능 코드 변경

### 수행 작업
1. 원격 컨테이너 상태 체크 추가
- `ssh + docker inspect`로 `eunhye-api`가 `running healthy`인지 재시도 기반 검증
- `eunhye-nginx`가 `running`인지 재시도 기반 검증

2. 실패 신호 명확화
- 상태 검증 재시도 소진 시 `::error::`로 즉시 실패 처리
- 마지막 관측 상태를 로그에 남겨 장애 분석 단서 보강

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh api repos/V4N1LLA/Eunhye_Hymn/contents/.github/workflows/deploy-staging.yml?ref=ci/staging-verify-container-health --jq .sha`

## 22. 이번 사이클 기록 (2026-02-14, 19차)

### 목표
- 스테이징 배포 재현성 강화: `latest` 의존도를 낮추고 워크플로우 SHA 이미지로 배포/검증 일치 보장

### 범위
- 포함: compose 이미지 태그 전략 변경, deploy/env 전달 보강, 배포 후 이미지 태그 검증 추가, 문서 동기화
- 제외: 애플리케이션 기능 코드 변경

### 수행 작업
1. 이미지 태그 전략 전환
- `infra/aws/docker-compose.prod.yml`의 API/Admin 이미지를 `${IMAGE_TAG:-latest}`로 변경

2. 배포 파이프라인 연계
- `deploy-staging.yml`에서 `IMAGE_TAG=${{ github.sha }}`를 deploy 스크립트에 전달
- `infra/aws/deploy.sh`가 `IMAGE_TAG`를 기본값 `latest`로 처리하고 로그에 노출

3. 배포 검증 강화
- Verify 단계에서 `docker inspect`로 `eunhye-api`, `eunhye-nginx`의 실제 이미지 태그가 `github.sha`와 일치하는지 확인

4. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh api repos/V4N1LLA/Eunhye_Hymn/contents/.github/workflows/deploy-staging.yml?ref=ci/staging-deploy-sha-tag --jq .sha`

## 23. 이번 사이클 기록 (2026-02-14, 20차)

### 목표
- 스테이징 배포 검증 유지보수성 강화: workflow 인라인 검증 스크립트를 분리해 재사용성과 변경 안정성 확보

### 범위
- 포함: verify 스크립트 분리, deploy workflow 하드닝(타임아웃/원격 디렉터리 보장), workflow lint 대상 확장, 문서 동기화
- 제외: 애플리케이션 기능 코드 변경, 배포 대상/인프라 리소스 변경

### 수행 작업
1. verify 단계 스크립트 분리
- `infra/aws/verify-staging.sh` 신규 추가
- 기존 `deploy-staging.yml` 인라인 함수(`check_ping`, `check_http_status`, `check_container_state`, `check_container_image_tag`)를 스크립트로 이관
- 워크플로우는 `STAGING_HOST`, `STAGING_SSH_KEY`, `EXPECTED_IMAGE_TAG` 환경 변수로 스크립트를 실행하도록 변경

2. deploy 단계 하드닝
- `deploy` job에 `timeout-minutes: 30` 추가
- 파일 전송 전에 `mkdir -p /home/ec2-user/app`를 수행해 원격 배포 디렉터리 부재로 인한 실패를 예방

3. lint 검증 범위 확장
- `.github/workflows/workflow-lint.yml` path filter에 `infra/aws/verify-staging.sh` 추가
- `bash -n infra/aws/verify-staging.sh` 문법 검증 추가

4. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `bash -n infra/aws/deploy.sh`
- `bash -n infra/aws/verify-staging.sh`

## 24. 이번 사이클 기록 (2026-02-14, 21차)

### 목표
- 스테이징 배포 회귀 복구: verify 단계 SSH 키 경로 해석 오류로 인한 실패 제거

### 범위
- 포함: `verify-staging.sh` 경로 정규화 버그 수정, workflow verify env 경로 고정, 문서 동기화
- 제외: 애플리케이션 기능 코드/배포 대상 인프라 변경

### 수행 작업
1. SSH 키 경로 정규화 버그 수정
- `expand_home_path`에서 `~/` 처리 시 문자열 슬라이싱(`${path:2}`)을 사용하도록 변경
- `~` 단일 값도 `${HOME}`으로 해석하도록 처리 추가

2. workflow 경로 명시
- `deploy-staging.yml` Verify 단계의 `STAGING_SSH_KEY`를 `/home/runner/.ssh/deploy_key` 절대 경로로 변경

3. 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `bash -n infra/aws/verify-staging.sh`

## 25. 이번 사이클 기록 (2026-02-14, 22차)

### 목표
- API 테스트 캐시 안정화: Gradle 캐시 restore 400 경고 노이즈를 줄이고 워크플로우 간 캐시 설정 일관성 확보

### 범위
- 포함: `deploy-staging.yml`/`api-ci.yml`의 API 테스트 캐시 설정 통일, 문서 동기화
- 제외: 애플리케이션 기능 코드 변경, 테스트 시나리오 변경

### 수행 작업
1. 배포 파이프라인 캐시 전략 변경
- `deploy-staging.yml`의 `test-api` job에서 `gradle/actions/setup-gradle@v3` 제거
- `actions/setup-java@v4`에 `cache: gradle` + `cache-dependency-path` 설정 추가

2. API CI 캐시 전략 통일
- `api-ci.yml`도 동일한 `setup-java` 내장 Gradle 캐시 방식으로 변경
- `cache-dependency-path`를 `apps/api` Gradle 파일/Wrapper 기준으로 명시

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh workflow view api-ci.yml --yaml`
- `gh workflow view deploy-staging.yml --yaml`

## 26. 이번 사이클 기록 (2026-02-14, 23차)

### 목표
- API CI 누락/중복 실행 최소화: 워크플로우 파일 변경 시 검증 누락을 막고 동일 브랜치 중복 실행을 제어

### 범위
- 포함: `api-ci.yml` path 트리거 보강, workflow-level concurrency 추가, 문서 동기화
- 제외: API 테스트 명령/빌드 설정 변경

### 수행 작업
1. API CI 트리거 보강
- `pull_request`/`push` path에 `.github/workflows/api-ci.yml` 추가
- API CI 워크플로우 자체 수정 시에도 자동 검증되도록 보강

2. API CI 동시성 제어 추가
- `concurrency.group`을 `api-ci-${{ github.ref }}`로 설정
- `cancel-in-progress: true`를 적용해 동일 브랜치의 이전 실행을 자동 취소

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `gh workflow view api-ci.yml --yaml`

## 27. 이번 사이클 기록 (2026-02-14, 24차)

### 목표
- 스테이징 수동 검수 진입 판단 단순화: 최신 배포 run의 핵심 상태를 한 번에 확인하는 조회 스크립트 제공

### 범위
- 포함: `staging-latest-status.ps1` 신규, runbook/체크리스트/인프라 가이드 문서 반영
- 제외: 배포 로직/애플리케이션 기능 코드 변경

### 수행 작업
1. 최신 배포 상태 조회 스크립트 추가
- `scripts/staging-latest-status.ps1` 신규 추가
- 최신 `deploy-staging.yml` run의 상태/결론/커밋/URL 및 job 요약 출력
- `deploy` job의 `Verify deployment` step 결과를 별도 필드로 노출
- `-RequireSuccess` 옵션으로 최신 run이 성공이 아니면 비정상 종료
- `-AsJson` 옵션으로 자동화 소비 가능한 JSON 출력 지원

2. 운영 문서 동기화
- `docs/runbook.md` 배포 체크리스트/절차에 상태 확인 명령 추가
- `docs/staging-smoke-checklist.md` 실행 전 준비 항목에 상태 확인 명령 추가
- `infra/aws/README.md` 리허설 가이드 섹션에 상태 확인 명령 추가

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `powershell -File scripts/staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Workflow deploy-staging.yml -AsJson -RequireSuccess`

## 28. 이번 사이클 기록 (2026-02-14, 25차)

### 목표
- 스테이징 상태 조회 정확도 향상: 배포(push)와 리허설(workflow_dispatch) run을 필터링 조회 가능하도록 스크립트 확장

### 범위
- 포함: `staging-latest-status.ps1` 옵션 확장(`-Branch`, `-Event`), 운영 문서 안내 보강
- 제외: 배포 workflow 로직 변경, 애플리케이션 기능 코드 변경

### 수행 작업
1. 조회 옵션 확장
- `scripts/staging-latest-status.ps1`에 `-Branch`(기본 `develop`) 옵션 추가
- `scripts/staging-latest-status.ps1`에 `-Event` 옵션 추가
- 내부 `gh run list` 호출 시 branch/event 필터를 선택적으로 적용하도록 개선

2. 운영 문서 동기화
- `docs/runbook.md`에 브랜치/이벤트 필터 옵션 안내 추가
- `infra/aws/README.md`에 리허설 run 확인 시 `-Branch`, `-Event` 사용 가이드 추가

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `powershell -File scripts/staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -RequireSuccess -AsJson`

## 29. 이번 사이클 기록 (2026-02-14, 26차)

### 목표
- 스테이징 점검 기록 작성 효율화: 최신 배포 상태를 문서/티켓에 바로 붙여넣을 수 있는 Markdown 출력 지원

### 범위
- 포함: `staging-latest-status.ps1`의 `-AsMarkdown` 출력 추가, 관련 운영 문서 옵션 안내 동기화
- 제외: 배포 workflow 로직 변경, 애플리케이션 기능 변경

### 수행 작업
1. Markdown 출력 모드 추가
- `scripts/staging-latest-status.ps1`에 `-AsMarkdown` 옵션 추가
- run 요약 + job 상태를 Markdown 테이블로 출력하도록 구현
- `-AsJson`/`-AsMarkdown` 동시 사용 시 충돌 에러 처리 추가

2. 운영 문서 동기화
- `docs/runbook.md`에 `-AsMarkdown` 사용 안내 추가
- `docs/staging-smoke-checklist.md`에 기록용 표 출력 안내 추가
- `infra/aws/README.md`에 Markdown 출력 옵션 안내 추가

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `powershell -File scripts/staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Event push -RequireSuccess -AsMarkdown`

## 30. 이번 사이클 기록 (2026-02-14, 27차)

### 목표
- 스테이징 수동 검수 진입 기준 강화: 최신 배포 run의 성공 여부뿐 아니라 deploy/verify 단계 성공과 실행 신선도까지 자동 게이트화

### 범위
- 포함: `staging-latest-status.ps1` 옵션 확장(`-RequireDeploySuccess`, `-RequireVerifySuccess`, `-MaxAgeMinutes`), 운영 문서 동기화
- 제외: 배포 workflow 로직 변경, 애플리케이션 기능 코드 변경

### 수행 작업
1. 상태 확인 게이트 확장
- `scripts/staging-latest-status.ps1`에 `-RequireDeploySuccess` 추가
- `scripts/staging-latest-status.ps1`에 `-RequireVerifySuccess` 추가
- `scripts/staging-latest-status.ps1`에 `-MaxAgeMinutes` 추가
- run 요약에 `RunAgeMinutes` 필드 추가, Markdown 출력 테이블에 `AgeMin` 컬럼 추가

2. 운영 문서 동기화
- `docs/runbook.md`, `docs/staging-smoke-checklist.md`, `infra/aws/README.md`의 상태 확인 명령을 강화 옵션 기준으로 갱신
- `-MaxAgeMinutes` 신선도 기준 사용 가이드 추가

3. 변경 이력 문서 동기화
- `docs/changelog-dev.md` 업데이트

### 검증
- `powershell -File scripts/staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Event push -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120 -AsJson`
- `powershell -File scripts/staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Event push -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120 -AsMarkdown`
- `powershell -File scripts/staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Event push -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 1 -AsJson` (expected non-zero exit)

## 31. Cycle record (2026-02-14, login guidance patch)

### Goal
- Remove staging login confusion by clarifying token input semantics for social login.

### Scope
- Included: Admin login UI label/help updates for Kakao token types, hide dev login on non-localhost.
- Excluded: auth API logic changes, deployment workflow changes.

### Work
1. Token input clarification
- `apps/admin/src/pages/LoginPage.tsx`
- Provider-specific token label/help:
  - Kakao -> `Access Token`
  - Kakao -> `Access Token` (without `Bearer ` prefix)

2. Dev login visibility guard
- `apps/admin/src/pages/LoginPage.tsx`
- Dev login section is rendered only for `localhost` and `127.0.0.1`.

3. Documentation sync
- `docs/changelog-dev.md` updated.

### Validation
- `cd apps/admin && npm ci && npx tsc --noEmit && npm run build`

## 32. 이번 사이클 기록 (2026-02-15, staging feedback checklist)

### 목표
- 스테이징 수동 검수 단계에서 피드백 품질과 재현 속도를 높이기 위한 표준 체크리스트를 추가한다.

### 범위
- 포함: 피드백 루프 체크리스트 문서 신설, README/CLAUDE 동기화
- 제외: 애플리케이션 기능 코드 변경, 배포 워크플로 변경

### 수행 작업
1. 스테이징 피드백 체크리스트 추가
- `docs/staging-feedback-checklist.md` 신규 추가
- 작은 배포 단위, 고정 검증 시나리오, 심각도 기반 처리 규칙, 사이클 종료 기준을 명시
- 이슈 전달용 표준 템플릿(`환경/재현절차/기대결과/실제결과/로그/심각도/완료기준`) 포함

2. 문서 진입점 동기화
- `README.md` 문서 목록에 `docs/staging-feedback-checklist.md` 링크 추가

3. 작업 기준 문서 동기화
- `CLAUDE.md` 마지막 업데이트 날짜를 `2026-02-15`로 갱신
- 문서/준비도 점검 항목에 새 체크리스트 반영
- 운영 문서/절차 고도화 항목에 새 체크리스트 반영

### 검증
- `rg -n "staging-feedback-checklist" README.md CLAUDE.md docs/WORK_CYCLE.md docs/staging-feedback-checklist.md`
- `rg -n "^(<<<<<<<|>>>>>>>|=======)$" README.md CLAUDE.md docs/WORK_CYCLE.md docs/staging-feedback-checklist.md`

## 33. 이번 사이클 기록 (2026-02-15, mobile friendly ui/ux)

### 목표
- 모바일 앱의 목록/히스토리 탐색 경험을 더 빠르고 명확하게 만들어 스테이징 수동 검수 효율을 높인다.

### 범위
- 포함: 찬양 목록/최근 열람 화면 UI·UX 개선, 모바일 문서/작업 기준 문서 동기화
- 제외: API 계약 변경, 인증/도메인 로직 변경, 배포 워크플로 변경

### 수행 작업
1. 찬양 목록 UX 개선
- `apps/mobile/lib/src/features/hymn/hymn_list_page.dart`
- 검색어 즉시 지우기 버튼, 태그 필터(ChoiceChip), 필터 초기화 액션 추가
- 로딩 스켈레톤, 빈 상태 가이드, soft error 배너(캐시 fallback 안내) 추가
- 카드 정보 구조 개선(태그 칩/가독성 강화)

2. 최근 열람 UX 개선
- `apps/mobile/lib/src/features/history/history_page.dart`
- 검색 + 기간 필터(전체/오늘/최근 7일/최근 30일) 추가
- 필터 초기화, 빈 상태 가이드, soft error 배너 추가
- 히스토리 카드 태그 노출 및 정렬/시간 표기 정돈

3. 문서 동기화
- `docs/mobile/README.md`: 모바일 기능 목록에 목록/히스토리 UX 개선 내역 반영
- `CLAUDE.md`: 모바일 구현 기능/파일 역할 설명에 UX 개선 반영

### 검증
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 analyze`
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 test`

## 34. 이번 사이클 기록 (2026-02-16, mobile release readiness)

### 목표
- 모바일 배포 패키징 준비의 첫 단계로 Android release APK 빌드 검증 경로를 CI에 추가한다.

### 범위
- 포함: release APK 빌드 워크플로우 추가, 모바일/기준 문서 동기화
- 제외: 앱스토어 서명 키 연동, iOS 릴리즈 파이프라인 구성

### 수행 작업
1. Mobile release 검증 워크플로우 추가
- `.github/workflows/mobile-release-check.yml` 신규 추가
- PR/develop push + workflow_dispatch 트리거 구성
- `flutter build apk --release` 실행 및 `app-release.apk` artifact 업로드

2. 모바일 문서 동기화
- `docs/mobile/README.md`에 release 패키징 준비 섹션 추가
- 수동 실행 입력(`api_base_url`) 및 artifact 확인 경로 안내 반영

3. 기준 문서 동기화
- `CLAUDE.md` 마지막 업데이트 날짜 갱신
- 워크플로우 목록/CI 설명/미완료 항목(모바일 배포 패키징) 상태 갱신

### 검증
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 analyze`
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 test`
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 build apk --release --dart-define=API_BASE_URL=https://example.com/api/v1`
- 로컬 환경에서 Android SDK 미설치로 release APK 빌드는 실패(`Android SDK could not be found`), CI 워크플로우에서 동일 단계로 검증 보완

## 35. 이번 사이클 기록 (2026-02-16, changelog sync for merged mobile PRs)

### 목표
- 머지 완료된 모바일 작업(#89, #90)이 `docs/changelog-dev.md`에도 누락 없이 반영되도록 문서 싱크를 맞춘다.

### 범위
- 포함: changelog 최신화, 기준 문서(CLAUDE/WORK_CYCLE) 동기화
- 제외: 애플리케이션 코드/워크플로우 동작 변경

### 수행 작업
1. changelog 누락분 반영
- `docs/changelog-dev.md`에 `2026-02-16` 섹션 추가
- PR #89(모바일 목록/히스토리 UX 개선)과 PR #90(Android release readiness) 반영
- 머지 커밋(`0520074`, `35569af`) 기준으로 추적 정보 명시

2. 기준 문서 동기화
- `CLAUDE.md` 문서/준비도 점검 항목에 `docs/changelog-dev.md` 동기화 항목 추가
- `docs/WORK_CYCLE.md`에 이번 사이클 기록 추가

### 검증
- `rg -n "2026-02-16|PR #89|PR #90|0520074|35569af" docs/changelog-dev.md`
- `rg -n "changelog-dev.md" CLAUDE.md docs/WORK_CYCLE.md`
- `rg -n "^(<<<<<<<|>>>>>>>|=======)$" docs/changelog-dev.md CLAUDE.md docs/WORK_CYCLE.md`

## 36. 이번 사이클 기록 (2026-02-17, docs latest sync + pr/merge)

### 목표
- 최신 `develop` 기준으로 핵심 운영 문서를 동기화하고 PR 생성/머지까지 완료한다.

### 범위
- 포함: `README.md`, `docs/current-usable-scope.md`, `docs/deployment-readiness-audit.md` 최신화 + 추적 문서(`docs/changelog-dev.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`) 동기화
- 제외: 애플리케이션 코드/인프라 동작 변경

### 수행 작업
1. 문서 기준점 최신화
- `docs/current-usable-scope.md`
- 작성일/기준 커밋을 `c578c3f` 기준으로 갱신
- 근거 PR에 #91/#90/#89/#88/#87 반영
- 스테이징/모바일 실행 증빙(run `22052664286`, `22052482140`) 반영

2. 배포 준비도 리포트 최신화
- `docs/deployment-readiness-audit.md`
- 작성일/점검 브랜치 최신화
- 최신 GitHub Actions 성공 run 근거 추가
- 잔여 리스크를 "수동 스모크 정례화" 기준으로 명시

3. 루트 문서 정합성 보강
- `README.md` CI/CD 설명을 현재 워크플로우 구성(API/Admin/Mobile + mobile release + staging deploy)에 맞게 갱신

4. 추적 문서 동기화
- `docs/changelog-dev.md`에 2026-02-17 문서 동기화 이력 추가
- `CLAUDE.md` 마지막 업데이트 날짜 및 문서/준비도 점검 항목 동기화
- `docs/WORK_CYCLE.md`에 본 사이클 기록 추가

### 검증
- `git pull --ff-only origin develop`
- `gh run list --repo V4N1LLA/Eunhye_Hymn --workflow deploy-staging.yml --limit 5 --json databaseId,headBranch,headSha,status,conclusion,url`
- `gh run list --repo V4N1LLA/Eunhye_Hymn --workflow mobile-release-check.yml --limit 5 --json databaseId,headBranch,headSha,status,conclusion,url`
- `rg -n "c578c3f|22052664286|22052482140|#91|#90|#89|#88|#87" docs/current-usable-scope.md docs/deployment-readiness-audit.md docs/changelog-dev.md`
- `rg -n "Mobile release APK check|Staging 자동 배포" README.md`
- `rg -n "^(<<<<<<<|>>>>>>>|=======)$" README.md docs/current-usable-scope.md docs/deployment-readiness-audit.md docs/changelog-dev.md CLAUDE.md docs/WORK_CYCLE.md`

## 37. 이번 사이클 기록 (2026-02-17, staging ops cycle + mobile store readiness)

### 목표
- 우선순위 1~3 항목(스테이징 수동 스모크 정례화, preflight 무스킵 유지, 모바일 스토어 배포 준비)을 한 사이클로 구현/검증/PR/머지까지 완료한다.

### 범위
- 포함: 운영 사이클 자동화 스크립트 + preflight/status 하드닝 + 모바일 릴리즈 readiness 워크플로우 및 서명 설정 + 문서 동기화
- 제외: 실제 AWS 자격증명 재발급, 실제 앱스토어 업로드

### 수행 작업
1. 운영 사이클 자동화
- `scripts/staging-ops-cycle.ps1` 추가
- preflight + 최신 배포 게이트 + `docs/staging-smoke-log.md` 로그 적재를 일괄 수행

2. preflight/status 하드닝
- `scripts/staging-preflight.ps1`: 세션 만료/자격증명/프로필 오류 메시지 가시성 강화
- `scripts/staging-latest-status.ps1`: 진행 중 job의 `completedAt=0001-01-01` 케이스에서 duration 음수 계산 방지
- `-PreferCompleted` 옵션 추가로 최신 완료 run 기준 점검 지원

3. 모바일 스토어 readiness
- `apps/mobile/android/app/build.gradle.kts`
  - `key.properties` 기반 release signing 지원
  - 미구성 시 debug signing fallback 유지
- `apps/mobile/android/key.properties.example` 추가
- `.github/workflows/mobile-store-release.yml` 추가
  - manual target 선택(`android|ios|both`)
  - Android signed AAB build
  - iOS release no-codesign build
  - 리뷰 반영: keystore 생성 경로를 `android/app/keystore/release.jks`로 수정

4. 문서 동기화
- 운영 문서: `docs/staging-smoke-checklist.md`, `docs/staging-feedback-checklist.md`, `docs/runbook.md`, `infra/aws/README.md`
- 모바일 문서: `apps/mobile/README.md`, `docs/mobile/README.md`
- 기준 문서: `README.md`, `docs/current-usable-scope.md`, `docs/deployment-readiness-audit.md`, `docs/changelog-dev.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn`
- `aws sts get-caller-identity` (현재 환경: session expired 확인)
- `powershell -NoProfile -File .\\scripts\\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -AsJson`
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex`
- `rg -n "staging-ops-cycle|session expired|22119056042|mobile-store-release|key.properties.example" scripts docs README.md CLAUDE.md apps/mobile`
- `rg -n "^(<<<<<<<|>>>>>>>|=======)$" .github/workflows/mobile-store-release.yml apps/mobile/android/app/build.gradle.kts scripts/staging-preflight.ps1 scripts/staging-latest-status.ps1 scripts/staging-ops-cycle.ps1 docs/staging-smoke-checklist.md docs/staging-feedback-checklist.md docs/runbook.md docs/staging-smoke-log.md docs/mobile/README.md apps/mobile/README.md docs/current-usable-scope.md docs/deployment-readiness-audit.md docs/changelog-dev.md CLAUDE.md docs/WORK_CYCLE.md README.md infra/aws/README.md`

## 38. 이번 사이클 기록 (2026-02-20, review comment thread response policy sync)

### 목표
- 리뷰 코멘트 반영 내역을 thread 단위로 기록하는 규칙을 작업 사이클 기준에 고정한다.

### 범위
- 포함: `docs/WORK_CYCLE.md`, `CLAUDE.md` 작업 사이클 규칙 갱신
- 제외: 애플리케이션 코드/인프라 동작 변경

### 수행 작업
1. 리뷰 답글 기록 규칙 명문화
- `docs/WORK_CYCLE.md`의 "8. 리뷰 반영"에 thread별 답글 의무 추가
- `CLAUDE.md`의 작업 주의사항/사이클 단계에 동일 규칙 반영

2. 원격 동기화 준비
- 문서 변경 커밋 및 원격 브랜치 푸시

### 검증
- `rg -n "thread|답글|리뷰 코멘트" docs/WORK_CYCLE.md CLAUDE.md`
- `rg -n "^(<<<<<<<|>>>>>>>|=======)$" docs/WORK_CYCLE.md CLAUDE.md`

## 39. 이번 사이클 기록 (2026-02-20, staging preflight auto-login + ops cycle gate)

### 목표
- 스테이징 운영 사이클의 1순위 블로커(preflight 세션 만료)를 자동 복구 가능한 형태로 줄이고, 최신 run 완료 대기 기반 게이트를 고정한다.

### 범위
- 포함: `scripts/staging-preflight.ps1`, `scripts/staging-ops-cycle.ps1`, `scripts/staging-latest-status.ps1`, `scripts/staging-rehearsal.ps1` 및 운영 문서 동기화
- 제외: AWS 자격증명 재발급, 실제 모바일 스토어 업로드

### 수행 작업
1. preflight 자동 복구
- `scripts/staging-preflight.ps1`에 `-AutoLogin` 옵션 추가
- `aws sts get-caller-identity` 실패(`session expired`/`credentials missing`) 시 `aws sso login` 자동 재시도 지원
- 자동 복구 결과를 체크 테이블(`aws session recovery`)로 출력

2. 운영 사이클 게이트 보강
- `scripts/staging-ops-cycle.ps1`에 `-AutoLogin` 옵션 추가(내부 preflight 전달)
- `-WaitForCompletion` 시 `-PreferCompleted` 대신 최신 run 완료 대기 경로 사용
- `scripts/staging-latest-status.ps1`의 `-AsJson/-AsMarkdown + -Wait` 출력 충돌 수정

3. 리허설/문서 동기화
- `scripts/staging-rehearsal.ps1`에 `-AutoLogin` 옵션 추가
- 운영 문서 명령 갱신
  - `docs/runbook.md`
  - `docs/staging-smoke-checklist.md`
  - `infra/aws/README.md`
  - `docs/current-usable-scope.md`
  - `CLAUDE.md`
  - `docs/changelog-dev.md`
- 실행 증빙 반영
  - `docs/staging-smoke-log.md` (`run 22206920873`, `HOLD`)

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn`
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -AutoLogin`
- `powershell -NoProfile -File .\\scripts\\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -SkipPreflight -AutoLogin -DryRun`
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -AutoLogin -WaitForCompletion`
- `rg -n "AutoLogin|WaitForCompletion|aws session recovery|22206920873" scripts docs CLAUDE.md infra/aws/README.md`

## 40. 이번 사이클 기록 (2026-02-20, aws login fallback + preflight pass recovery)

### 목표
- AutoLogin 경로가 `sso_start_url` 미설정 환경에서도 동작하도록 보강하고, preflight PASS를 실제로 복구한다.

### 범위
- 포함: `scripts/staging-preflight.ps1` fallback 보강 + 운영 문서/로그 최신화
- 제외: Admin/Mobile 수동 스모크 실제 수행

### 수행 작업
1. AutoLogin fallback 보강
- `scripts/staging-preflight.ps1`
  - `aws sso login` 실패/비구성 시 `aws login` fallback 재시도 추가
  - fallback 실패 시 원인(지원 불가/명령 실패)을 체크 결과에 누적 출력

2. 운영 실행 및 증빙 갱신
- `aws logout --profile default`로 만료 상태를 재현한 뒤 `staging-preflight.ps1 -AutoLogin` 재실행
- `aws login` fallback 자동 복구로 preflight PASS 확인
- `staging-ops-cycle.ps1 -AutoLogin -WaitForCompletion` 재실행
  - `run 22207213127` 기준 preflight/deploy/verify PASS, 판정 `CONDITIONAL_GO`
- `docs/staging-smoke-log.md`, `docs/staging-smoke-checklist.md`에 실행 결과 반영

3. 기준 문서 동기화
- `docs/runbook.md`, `infra/aws/README.md`, `docs/current-usable-scope.md`, `CLAUDE.md`, `docs/changelog-dev.md` 갱신

### 검증
- `aws sts get-caller-identity`
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn`
- `powershell -NoProfile -File .\\scripts\\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -AutoLogin`
- `aws logout --profile default`
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -AutoLogin -WaitForCompletion`
- `rg -n "aws login|22207213127|CONDITIONAL_GO" docs/staging-smoke-log.md docs/staging-smoke-checklist.md docs/changelog-dev.md CLAUDE.md`

## 41. 이번 사이클 기록 (2026-02-20, mobile store release ops cycle automation)

### 목표
- 모바일 스토어 릴리즈 작업을 반복 가능한 사이클로 고정한다. (preflight → dispatch → run watch → 로그 적재)

### 범위
- 포함: 모바일 스토어 운영 스크립트 추가, Android build_only 실검증, 문서 동기화
- 제외: Google Play 실제 업로드(`play_upload`), iOS TestFlight 실제 업로드

### 수행 작업
1. 모바일 스토어 운영 스크립트 추가
- `scripts/mobile-store-preflight.ps1`
  - target/mode 기준 필수 시크릿/입력값 점검
- `scripts/mobile-store-cycle.ps1`
  - preflight 실행 후 `mobile-store-release.yml` workflow_dispatch
  - run ID 감지/완료 대기/결과 로그(`docs/mobile-store-release-log.md`) 자동 적재

2. 실검증
- Android build_only 실행 성공
  - run `22210175274`
  - run `22210322592`
- iOS testflight preflight 실행 결과: `MOBILE_IOS_*` 시크릿 미구성으로 실패(예상)

3. 문서 동기화
- `apps/mobile/README.md`, `docs/mobile/README.md`
- `docs/mobile-store-release-log.md`
- `docs/changelog-dev.md`, `docs/current-usable-scope.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`

### 검증
- `powershell -NoProfile -File .\\scripts\\mobile-store-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -Target android -AndroidDistributionMode build_only`
- `powershell -NoProfile -File .\\scripts\\mobile-store-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -Target ios -IosDistributionMode testflight`
- `powershell -NoProfile -File .\\scripts\\mobile-store-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -Target android -AndroidDistributionMode build_only -IosDistributionMode build_only`
- `gh run view 22210175274 --repo V4N1LLA/Eunhye_Hymn --json conclusion,status,url,headSha`
- `rg -n "mobile-store|22210175274|22210322592|MOBILE_IOS_" scripts docs apps/mobile CLAUDE.md`

## 42. 이번 사이클 기록 (2026-02-20, mobile UX restructure + profile change approval flow)

### 목표
- 사용자 요청 기준으로 모바일 정보구조/문구를 정리하고, 개인정보 변경 요청을 관리자 승인 플로우로 연결한 뒤 스테이징 배포까지 완료한다.

### 범위
- 포함: mobile UI/UX 개편, 인증/프로필 확장, admin 승인 화면, API/usecase/migration 추가, 문서화, 스테이징 배포
- 제외: 결제 실연동(후원/구독), 운영 정책 결정(반려 기준/알림 채널)

### 수행 작업
1. 모바일 UX/네비게이션 개편
- 하단 탭 중심 구조를 드로어 메뉴 구조로 변경
- 상단 중복 헤더(찬양/히스토리/내 정보) 제거
- 홈 뒤로가기 동작을 "한 번 더 누르면 종료" 패턴으로 통일
- 로그인 CTA 개선: Kakao 아이콘 + `카카오로 시작하기`, 이메일 로그인 진입 복원
- 설정 카피를 일반 사용자 관점 문구로 정리하고 화면 단위를 분리

2. 개인정보 변경 요청 승인 플로우
- 사용자 요청 API 추가
  - `POST /me/profile-change-requests`
  - `GET /me/profile-change-requests/latest`
- 관리자 처리 API 추가
  - `GET /admin/profile-change-requests`
  - `PATCH /admin/profile-change-requests/{id}`
- Admin 웹에 `개인정보 변경 요청` 페이지/네비게이션 추가
- DB migration 추가
  - `V15__profile_change_requests.sql`
  - `V16__add_gender_to_profiles.sql`

3. 인증/프로필 안정화 + AI 추천 확장
- 가입 이후 인증 흐름을 invite/phone/sms 단계로 분리하고 모바일 플로우 연결
- 프로필/온보딩 성별 필드 추가 및 기존 계정 호환(`UNKNOWN`) 처리
- `SegmentedButton` 선택 집합 비어있음 assertion 재발 방지
- AI 찬송 추천 API/클라이언트/UI 진입점 추가

4. 문서화
- `docs/changelog-dev.md`에 기능/수정/검증 내역 추가
- API 계약 문서와 모바일 README 동기화(`docs/api-contract.md`, `docs/mobile/README.md`, `apps/mobile/README.md`)

### 검증
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 analyze`
- `cd apps/mobile && ..\\..\\scripts\\flutterw.ps1 test`
- `cd apps/api && .\\gradlew.bat test`
- `cd apps/admin && npm run build`
- `powershell -NoProfile -File .\\scripts\\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -AsJson`

## 43. 이번 사이클 기록 (2026-02-22, staging secret rotation audit automation)

### 목표
- 운영 시크릿 로테이션 점검을 수동 확인에서 자동 기준(`OK/STALE/MISSING`)으로 전환한다.

### 범위
- 포함: 시크릿 최신성 점검 스크립트 추가, 운영 문서 반영
- 제외: 실제 시크릿 재발급/교체 작업

### 수행 작업
1. 시크릿 점검 스크립트 추가
- `scripts/staging-secret-rotation-audit.ps1`
- GitHub repo secrets의 `updatedAt`을 조회해 상태 판정
- 기본 스테이징 필수 시크릿 점검 + `-IncludeMobileReleaseSecrets` 확장 지원
- `-MaxAgeDays` 초과 시 `STALE` 판정 및 종료코드 1 반환

2. 운영 문서 동기화
- `docs/runbook.md`: 월간 정기 점검에 자동 점검 명령/조치 기준 추가
- `docs/SECRETS_MANAGEMENT.md`: 로테이션 자동 점검 명령/상태 해석 추가
- `infra/aws/README.md`: 운영 점검 루틴에 시크릿 점검 명령 추가
- `docs/changelog-dev.md`: 변경 이력 기록

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-secret-rotation-audit.ps1 -Repo V4N1LLA/Eunhye_Hymn -MaxAgeDays 90`
- `powershell -NoProfile -File .\\scripts\\staging-secret-rotation-audit.ps1 -Repo V4N1LLA/Eunhye_Hymn -MaxAgeDays 90 -IncludeMobileReleaseSecrets`
- `rg -n "staging-secret-rotation-audit|STALE|MISSING" docs/runbook.md docs/SECRETS_MANAGEMENT.md infra/aws/README.md docs/changelog-dev.md docs/WORK_CYCLE.md`

## 44. 이번 사이클 기록 (2026-02-23, manual smoke recency gate + auto recording)

### 목표
- 스테이징 운영 사이클에서 수동 스모크 기록 누락/지연을 자동 감지하고, 결과 기록을 스크립트로 표준화한다.

### 범위
- 포함: `staging-ops-cycle.ps1` 게이트/기록 파라미터 확장, 운영 문서 동기화
- 제외: AWS 인프라 변경, 실제 스테이징 배포/수동 스모크 실행

### 수행 작업
1. 운영 사이클 스크립트 확장
- `scripts/staging-ops-cycle.ps1`
- 추가 옵션:
  - `-ManualSmokeMaxAgeDays` (기본 7)
  - `-ManualSmokeResult PASS|FAIL`
  - `-ManualSmokeEvidence`
  - `-ManualSmokeNotes`
  - `-SkipManualSmokeRecencyGate`
- 최신 수동 스모크 기록(`docs/staging-smoke-log.md`의 PASS/FAIL) 파싱 후 최신성 게이트 적용
- 기록 누락/지연 시 `Manual Smoke=OVERDUE`, `Decision=HOLD`

2. 운영 문서 동기화
- `docs/runbook.md`: 운영 체크리스트/수동 결과 기록 명령 반영
- `docs/staging-smoke-checklist.md`: `OVERDUE` 해석 및 자동 기록 명령 반영
- `infra/aws/README.md`: 운영 명령/옵션/자동 기록 절차 반영
- `docs/current-usable-scope.md`: 우선 과제 문구 갱신
- `docs/changelog-dev.md`: 변경 이력 반영

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -SkipPreflight -SkipManualSmokeRecencyGate`
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -SkipPreflight` (기존 로그 기준 수동 스모크 recency 게이트 동작 확인)
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -SkipPreflight -ManualSmokeResult PASS -ManualSmokeEvidence https://example.com/smoke`
- `rg -n "ManualSmokeMaxAgeDays|ManualSmokeResult|OVERDUE|SkipManualSmokeRecencyGate" scripts/staging-ops-cycle.ps1 docs/runbook.md docs/staging-smoke-checklist.md infra/aws/README.md`

## 45. 이번 사이클 기록 (2026-02-23, secrets rotation cycle log automation)

### 목표
- 시크릿 로테이션 점검 결과를 수동 출력 확인에서 운영 로그 누적 방식으로 전환한다.

### 범위
- 포함: 시크릿 점검 사이클 스크립트 추가, 로그 문서/운영 문서 동기화
- 제외: 실제 시크릿 값 교체/등록

### 수행 작업
1. 시크릿 점검 사이클 스크립트 추가
- `scripts/secrets-rotation-cycle.ps1`
- `staging-secret-rotation-audit.ps1 -AsJson` 실행 결과를 수집
- 상태 집계(`OK/STALE/MISSING/UNKNOWN`) 후 `Decision=PASS/HOLD` 산출
- `docs/secrets-rotation-log.md`에 감사 결과를 표 형태로 누적 기록

2. 운영 문서 동기화
- `docs/secrets-rotation-log.md` 신규 생성
- `docs/SECRETS_MANAGEMENT.md`: 월간 점검 명령을 cycle 스크립트 기준으로 갱신
- `docs/runbook.md`: 월간 정기 점검 항목을 cycle 스크립트 기준으로 갱신
- `infra/aws/README.md`: 운영 가이드 명령/로그 경로 갱신
- `docs/current-usable-scope.md`, `docs/changelog-dev.md` 반영

### 검증
- `powershell -NoProfile -File .\\scripts\\secrets-rotation-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Owner codex -MaxAgeDays 90 -LogFile .tmp\\secrets-rotation-cycle\\staging-only.md`
- `powershell -NoProfile -File .\\scripts\\secrets-rotation-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Owner codex -MaxAgeDays 90 -IncludeMobileReleaseSecrets -LogFile .tmp\\secrets-rotation-cycle\\with-mobile.md` (현재 기준 expected fail)
- `rg -n "secrets-rotation-cycle|secrets-rotation-log|Decision=HOLD|IncludeMobileReleaseSecrets" scripts/secrets-rotation-cycle.ps1 docs/SECRETS_MANAGEMENT.md docs/runbook.md infra/aws/README.md docs/changelog-dev.md docs/WORK_CYCLE.md docs/current-usable-scope.md`

## 46. 이번 사이클 기록 (2026-02-23, ops health cycle + exception-aware monitoring)

### 목표
- 운영자가 배포 게이트/시크릿 점검/예외 원인을 한 번에 확인하고, 디버깅 가능한 구조화 출력까지 확보한다.

### 범위
- 포함: 통합 운영 헬스 사이클 스크립트 추가, JSON 출력 모드 확장, 운영 로그/문서 동기화
- 제외: AWS 리소스 변경, 실제 시크릿 값 교체

### 수행 작업
1. 통합 운영 헬스 스크립트 추가
- `scripts/ops-health-cycle.ps1`
- `staging-ops-cycle.ps1` + `secrets-rotation-cycle.ps1`를 순차 실행
- 단계별 결과를 `PASS/HOLD/ERROR`로 정규화하고 `FailureCategory` 분류
- `docs/ops-health-log.md`에 통합 상태/증빙/노트 누적 기록

2. 예외/디버깅 하드닝
- `scripts/staging-ops-cycle.ps1`
  - `-AsJson` 지원
  - status 조회 실패/JSON 파싱 실패 시 구조화된 에러 payload 출력
- `scripts/secrets-rotation-cycle.ps1`
  - `-AsJson` 지원
  - 집계 결과 + 상세(stale/missing/unknown) 목록 구조화 출력

3. 운영 문서 동기화
- `docs/ops-health-log.md` 신규 생성
- `docs/runbook.md`: 통합 운영 헬스 사이클 명령/로그 반영
- `docs/SECRETS_MANAGEMENT.md`: 통합 모니터링 경로 반영
- `infra/aws/README.md`: 운영 명령/실패 원인 분류 로그 반영
- `docs/current-usable-scope.md`, `docs/changelog-dev.md` 갱신

### 검증
- `powershell -NoProfile -File .\\scripts\\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -SkipPreflight -SkipManualSmokeRecencyGate -MaxAgeMinutes 10000 -LogFile .tmp\\ops-health-cycle\\staging-log.md -AsJson`
- `powershell -NoProfile -File .\\scripts\\secrets-rotation-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Owner codex -MaxAgeDays 90 -LogFile .tmp\\ops-health-cycle\\secrets-log.md -AsJson`
- `powershell -NoProfile -File .\\scripts\\ops-health-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -SkipPreflight -SkipManualSmokeRecencyGate -StagingMaxAgeMinutes 10000 -SecretsMaxAgeDays 90 -OpsHealthLogFile .tmp\\ops-health-cycle\\ops-health-log.md -StagingLogFile .tmp\\ops-health-cycle\\staging-log.md -SecretsLogFile .tmp\\ops-health-cycle\\secrets-log.md`
- `powershell -NoProfile -File .\\scripts\\ops-health-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner codex -SkipPreflight -SkipManualSmokeRecencyGate -StagingMaxAgeMinutes 10000 -SecretsMaxAgeDays 90 -IncludeMobileReleaseSecrets -OpsHealthLogFile .tmp\\ops-health-cycle\\ops-health-log.md -StagingLogFile .tmp\\ops-health-cycle\\staging-log.md -SecretsLogFile .tmp\\ops-health-cycle\\secrets-log.md` (현재 기준 expected fail; secret missing)
- `rg -n "ops-health-cycle|AsJson|FailureCategory|ops-health-log" scripts docs infra/aws/README.md`

