# Eunhye Hymn Development Guide

## 1. Local Development Plan
- Use Docker for DB and local service dependencies.
- Run the API locally with mocked S3 (or localstack).
- Admin web and mobile apps run against `http://localhost` API.

## 2. Prerequisites (Placeholder)
- Node.js LTS
- Java 17+ (if API is JVM-based)
- Flutter stable
- Docker + Docker Compose

## 3. Setup Steps (Placeholder)
1. Clone repository.
2. Copy `.env.example` to `.env` (to be added later).
3. Start local dependencies via Docker.
4. Run API, then admin web, then mobile.

## 4. Environment Variables (Placeholder)
- `DATABASE_URL`
- `JWT_SECRET`
- `GOOGLE_CLIENT_ID`
- `KAKAO_CLIENT_ID`
- `S3_BUCKET`

## 5. AI-Assisted Development Guidelines
- Keep doc updates explicit and structured.
- Prefer small, atomic changes with clear commit messages.
- Update API contract before changing behavior.
