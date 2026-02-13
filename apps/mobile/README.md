# Eunhye Hymn Mobile (Flutter MVP)

교회 멤버용 모바일 앱 MVP입니다.

## 구현 범위

- 로그인
  - 소셜 로그인 API 토큰 입력 방식 (Google/Kakao)
  - Dev 로그인 (개발 환경)
- 찬양
  - 목록 조회 및 검색
  - 상세 조회 (에셋 표시)
- 개인화
  - 즐겨찾기 토글
  - 메모 조회/저장
  - 최근 열람 히스토리 조회
- 인증
  - Access/Refresh 토큰 로컬 저장
  - 401 발생 시 `/auth/refresh` 자동 재시도

## 실행

저장소 루트의 `scripts/flutterw.ps1`가 Flutter SDK 설치/실행을 자동 처리합니다.

```powershell
# 저장소 루트에서 최초 1회 (Flutter SDK 자동 설치 + 버전 확인)
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

기본 `API_BASE_URL`:

- Android Emulator: `http://10.0.2.2:8080/api/v1`

실기기 사용 시 로컬 PC IP로 변경:

```powershell
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://<PC_IP>:8080/api/v1
```

## 구조

```
lib/
  main.dart
  src/
    app.dart
    core/
      config/app_config.dart
      network/api_client.dart
      network/api_exception.dart
      storage/token_storage.dart
    features/
      auth/
      hymn/
      history/
```

## 주의사항

- API Base path는 반드시 `/api/v1`를 포함해야 합니다.
- Dev 로그인은 백엔드 `dev` 프로필에서만 동작합니다.
