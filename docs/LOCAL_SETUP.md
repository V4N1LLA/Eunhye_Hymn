# 로컬 개발 환경 설정 가이드

> 기준일: 2026-02-19 (`develop` 최신 흐름)

## 1. 사전 요구사항

| 도구 | 권장 버전 | 확인 명령 |
|------|-----------|-----------|
| Docker Desktop | 최신 | `docker --version` |
| Java (JDK) | 17 | `java -version` |
| Node.js | 20+ | `node --version` |
| npm | 10+ | `npm --version` |
| Git | 최신 | `git --version` |
| Android Studio + Emulator | 최신 | (GUI) |

## 2. 가장 빠른 시작(권장)

```powershell
# 저장소 루트
.\scripts\local-bootstrap.ps1
```

이 스크립트가 자동으로 수행하는 일:

- `.env`, `apps/mobile/.env` 템플릿 생성
- 필요한 로컬 기본값 보정
- Docker 기반 API/DB 스모크 검증(`local-verify.ps1`) 실행
- 테스트 데이터 요약 출력(`.tmp/local-smoke-summary.json`)

## 3. 수동 설정

### 3.1 env 파일

```powershell
Copy-Item .env.example .env
Copy-Item apps/mobile/.env.example apps/mobile/.env
```

필수 점검 값:

- `.env`
  - `JWT_SECRET`
  - `INVITE_CODE`
  - `ADMIN_LOGIN_ID`
  - `ADMIN_LOGIN_PASSWORD`
- `apps/mobile/.env`
  - `API_BASE_URL=http://10.0.2.2:8080/api/v1`
  - `KAKAO_NATIVE_APP_KEY=<your_key>`

### 3.2 Docker stack 기동

```powershell
docker compose --env-file .env -f infra/docker/docker-compose.yml up -d
curl http://localhost:8080/api/v1/ping
```

### 3.3 로컬 스모크 검증

```powershell
.\scripts\local-verify.ps1
# 이미 stack이 떠 있으면
.\scripts\local-verify.ps1 -SkipDockerUp
```

검증 항목:

- `/ping`
- admin 로그인
- 초대코드 생성/검증(대소문자/공백 정규화)
- 찬양/에셋 생성/업로드/확정
- DB 사용자 생성 + `/auth/dev/login`

## 4. 앱 실행

### 4.1 Admin 웹

```powershell
cd apps/admin
npm install
npm run dev
```

- URL: `http://localhost:5173`
- 로그인: `.env`의 `ADMIN_LOGIN_ID` / `ADMIN_LOGIN_PASSWORD`

### 4.2 Mobile (Android Emulator)

```powershell
# 저장소 루트
.\scripts\run-mobile-emulator.ps1 -DeviceId emulator-5554
```

앱 테스트 시:

- 초대코드: `local-verify.ps1` 출력값 또는 `.tmp/local-smoke-summary.json` 값 사용
- 계정 로그인 경로 검증: `local-verify.ps1`가 생성한 `DB User ID` 사용

## 5. 서비스 종료

```powershell
# 컨테이너만 종료 (데이터 유지)
docker compose --env-file .env -f infra/docker/docker-compose.yml down

# 컨테이너 + 데이터 볼륨 삭제
docker compose --env-file .env -f infra/docker/docker-compose.yml down -v
```

## 6. 트러블슈팅

### 6.1 API 부팅 실패

- `.env`의 `JWT_SECRET`, `INVITE_CODE` 확인
- Docker 로그 확인:

```powershell
docker compose --env-file .env -f infra/docker/docker-compose.yml logs api
```

### 6.2 에셋 URL 접근 실패(에뮬레이터)

`.env`에서 아래 값 사용 후 API 재시작:

```env
S3_PUBLIC_BASE_URL=http://10.0.2.2:4566/local-bucket
```

### 6.3 Flutter/Android 라이선스

```powershell
.\scripts\flutterw.ps1 doctor --android-licenses
```

## 7. 참고 문서

- `docs/TEAM_LOCAL_DEVELOPMENT.md`
- `docs/mobile/README.md`
- `docs/runbook.md`
