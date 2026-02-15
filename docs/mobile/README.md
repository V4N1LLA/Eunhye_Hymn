# 모바일 앱 문서

## 1. 현재 상태 (MVP)

`apps/mobile`에 Flutter MVP가 추가되었다.

구현 기능:

- 로그인
  - 소셜 SDK 직접 로그인 (Kakao 모바일)
  - 로그인 화면에서 Kakao 버튼 클릭 시 provider 앱/브라우저로 리디렉션
  - 초대코드 입력 지원 (최초 1회)
  - Dev 로그인은 `ENABLE_DEV_LOGIN=true`일 때만 노출
- 찬양
  - 목록 조회 + 검색
  - 목록 화면 태그 필터 + 검색어/필터 초기화 + 빈 상태 가이드
  - 상세 조회
  - PNG 에셋 이미지 표시 (한 곡 다중 페이지 지원)
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
  - `POST /auth/dev/login`
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

# 웹 실행
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1

# Android 에뮬레이터/실기기 (staging)
..\..\scripts\flutterw.ps1 run -d emulator-5554 --dart-define=API_BASE_URL=http://13.209.200.12

# 소셜 로그인 포함 실행 (권장)
..\..\scripts\flutterw.ps1 run -d emulator-5554 `
  --dart-define=API_BASE_URL=http://13.209.200.12 `
  --dart-define=KAKAO_NATIVE_APP_KEY=<kakao_native_app_key>
```

실기기에서는 `10.0.2.2` 대신 로컬 서버 IP를 사용한다.
`API_BASE_URL`에 `/api/v1`를 생략해도 앱에서 자동으로 보정한다.
Android Kakao 콜백 스킴은 `kakao<KAKAO_NATIVE_APP_KEY>`이므로,
앱 실행 시 `--dart-define=KAKAO_NATIVE_APP_KEY=...` 값이 누락/불일치하면
동의 화면의 "계속하기" 이후 앱으로 복귀하지 않을 수 있다.
로컬 개발용 로그인 화면이 필요하면 `--dart-define=ENABLE_DEV_LOGIN=true`를 함께 사용한다.

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

## 7. 운영 연계 체크포인트

- 스테이징 스모크 테스트는 `docs/staging-smoke-checklist.md` 기준으로 수행한다.
- 모바일 결과(로그인/재생/동기화/Kakao fallback)는 `docs/runbook.md` 배포 기록과 함께 남긴다.
- CI(`flutter analyze`, `flutter test`) 결과를 배포 승인 근거로 포함한다.

