# 개발 변경 이력

작업 단위별 핵심 변경만 기록한다. 상세 구현은 각 PR 본문과 커밋 로그를 참고한다.

## 2026-02-14

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
