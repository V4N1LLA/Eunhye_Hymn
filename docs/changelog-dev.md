# 개발 변경 이력

작업 단위별 핵심 변경만 기록한다. 상세 구현은 각 PR 본문과 커밋 로그를 참고한다.

## 2026-02-13

### Admin 토큰 저장 하드닝(작업중)
- Admin 웹 토큰 저장소를 `localStorage`에서 `sessionStorage` 기반으로 전환
- 구버전 `localStorage` 토큰은 최초 로드시 `sessionStorage`로 마이그레이션 후 삭제
- 적용 파일:
  - `apps/admin/src/auth/tokenStore.ts`
  - `apps/admin/src/auth/AuthContext.tsx`
  - `apps/admin/src/api/client.ts`
  - `apps/admin/src/api/adminEvents.ts`

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

### 관리자 감사 로그 기능(작업중)
- API
  - `GET /api/v1/admin/events`
  - `GET /api/v1/admin/events/export`
- Admin UI
  - `/events` 페이지에서 필터/집계/페이지네이션/CSV 내보내기 제공
- 테스트
  - `AdminEventApiTest` 추가

## 검증 로그
- API
  - `./gradlew.bat test --no-daemon --tests "*AdminEventApiTest*"`
- Admin
  - `npm run build` (`apps/admin`)

## 운영 참고
- 로컬 API 실행 보조 스크립트
  - `scripts/start-wsl-postgres.ps1`
  - `scripts/run-api-local-wsl-db.ps1`
