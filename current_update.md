# Current Project Update (2026-02-19)

- 작성일: 2026-02-19
- 기준 브랜치: `develop`
- 기준 커밋: `273c3b4` (Merge PR #100)
- 직전 주요 반영:
  - PR #101: 모바일 환경 분리(local/staging/release) + 실행/빌드 스크립트 정비
  - PR #100: 운영용 사용자 계정 회원가입/로그인(`/auth/signup`, `/auth/login`) 구현

## 1) 현재 상태 요약

현재 상태는 "핵심 기능 구현 완료 + 운영 안정화/배포 품질 관리 단계"다.

- Backend(API)
  - Kakao 소셜 로그인, Admin ID/PW 로그인, 사용자 ID/PW 회원가입/로그인 구현
  - JWT access/refresh 토큰 발급/갱신, invite code 검증 플로우 동작
- Mobile
  - Kakao 로그인 + 계정 로그인/회원가입 UI/API 연동 완료
  - 찬양 목록/상세, MIDI 재생, 즐겨찾기/메모/히스토리, 오프라인 캐시 동작
  - `local/staging/release` 실행 프로파일 분리 완료
- Admin
  - 찬양/에셋/사용자/초대코드/이벤트 분석 화면 및 운영 경로 동작

## 2) 구현 범위(핵심)

### API (`apps/api`)

- 인증
  - `POST /auth/social`
  - `POST /auth/admin/login`
  - `POST /auth/signup`
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `POST /auth/invite/validate`
- 도메인/운영 API
  - 관리자: 찬양/에셋/사용자/초대코드/이벤트/CSV export
  - 사용자: 찬양/프로필/즐겨찾기/메모/히스토리/이벤트

### Mobile (`apps/mobile`)

- 로그인
  - Kakao 로그인
  - 계정 회원가입/로그인(운영 API 연동)
- 실행 환경
  - `apps/mobile/env/local.json`
  - `apps/mobile/env/staging.json`
  - `apps/mobile/env/release.json`
- 빌드/실행 스크립트
  - `scripts/run-mobile-emulator.ps1`
  - `scripts/build-mobile-apk.ps1`

## 3) 배포/운영 관점 현재 판단

- 로컬 개발/검증: Go
- 스테이징 운영 점검: Go (수동 스모크는 지속 필요)
- 프로덕션 공개 배포: Conditional Go
  - 필수 운영 체크리스트(시크릿/모니터링/배포 롤백 절차) 점검 후 진행 권장

## 4) 남은 우선순위 작업

### P0
1. 온보딩 데이터(교회/이름/구역) 서버 동기화 정책 확정
2. 로그인/회원가입 rate-limit 및 이상 징후 모니터링 강화
3. 스테이징 수동 스모크 결과를 정기적으로 누적 기록

### P1
1. 모바일 스토어 배포 리허설(Android Play/iOS TestFlight) 정례화
2. 운영 알림/지표(인증 실패율, 토큰 갱신 실패율, export 실패율) 대시보드 보강

## 5) 빠른 실행 명령

```powershell
# bootstrap
.\scripts\local-bootstrap.ps1

# local emulator
.\scripts\run-mobile-emulator.ps1 -Environment local -DeviceId emulator-5554

# staging debug apk
.\scripts\build-mobile-apk.ps1 -Environment staging -BuildMode debug -Install
```
