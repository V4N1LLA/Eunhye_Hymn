# Eunhye Hymn 개발 가이드

## 1. 로컬 개발 계획
- Docker로 DB 및 로컬 의존성을 구동한다.
- API는 로컬에서 실행하고, S3는 mock 또는 localstack을 사용한다.
- 관리자 웹/모바일 앱은 `http://localhost` API를 사용한다.

## 2. 사전 준비
- Java 17+
- Docker + Docker Compose

## 3. API 로컬 실행
1. 환경 변수 템플릿 복사:
   ```bash
   cp apps/api/.env.example apps/api/.env
   ```
2. `apps/api/.env` 값을 로컬 환경에 맞게 수정.
3. 로컬 PostgreSQL 실행(Docker 또는 로컬 설치).
4. API 실행:
   ```bash
   cd apps/api
   ./gradlew bootRun
   ```

## 4. 테스트 실행
- `apps/api`에서 실행:
  ```bash
  ./gradlew test
  ```

## 5. DB 환경 변수 설명
- `DB_URL`: JDBC 접속 URL (예: `jdbc:postgresql://localhost:5432/eunhye_hymn`)
- `DB_USER`: DB 사용자명
- `DB_PASS`: DB 비밀번호

## 6. Flyway 베이스라인
- 초기 마이그레이션 파일은 `V1__baseline.sql`로 비어 있으며,
  Flyway가 정상 실행되는지 확인하기 위한 용도다.
- 실제 테이블 생성은 이후 마이그레이션에서 진행한다.

## 7. 테스트 DB 동작
- 테스트 프로파일은 H2 인메모리 DB를 사용한다.
- 테스트 실행 시 Flyway가 동일하게 적용되어 외부 DB 의존이 없다.

## 8. 환경 변수 목록
- `DB_URL`
- `DB_USER`
- `DB_PASS`
- `JWT_SECRET`
- `INVITE_CODE`
- `GOOGLE_CLIENT_ID`
- `KAKAO_CLIENT_ID`
- `S3_BUCKET`
