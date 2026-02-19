# Current Project Update (2026-02-19)

- 작성일: 2026-02-19
- 기준 브랜치: `develop`
- 기준 커밋: `e877f2f` (Merge PR #98)
- 직전 기준 대비 핵심 변경: PR #97(local verification flow), PR #98(invite-code 대소문자 호환)

## 1. 현재 개발 상태 요약

프로젝트는 로컬 개발/검증, 스테이징 자동 배포, 관리자/모바일 핵심 기능까지 구현되어 있다.
현재 상태는 "기능 구현 완료 + 운영 안정화/출시 전환 준비 단계"에 가깝다.

- 백엔드: 관리자/멤버 API, 이벤트 로그 분석/비동기 CSV export, JWT/refresh, Kakao 소셜 로그인, Admin ID/PW 로그인 구현 완료
- 관리자 웹: 찬양/에셋/사용자/초대코드/이벤트 분석 화면 구현 완료
- 모바일 앱: 로그인, 찬양 목록/상세, MIDI 재생, 즐겨찾기/메모/히스토리, 오프라인 캐시/동기화 구현 완료
- 로컬 운영 자동화: `local-bootstrap.ps1`, `local-verify.ps1`, `run-mobile-emulator.ps1` 추가로 새 PC 재현성 강화
- 스테이징 배포: `deploy-staging.yml` 기준 자동 배포 동작 중

## 2. 어디까지 개발되었는지 (구현 범위)

### 2.1 API (`apps/api`)

구현 완료 범위:

- 인증/인가
  - `POST /auth/social` (Kakao)
  - `POST /auth/admin/login` (운영자 ID/PW)
  - `POST /auth/refresh`, `POST /auth/logout`
  - `POST /auth/invite/validate`
  - `POST /auth/dev/login` (개발용)
- 찬양/에셋/사용자/초대코드 관리자 API
  - `/admin/hymns`, `/admin/assets`, `/admin/users`, `/admin/invite-codes`
- 감사 로그/분석
  - `GET /admin/events`
  - `GET /admin/events/export`
  - `POST /admin/events/export-jobs`
  - `GET /admin/events/export-jobs/{jobId}`
  - `GET /admin/events/export-jobs/{jobId}/download`
  - `GET /admin/events/export-jobs/metrics`
- 멤버 API
  - `/hymns`, `/me/profile`, `/me/favorites`, `/me/hymns/{id}/note`, `/me/history`, `/events`

최신 보강 사항:

- 초대코드 입력 대소문자/공백 정규화 일관 처리
- 레거시 소문자 코드와의 호환 조회/사용 카운트 증가 처리 보강
- 관련 API 테스트 보강 (`AdminInviteCodeApiTest`, `SocialLoginApiTest`)

### 2.2 Admin (`apps/admin`)

구현 완료 범위:

- 라우트: `/`, `/hymns`, `/hymns/new`, `/hymns/:id/edit`, `/assets/upload`, `/users`, `/invite-codes`, `/events`
- 로그인: 운영자 ID/PW 로그인 기본, localhost에서만 Dev 로그인 폴백
- 운영 기능: 이벤트 필터/집계/CSV(동기/비동기) 확인 가능

### 2.3 Mobile (`apps/mobile`)

구현 완료 범위:

- 로그인
  - Kakao SDK 로그인
  - 초대코드 입력
  - 로컬 검증용 계정 로그인(UI에서 ID/PW 입력, 현재는 UUID 기반 dev-login 검증 경로)
- 온보딩
  - 첫 로그인 후 교회/이름/구역 입력 화면
  - 온보딩 데이터는 로컬 저장(SharedPreferences)
- 찬양 기능
  - 목록/검색/필터
  - 상세(PNG 다중 페이지, MIDI 재생/일시정지/정지/속도)
- 개인화
  - 즐겨찾기, 메모, 히스토리
- 오프라인
  - 캐시 fallback + 재접속 동기화 큐

## 3. 배포/검증 상태

최신 GitHub Actions 확인(2026-02-19 기준):

- Staging deploy: 성공
  - run `22161011643` (event: push to `develop`, title: PR #98 merge)
- Mobile release check(APK): 성공
  - run `22158976340` (event: push to `develop`, title: PR #97 merge)
- Mobile CI: 성공
  - run `22158976375`
- API CI: 성공
  - run `22161011631`

판단:

- 로컬 개발/검증: Go
- 스테이징 운영 점검: Conditional Go
- 프로덕션 공개 배포: No-Go
- 앱스토어 실제 배포: No-Go (워크플로우 준비 완료, 실제 publish 이력 없음)

## 4. 앞으로 해야 할 작업 (우선순위)

### P0 (출시 전 필수)

1. 모바일 계정 로그인/회원가입 경로 실제화
- 현재 `회원가입`은 안내 화면 수준이며 서버 회원가입 API가 없다.
- 모바일 ID/PW 로그인도 실제 계정 인증이 아니라 dev-login 검증 경로다.

2. 온보딩 서버 연동 여부 결정
- 현재 온보딩(교회/이름/구역)은 로컬 저장만 수행한다.
- 서버 프로필 저장/동기화 정책을 확정해야 다기기 일관성이 생긴다.

3. 스테이징 수동 스모크 정례화
- Admin/Mobile 실기기 점검 결과를 `docs/staging-smoke-log.md`에 주기적으로 누적해야 한다.

### P1 (운영 안정화)

1. Mobile store release workflow 실전 리허설
- Android `play_upload`, iOS `testflight` 모드 각각 1회 dry-run/검증
- 시크릿 검증 및 실패 케이스(runbook) 보완

2. 운영 지표/알림 고도화
- 비동기 export 지표 기반 임계치 알림을 실제 운영 대응 절차와 연결

3. 로컬 자동검증 확대
- `local-verify.ps1` 결과를 CI에서 재사용할 수 있는 경량 검증 단계 도입 검토

### P2 (확장)

1. 프로덕션 도메인/HTTPS 기준 모바일 네트워크 정책 정리
2. QA 자동화(E2E/스냅샷) 추가
3. 운영자 대시보드 지표 확장

## 5. 빠른 검증 명령

```powershell
# 새 PC/재현성 기준 권장
.\scripts\local-bootstrap.ps1

# 스택 기동 후 검증만
.\scripts\local-verify.ps1 -SkipDockerUp

# 모바일 실행
.\scripts\run-mobile-emulator.ps1 -DeviceId emulator-5554
```
