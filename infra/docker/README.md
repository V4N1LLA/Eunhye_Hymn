# Docker 로컬 개발 환경

Docker Compose를 사용한 Eunhye Hymn 로컬 개발 환경 안내입니다.

## 구성 서비스

| 서비스 | 설명 | 포트 |
|--------|------|------|
| `postgres` | PostgreSQL 15 (Alpine) | 5432 |
| `localstack` | LocalStack S3 에뮬레이션 | 4566 |
| `init-s3` | S3 버킷 초기화(일회성) | - |
| `api` | Spring Boot API | 8080 |

## 사전 요구사항

- Docker + Docker Compose v2
- 저장소 루트 `.env` 파일

## 빠른 시작

```bash
cd Eunhye_Hymn
cp .env.example .env
docker compose -f infra/docker/docker-compose.yml up -d
docker compose -f infra/docker/docker-compose.yml logs -f api
curl http://localhost:8080/api/v1/ping
```

## 중지/정리

```bash
docker compose -f infra/docker/docker-compose.yml down
docker compose -f infra/docker/docker-compose.yml down -v
```

## DB/S3만 실행하고 API는 로컬 실행

```bash
docker compose -f infra/docker/docker-compose.yml up -d postgres localstack init-s3

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

## 주요 환경변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `JWT_SECRET` | JWT 서명 키(필수) | `dev-secret-key-...` |
| `INVITE_CODE` | 초대코드(필수) | `dev-invite-code` |
| `DB_USER` / `DB_PASS` | PostgreSQL 인증 | `postgres` / `postgres` |
| `S3_BUCKET` | S3 버킷명 | `local-bucket` |
| `SMS_TWILIO_ENABLED` | SMS 실발송 여부 | `false` |
| `AI_GEMINI_ENABLED` | AI 추천 활성화 여부 | `false` |
| `AI_GEMINI_API_KEY` | Gemini API 키 | (빈값) |

## 주의사항

- `JWT_SECRET`, `INVITE_CODE` 미설정 시 API가 부팅되지 않습니다.
- Compose 내부 `DB_URL`은 `jdbc:postgresql://postgres:5432/eunhye_hymn`를 사용합니다.
- Compose 내부 `S3_ENDPOINT`는 `http://localstack:4566`를 사용합니다.
- 호스트 접근용 `S3_PUBLIC_BASE_URL`은 `http://localhost:4566/local-bucket`를 사용합니다.
- AI 추천 테스트 시에만 `AI_GEMINI_ENABLED=true`와 `AI_GEMINI_API_KEY`를 함께 설정하세요.
