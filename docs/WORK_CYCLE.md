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
- 핵심: Google/Kakao SDK 로그인 흐름 추가, KakaoTalk 실패 시 계정 로그인 fallback

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
