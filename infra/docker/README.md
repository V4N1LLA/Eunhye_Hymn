# Docker 로컬 개발 환경

Docker Compose를 사용한 Eunhye Hymn 로컬 개발 환경입니다.

## 구성 서비스

| 서비스 | 설명 | 포트 |
|--------|------|------|
| **postgres** | PostgreSQL 15 (Alpine) | 5432 |
| **localstack** | LocalStack — S3 에뮬레이션 | 4566 |
| **init-s3** | S3 버킷 초기화 (일회성) | - |
| **api** | Spring Boot API 서버 | 8080 |

## 사전 요구사항

- [Docker](https://docs.docker.com/get-docker/) 및 Docker Compose v2
- 프로젝트 루트에 `.env` 파일 (환경변수 설정)

## 빠른 시작

```bash
# 1. 프로젝트 루트로 이동
cd Eunhye_Hymn

# 2. 환경변수 파일 생성
cp .env.example .env
# 필요 시 .env 파일을 편집하여 값 변경

# 3. Docker Compose 실행
docker compose -f infra/docker/docker-compose.yml up -d

# 4. 로그 확인
docker compose -f infra/docker/docker-compose.yml logs -f api

# 5. API 헬스 체크
curl http://localhost:8080/api/v1/ping
```

## 서비스 중지

```bash
docker compose -f infra/docker/docker-compose.yml down
```

데이터를 포함하여 완전히 정리하려면:

```bash
docker compose -f infra/docker/docker-compose.yml down -v
```

## 개별 서비스만 실행

API 서버 없이 PostgreSQL과 LocalStack만 실행할 수 있습니다:

```bash
docker compose -f infra/docker/docker-compose.yml up -d postgres localstack init-s3
```

이 경우 API는 로컬에서 직접 실행합니다:

```bash
cd apps/api
DB_URL=jdbc:postgresql://localhost:5432/eunhye_hymn \
JWT_SECRET=dev-secret-key-change-in-production-min-32-chars!! \
JWT_ACCESS_TTL_SECONDS=3600 \
JWT_REFRESH_TTL_SECONDS=604800 \
INVITE_CODE=dev-invite-code \
S3_ENDPOINT=http://localhost:4566 \
S3_PUBLIC_BASE_URL=http://localhost:4566/local-bucket \
./gradlew bootRun
```

## 환경변수

`.env.example` 파일을 참조하세요. 주요 변수:

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `JWT_SECRET` | JWT 서명 키 (필수) | `dev-secret-key-...` |
| `INVITE_CODE` | 초대코드 (필수) | `dev-invite-code` |
| `DB_USER` / `DB_PASS` | PostgreSQL 인증 | `postgres` / `postgres` |
| `S3_BUCKET` | S3 버킷명 | `local-bucket` |

## 주의사항

- `JWT_SECRET`과 `INVITE_CODE`는 **필수**입니다. 미설정 시 API가 부팅에 실패합니다.
- Docker Compose 내부에서 DB_URL은 `jdbc:postgresql://postgres:5432/eunhye_hymn`으로 자동 설정됩니다.
- S3_ENDPOINT는 컨테이너 간 통신을 위해 `http://localstack:4566`으로 설정됩니다.
- S3_PUBLIC_BASE_URL은 호스트에서 접근하기 위해 `http://localhost:4566/local-bucket`으로 설정됩니다.
- `init-s3.sh`는 멱등적으로 동작하며, 버킷이 이미 존재하면 건너뜁니다.
