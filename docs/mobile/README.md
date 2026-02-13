# 모바일 앱 문서

## 1. 현재 상태 (MVP)

`apps/mobile`에 Flutter MVP가 추가되었다.

구현 기능:

- 로그인
  - 소셜 SDK 직접 로그인 (Google, Kakao 모바일)
  - Kakao 웹/미지원 플랫폼은 토큰 입력 fallback
  - Dev 로그인 (개발 환경)
- 찬양
  - 목록 조회 + 검색
  - 상세 조회
  - PNG 에셋 이미지 표시
  - MIDI 에셋 앱 내 재생 UX (재생/일시정지/정지/속도)
- 개인화
  - 즐겨찾기 토글
  - 메모 조회/저장
  - 최근 열람 히스토리 조회
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
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

실기기에서는 `10.0.2.2` 대신 로컬 서버 IP를 사용한다.

## 6. CI

- 워크플로우: `.github/workflows/mobile-ci.yml`
- 트리거: PR 및 `develop` push (경로: `apps/mobile/**`)
- 실행 단계:
  - `flutter pub get`
  - `flutter analyze`
  - `flutter test`

## 7. 다음 고도화 항목

- 오프라인 캐시/동기화
