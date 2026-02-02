# Eunhye Hymn Development Guide

## 1. Local Development Plan
- Use Docker for DB and local service dependencies.
- Run the API locally with mocked S3 (or localstack).
- Admin web and mobile apps run against `http://localhost` API.

## 2. Prerequisites
- Java 17+
- Docker + Docker Compose

## 3. API Setup (Local)
1. Copy the environment template:
   ```bash
   cp apps/api/.env.example apps/api/.env
   ```
2. Update `apps/api/.env` with local values.
3. Start a local PostgreSQL instance (Docker or local install).
4. Run the API:
   ```bash
   cd apps/api
   ./gradlew bootRun
   ```

## 4. API Tests
- From `apps/api`:
  ```bash
  ./gradlew test
  ```

## 5. Environment Variables
- `DB_URL`
- `DB_USER`
- `DB_PASS`
- `JWT_SECRET`
- `INVITE_CODE`
- `GOOGLE_CLIENT_ID`
- `KAKAO_CLIENT_ID`
- `S3_BUCKET`

## 6. AI-Assisted Development Guidelines
- Keep doc updates explicit and structured.
- Prefer small, atomic changes with clear commit messages.
- Update API contract before changing behavior.
