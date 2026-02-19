# 현재 사용 가능 범위 정리

- 작성일: 2026-02-19
- 기준 브랜치: `develop`
- 기준 커밋: `e877f2f` (2026-02-18, PR #98 merge)
- 근거 문서: `current_update.md`, `README.md`, `CLAUDE.md`
- 근거 PR(최근): #98, #97, #95, #94, #93, #92, #91, #90, #89, #88, #87, #56, #55, #54, #53, #52, #38, #37, #36, #31

## 1. 요약

현재 시점 기준 상태는 다음과 같다.

- 로컬 기능: 관리자 웹 + 백엔드 API + 모바일 앱 핵심 기능 사용 가능
- 로컬 검증 자동화: `local-bootstrap.ps1`, `local-verify.ps1`, `run-mobile-emulator.ps1`로 새 PC 재현 가능
- 스테이징 배포: 자동 배포 파이프라인 동작 중
- 모바일 릴리즈 준비: Android signed AAB / iOS TestFlight 경로를 워크플로우로 준비 완료
- 출시 단계: "기능 구현 완료" 단계이며, 운영 안정화/실 배포 전환 작업이 남아 있음

## 2. 지금 바로 검증 가능한 범위 (로컬)

### 2.1 관리자 웹 (`apps/admin`)

- 로그인
  - 운영자 ID/PW 로그인
  - localhost 전용 Dev 로그인 보조
- 찬양 관리: 목록/생성/수정/삭제, 검색/필터, 활성화 토글
- 에셋 관리: Presign -> 업로드 -> Confirm, 삭제
- 사용자 관리: 생성/조회/수정/soft-delete
- 초대코드 관리: 생성/조회/비활성화
- 감사 로그: 필터/집계/페이지네이션, 동기/비동기 CSV, 비동기 지표 조회

핵심 경로:
- `/`
- `/hymns`, `/hymns/new`, `/hymns/:id/edit`
- `/assets/upload`
- `/users`
- `/invite-codes`
- `/events`

### 2.2 백엔드 API (`apps/api`)

- 인증/인가
  - `/auth/social`, `/auth/admin/login`, `/auth/refresh`, `/auth/logout`, `/auth/invite/validate`, `/auth/dev/login`
- 도메인
  - 찬양 공개 조회 + 관리자 CRUD
  - 에셋 presign/confirm/delete
  - 사용자 CRUD
  - 초대코드 생성/조회/비활성화
  - 멤버 개인화(즐겨찾기/메모/히스토리) + 이벤트 기록
- 운영
  - 감사 로그 조회/CSV(동기)
  - 대용량 CSV 비동기 export + 상태 조회/다운로드
  - export 작업 운영 지표 API

최근 반영:
- 초대코드 정규화(공백 제거/대문자 변환) 및 레거시 소문자 코드 호환 처리 강화

### 2.3 모바일 앱 (`apps/mobile`)

- 로그인
  - Kakao SDK 로그인 + 초대코드 입력
  - 로컬 검증용 계정 로그인 경로(현재 UUID 기반 dev-login 검증용)
- 온보딩
  - 첫 로그인 시 교회/이름/구역 입력
  - 로컬 저장(SharedPreferences)
- 찬양
  - 목록/검색/필터
  - 상세(PNG 다중 페이지, MIDI 재생/일시정지/정지/속도)
- 개인화
  - 즐겨찾기/메모/히스토리
- 오프라인
  - 캐시 fallback + 재접속 시 동기화

## 3. 조건부로 검증 가능한 범위 (스테이징)

구성 완료:

- `deploy-staging.yml`: API test + Admin build + 이미지 push + EC2 배포 + verify
- `mobile-ci.yml`: analyze/test
- `mobile-release-check.yml`: Android release APK 빌드 검증
- `mobile-store-release.yml`: Android Play upload 경로 + iOS TestFlight 경로(수동)

최신 실행 상태(2026-02-19 확인):

- [x] Staging deploy 성공: run `22161011643` (PR #98 merge)
- [x] Mobile release check 성공: run `22158976340` (PR #97 merge)
- [x] API CI 성공: run `22161011631`
- [x] Mobile CI 성공: run `22158976375`
- [ ] Mobile store release workflow의 실제 publish 실행 이력 없음(준비 상태)
- [ ] Admin/Mobile 실기기 수동 스모크 정례 기록 강화 필요

## 4. 미완료 범위 (우선순위)

### P0

- 모바일 회원가입/계정 로그인 실서비스 경로 구현
  - 현재 회원가입 화면은 안내 수준
  - 모바일 ID/PW 로그인은 dev-login 검증 경로 의존
- 온보딩 서버 저장/동기화 정책 확정 및 구현
- 스테이징 수동 스모크 정례화(로그 누적)

### P1

- Android `play_upload`, iOS `testflight` 모드 운영 리허설 1회 이상 수행
- 비동기 export 지표 임계치 기반 운영 대응 절차 고도화

### P2

- 프로덕션 도메인/HTTPS 기준 모바일 네트워크 정책 확정
- QA 자동화 확장(E2E/회귀)

## 5. 빠른 검증 체크리스트

```powershell
# 1) 로컬 부트스트랩
.\scripts\local-bootstrap.ps1

# 2) 검증만 재실행
.\scripts\local-verify.ps1 -SkipDockerUp

# 3) 모바일 실행
.\scripts\run-mobile-emulator.ps1 -DeviceId emulator-5554
```
