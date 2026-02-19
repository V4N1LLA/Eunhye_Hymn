# 모바일 앱 문서

## 1. 현재 상태 (MVP)

`apps/mobile`에 Flutter MVP가 추가되었다.

구현 기능:

- 로그인
  - 소셜 SDK 직접 로그인 (Kakao 모바일)
  - 로그인 화면에서 Kakao 버튼 클릭 시 provider 앱/브라우저로 리디렉션
  - 초대코드 입력 지원 (최초 1회)
  - 계정 회원가입/로그인(`POST /auth/signup`, `POST /auth/login`)
- 찬양
  - 목록 조회 + 검색
  - 목록 화면 태그 필터 + 검색어/필터 초기화 + 빈 상태 가이드
  - 상세 조회
  - PNG 에셋 이미지 표시 (한 곡 다중 페이지 지원)
  - 로컬 악보 파일 디코딩 실패 시 원본 URL로 자동 fallback
  - MIDI 에셋 앱 내 재생 UX (재생/일시정지/정지/속도)
- 개인화
  - 즐겨찾기 토글
  - 메모 조회/저장
  - 최근 열람 히스토리 조회 (검색 + 기간 필터 + 빈 상태 가이드)
- 오프라인
  - 목록/상세/메모/즐겨찾기/히스토리 로컬 캐시 fallback
  - 오프라인 변경(메모 저장, 즐겨찾기 토글) 재접속 시 자동 동기화
- 인증
  - Access/Refresh 토큰 저장
  - 401 시 `/auth/refresh` 자동 토큰 갱신 후 재시도

## 2. 화면 인벤토리

1. 로그인 화면
2. 찬양 목록 화면
3. 찬양 상세 화면
4. 최근 열람 히스토리 화면

## 3. 네비게이션

- 로그인 성공 후 Home 진입
- Home 하단 탭:
  - 찬양 목록
  - 최근 열람
- 목록/히스토리에서 상세 화면으로 이동

## 4. API 연동 기준

- Base URL: `/api/v1`
- 사용 API:
  - `POST /auth/social`
  - `POST /auth/signup`
  - `POST /auth/login`
  - `POST /auth/refresh`
  - `POST /auth/logout`
  - `GET /me/profile`
  - `GET /me/favorites/{hymnId}`
  - `GET /hymns`
  - `GET /hymns/{id}`
  - `POST /me/favorites/{hymnId}`
  - `GET /me/hymns/{hymnId}/note`
  - `PUT /me/hymns/{hymnId}/note`
  - `GET /me/history`

## 5. 실행

```powershell
# 저장소 루트에서 최초 1회 (Flutter SDK 자동 설치 + 버전 확인)
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get

# 환경 프로파일(local/staging/release) 기반 실행
cd ..\..
.\scripts\run-mobile-emulator.ps1 -Environment local -DeviceId emulator-5554
.\scripts\run-mobile-emulator.ps1 -Environment staging -DeviceId emulator-5554
```

실기기에서는 `10.0.2.2` 대신 로컬 서버 IP를 사용한다.
`API_BASE_URL`에 `/api/v1`를 생략해도 앱에서 자동으로 보정한다.
Android Kakao 콜백 스킴은 `kakao<KAKAO_NATIVE_APP_KEY>`이므로,
`run-mobile-emulator.ps1`가 읽는 `apps/mobile/.env`(fallback: 루트 `.env`)의 키 값이 누락/불일치하면
동의 화면의 "계속하기" 이후 앱으로 복귀하지 않을 수 있다.
민감 정보 관리 원칙은 `docs/SECRETS_MANAGEMENT.md`를 따른다.
계정 로그인/회원가입은 운영 API(`/auth/login`, `/auth/signup`)를 사용한다.

## 5.1 배포 범위 주의

- 현재 저장소에는 네이티브 프로젝트 디렉토리(`android/`, `ios/`)가 포함되어 있다.
- Android debug는 `android/app/src/debug/AndroidManifest.xml`에서 cleartext(`http`)를 허용한다.
- iOS는 `ios/Runner/Info.plist`에 스테이징 호스트(`13.209.200.12`) ATS 예외가 포함되어 있다.
- 운영/스토어 배포 전에는 HTTPS 도메인 기준으로 ATS/cleartext 설정을 재검토해야 한다.

## 6. CI

- 워크플로우: `.github/workflows/mobile-ci.yml`
- 트리거: PR 및 `develop` push (경로: `apps/mobile/**`)
- 실행 단계:
  - `flutter pub get`
  - `flutter analyze`
  - `flutter test`

## 6.1 Release 패키징 준비

- 워크플로우: `.github/workflows/mobile-release-check.yml`
- 트리거:
  - PR 및 `develop` push (`apps/mobile/**` 변경 시)
  - 수동 실행(`workflow_dispatch`)
- 목적:
  - Android `release` APK 빌드가 깨지지 않는지 사전 검증
  - 빌드 산출물(`app-release.apk`)을 Actions artifact로 업로드

수동 실행 시 `api_base_url` 입력을 제공하면 해당 값으로 `--dart-define=API_BASE_URL`를 주입한다.

로컬에서 동일 검증을 수행하려면 Android SDK가 설치되어 있어야 한다.
예시:

```powershell
# staging debug APK + install
.\scripts\build-mobile-apk.ps1 -Environment staging -BuildMode debug -Install

# release APK (운영 URL 명시 필요)
.\scripts\build-mobile-apk.ps1 -Environment release -BuildMode release -ApiBaseUrl https://<prod-domain>/api/v1
```

## 6.2 Store release readiness

- 워크플로우: `.github/workflows/mobile-store-release.yml`
- 트리거: 수동 실행(`workflow_dispatch`)
- 현재 운영 원칙(2026-02-18):
  - 스테이징 검증만 실행하고 운영 배포/퍼블리시는 실행하지 않는다.
  - 운영 배포는 승인 즉시 실행할 수 있도록 시크릿/입력값/절차만 준비한다.
- 입력:
  - `target`: `android` | `ios` | `both`
  - `android_distribution_mode`: `build_only` | `play_upload`
  - `android_package_name`: Android package name 1회 오버라이드(미입력 시 `GOOGLE_PLAY_PACKAGE_NAME` 사용)
  - `android_track`: `internal` | `alpha` | `beta` | `production` (play upload 시)
  - `android_release_status`: `draft` | `completed` | `inProgress` | `halted` (play upload 시)
  - `android_changes_not_sent_for_review`: `true` | `false` (play upload 시)
  - `api_base_url`: 릴리즈 빌드 시 주입할 API URL
- Android 경로:
  - 서명형 AAB 빌드(`flutter build appbundle --release`)
  - `android_distribution_mode=play_upload`면 AAB 업로드 후 Google Play에 배포 편집 생성
  - 필요한 Secrets:
    - `MOBILE_ANDROID_KEYSTORE_BASE64`
    - `MOBILE_ANDROID_KEY_ALIAS`
    - `MOBILE_ANDROID_KEY_PASSWORD`
    - `MOBILE_ANDROID_STORE_PASSWORD`
    - `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` (play upload 시)
    - `GOOGLE_PLAY_PACKAGE_NAME` (입력 미지정 시)
- Android 서명 설정:
  - `apps/mobile/android/key.properties.example`를 기준으로 `apps/mobile/android/key.properties` 구성
  - `key.properties`가 없으면 로컬 릴리즈 체크는 debug signing fallback을 사용

## 7. 운영 연계 체크포인트

- 최소 설치 QA 가이드는 `docs/mobile/qa-minimal-tooling.md`를 기준으로 수행한다.
- 스테이징 스모크 테스트는 `docs/staging-smoke-checklist.md` 기준으로 수행한다.
- 운영 사이클 자동 점검은 `scripts/staging-ops-cycle.ps1`를 사용하고 결과를 `docs/staging-smoke-log.md`에 누적한다.
- 모바일 결과(로그인/재생/동기화/Kakao fallback)는 `docs/runbook.md` 배포 기록과 함께 남긴다.
- CI(`flutter analyze`, `flutter test`) 결과를 배포 승인 근거로 포함한다.

