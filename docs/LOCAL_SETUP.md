# 로컬 개발 환경 설정 가이드

> Eunhye Hymn 프로젝트를 로컬에서 실행하기 위한 단계별 가이드입니다.

---

## 1. 사전 요구사항

| 도구 | 버전 | 확인 명령 |
|------|------|-----------|
| **Docker Desktop** | 28+ | `docker --version` |
| **Java** | 17 (JDK) | `java -version` |
| **Node.js** | 20+ | `node --version` |
| **npm** | 10+ | `npm --version` |
| **Git Bash** | - | Windows 기본 터미널 |

### Git Bash PATH 설정 (Windows)

Docker와 npm이 Git Bash에서 인식되지 않는 경우 `~/.bashrc`에 추가:

```bash
# ~/.bashrc
export PATH="$PATH:/c/Users/<사용자명>/AppData/Roaming/npm"
export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"
```

변경 후 `source ~/.bashrc` 또는 새 터미널 열기.

---

## 2. 환경변수 설정

프로젝트 루트에서 `.env` 파일 생성:

```bash
cd Eunhye_Hymn
cp .env.example .env
```

`.env` 파일은 기본 개발값이 채워져 있어 수정 없이 사용 가능합니다.

주요 변수:

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `JWT_SECRET` | `dev-secret-key-...` | JWT 서명 키 (**필수**) |
| `INVITE_CODE` | `dev-invite-code` | 초대코드 (**필수**) |
| `DB_USER` / `DB_PASS` | `postgres` / `postgres` | DB 인증 |
| `S3_BUCKET` | `local-bucket` | LocalStack S3 버킷 |
| `ADMIN_LOGIN_ID` / `ADMIN_LOGIN_PASSWORD` | 빈값 | Admin ID/PW 로그인(선택) |
| `SMS_TWILIO_ENABLED` | `false` | 로컬 SMS 실발송 여부 |
| `AI_GEMINI_ENABLED` | `false` | AI 추천 기능 활성화 여부 |
| `AI_GEMINI_API_KEY` | 빈값 | Gemini API 키 (활성화 시 필수) |

AI 추천 테스트 시에만 아래 값을 추가 설정하세요.
- `AI_GEMINI_ENABLED=true`
- `AI_GEMINI_API_KEY=<your_key>`

---

## 3. 실행 방법

### 방법 A: Docker Compose로 전체 실행 (권장)

모든 서비스를 컨테이너에서 실행합니다. 코드 변경 없이 전체 시스템을 확인할 때 적합합니다.

```bash
# 1. Docker Desktop 실행 (시스템 트레이에서 확인)

# 2. 전체 서비스 시작 (PostgreSQL + LocalStack + API)
docker compose -f infra/docker/docker-compose.yml up -d

# 3. 시작 확인 (모두 healthy/running 될 때까지 대기)
docker compose -f infra/docker/docker-compose.yml ps

# 4. API 헬스 체크
curl http://localhost:8080/api/v1/ping
# → {"ok":true}

# 5. Admin 프론트엔드 시작
cd apps/admin
npm install
npm run dev
# → http://localhost:5173 (API는 localhost:8080으로 프록시)
```

**접속 정보:**

| 서비스 | URL |
|--------|-----|
| API | http://localhost:8080/api/v1 |
| Admin | http://localhost:5173 |
| PostgreSQL | localhost:5432 (eunhye_hymn / postgres / postgres) |
| LocalStack S3 | http://localhost:4566 |

### 방법 B: DB만 Docker, API는 로컬 실행 (개발용)

API 코드를 수정하면서 핫 리로드로 개발할 때 적합합니다.

```bash
# 1. DB + S3만 시작
docker compose -f infra/docker/docker-compose.yml up -d postgres localstack init-s3

# 2. API 로컬 실행 (환경변수 직접 전달)
cd apps/api
DB_URL=jdbc:postgresql://localhost:5432/eunhye_hymn \
JWT_SECRET=dev-secret-key-change-in-production-min-32-chars!! \
JWT_ACCESS_TTL_SECONDS=3600 \
JWT_REFRESH_TTL_SECONDS=604800 \
INVITE_CODE=dev-invite-code \
S3_ENDPOINT=http://localhost:4566 \
S3_PUBLIC_BASE_URL=http://localhost:4566/local-bucket \
./gradlew bootRun

# 3. Admin 프론트엔드 (별도 터미널)
cd apps/admin
npm install
npm run dev
```

### 방법 C: 테스트만 실행 (Docker 불필요)

외부 의존성 없이 H2 인메모리 DB로 테스트만 돌립니다.

```bash
# 백엔드 테스트
cd apps/api
./gradlew test --no-daemon --stacktrace

# 프론트엔드 타입체크 + 빌드
cd apps/admin
npm install
npx tsc --noEmit
npm run build
```

### 방법 D: Docker가 안 될 때 (WSL PostgreSQL 우회)

Docker Desktop/WSL2 엔진이 올라오지 않아도, **WSL(Ubuntu) + PostgreSQL**로 API를 실행할 수 있습니다.

```powershell
# 1. WSL PostgreSQL 준비 (서비스 시작 + DB 생성/보정)
.\scripts\start-wsl-postgres.ps1

# 2. API 실행 (DB/JWT/초대코드 환경변수 자동 세팅)
.\scripts\run-api-local-wsl-db.ps1

# 3. 헬스 체크 (별도 터미널)
curl http://localhost:8080/api/v1/ping
```

기본값:
- DB: `localhost:5432`, DB명 `eunhye_hymn`, 계정 `postgres/postgres`
- JWT/초대코드: 개발용 기본값 자동 적용

---

## 4. Admin 로그인 방법

로컬에서는 **Dev Login**을 사용합니다.

1. http://localhost:5173 접속
2. 로그인 페이지 하단의 **Dev Login** 섹션 펼치기
3. 기본값 그대로 (ADMIN 역할) **Login** 클릭
4. 대시보드 진입

초대코드가 필요한 경우: `.env`의 `INVITE_CODE` 값 사용 (기본: `dev-invite-code`)

Admin ID/PW 로그인을 테스트하려면 `.env`에 `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`를 설정한 뒤
`POST /api/v1/auth/admin/login` 또는 관리자 로그인 화면의 ID/PW 모드를 사용합니다.

---

## 5. 서비스 종료

```bash
# 컨테이너 중지 (데이터 유지)
docker compose -f infra/docker/docker-compose.yml down

# 컨테이너 + 데이터 볼륨 완전 삭제
docker compose -f infra/docker/docker-compose.yml down -v
```

---

## 6. 트러블슈팅

### Docker Desktop이 Git Bash에서 인식되지 않음

```bash
export PATH="$PATH:/c/Program Files/Docker/Docker/resources/bin"
```

### API가 부팅에 실패함

`JWT_SECRET`과 `INVITE_CODE`가 설정되었는지 확인:
```bash
# .env 파일 확인
cat .env | grep -E "(JWT_SECRET|INVITE_CODE)"
```

AI 추천을 활성화했다면 `AI_GEMINI_API_KEY` 누락 여부도 확인:
```bash
cat .env | grep -E "(AI_GEMINI_ENABLED|AI_GEMINI_API_KEY)"
```

### 포트 충돌 (5432, 8080, 5173)

기존에 실행 중인 PostgreSQL/Tomcat/Node 서버가 있으면 먼저 종료:
```bash
# Windows: 포트 사용 중인 프로세스 확인
netstat -ano | findstr :5432
netstat -ano | findstr :8080
```

### Gradle 빌드 실패 (JAVA_HOME 경로에 공백)

`gradlew.bat`에 이미 수정이 적용되어 있습니다. 여전히 문제가 있으면:
```bash
# JAVA_HOME 확인
echo $JAVA_HOME
# 공백이 포함된 경로인 경우 따옴표로 감싸기
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-17.0.x-hotspot"
```

### LocalStack S3 버킷 생성 실패

`init-s3` 컨테이너 로그 확인:
```bash
docker compose -f infra/docker/docker-compose.yml logs init-s3
```

### Docker Desktop이 `unable to start` / WSL2 `0x8037011e`로 실패

- `vmcompute`, `vmms`가 반복적으로 종료되면 Docker Linux 엔진이 올라오지 않습니다.
- 즉시 개발을 진행하려면 위의 **방법 D(WSL PostgreSQL 우회)** 를 사용하세요.
- 근본 해결은 BIOS/가상화/재부팅 등 호스트 설정이 필요할 수 있습니다.

### Flutter Android 라이선스 미승인

`flutter doctor`에서 Android 라이선스 경고가 보이면 인터랙티브 터미널에서:
```powershell
.\scripts\flutterw.ps1 doctor --android-licenses
```

---

## 7. 유용한 명령어

```bash
# 전체 로그 실시간 확인
docker compose -f infra/docker/docker-compose.yml logs -f

# API 로그만 확인
docker compose -f infra/docker/docker-compose.yml logs -f api

# 컨테이너 재시작
docker compose -f infra/docker/docker-compose.yml restart api

# DB 직접 접속
docker exec -it eunhye-postgres psql -U postgres -d eunhye_hymn

# S3 버킷 내용 확인
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  aws --endpoint-url=http://localhost:4566 s3 ls s3://local-bucket/
```
