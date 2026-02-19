# 모바일 앱 문서

## 1. 현재 상태 (2026-02-19)

`apps/mobile` Flutter 앱은 MVP 기능 + 로컬 검증 플로우를 포함한다.

구현 범위:

- 로그인
  - Kakao SDK 로그인 + 초대코드 입력
  - 로컬 검증용 계정 로그인 경로(현재 UUID 기반 dev-login 검증)
- 온보딩
  - 첫 로그인 시 교회/이름/구역 입력 화면
  - 입력값 로컬 저장(SharedPreferences)
- 찬양
  - 목록 조회/검색/필터
  - 상세 조회
  - PNG 에셋 다중 페이지 표시
  - MIDI 재생/일시정지/정지/속도
- 개인화
  - 즐겨찾기 토글
  - 메모 조회/저장
  - 최근 열람 히스토리
- 오프라인
  - 목록/상세/메모/즐겨찾기/히스토리 캐시 fallback
  - 오프라인 변경사항 재접속 시 동기화
- 인증
  - Access/Refresh 토큰 저장
  - 401 발생 시 `/auth/refresh` 자동 재시도

## 2. 화면 인벤토리

1. 로그인 화면 (`login_page.dart`)
2. 회원가입 화면(안내 수준, API 미연동) (`sign_up_page.dart`)
3. 온보딩 화면 (`onboarding_page.dart`)
4. 찬양 목록 화면 (`hymn_list_page.dart`)
5. 찬양 상세 화면 (`hymn_detail_page.dart`)
6. 히스토리 화면 (`history_page.dart`)

## 3. API 연동 기준

- Base URL: `/api/v1`
- 사용 API
  - `POST /auth/social`
  - `POST /auth/dev/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `GET /me/profile`
  - `GET /hymns`
  - `GET /hymns/{id}`
  - `GET /me/favorites/{hymnId}`
  - `POST /me/favorites/{hymnId}`
  - `GET /me/hymns/{hymnId}/note`
  - `PUT /me/hymns/{hymnId}/note`
  - `GET /me/history`

## 4. 로컬 실행

### 4.1 권장: 재현성 자동 플로우

```powershell
# 저장소 루트
.\scripts\local-bootstrap.ps1
.\scripts\run-mobile-emulator.ps1 -DeviceId emulator-5554
```

### 4.2 수동 실행

```powershell
# 저장소 루트에서 최초 1회
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get

# 웹
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1

# Android 에뮬레이터
..\..\scripts\flutterw.ps1 run -d emulator-5554 `
  --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1 `
  --dart-define=KAKAO_NATIVE_APP_KEY=<kakao_native_app_key>
```

## 5. CI

- 워크플로우: `.github/workflows/mobile-ci.yml`
- 트리거: PR + `develop` push (`apps/mobile/**`)
- 실행: `flutter pub get`, `flutter analyze`, `flutter test`

## 6. 릴리즈 준비도

### 6.1 Android release check

- 워크플로우: `.github/workflows/mobile-release-check.yml`
- 목적: Android release APK 빌드 검증 + artifact 업로드

### 6.2 Mobile store release readiness

- 워크플로우: `.github/workflows/mobile-store-release.yml`
- 수동 실행 입력
  - 공통: `target`, `api_base_url`
  - Android: `android_distribution_mode` (`build_only`, `play_upload`) + track/status 옵션
  - iOS: `ios_distribution_mode` (`build_only`, `testflight`) + bundle id/notes 옵션
- 현재 운영 원칙
  - 스테이징 검증 우선
  - production publish는 승인 후 실행

## 7. 운영 연계 체크포인트

- 최소 설치 QA 가이드: `docs/mobile/qa-minimal-tooling.md`
- 스테이징 스모크 체크: `docs/staging-smoke-checklist.md`
- 스테이징 운영 로그: `docs/staging-smoke-log.md`
- 팀 재현 가이드: `docs/TEAM_LOCAL_DEVELOPMENT.md`

## 8. 미완료 항목

- 회원가입 API 연동
- 모바일 ID/PW 실서비스 로그인 연동
- 온보딩 서버 저장/동기화 정책 확정
