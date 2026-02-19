# Eunhye Hymn

교회 찬양팀을 위한 악보(PNG) 및 파트 연습 음원(MIDI) 관리 시스템입니다.

관리자는 웹에서 찬양/에셋/사용자를 관리하고, 멤버는 모바일 앱으로 찬양 조회, 메모, 히스토리, 파트 연습을 수행합니다.

## 구조

```text
apps/
  api/      Spring Boot 백엔드 (Java 17, Gradle)
  admin/    React 관리자 웹 (TypeScript, Vite, Tailwind CSS)
  mobile/   Flutter 모바일 앱
infra/
  docker/   Docker Compose (PostgreSQL + LocalStack + API)
  aws/      AWS Staging 인프라 (Terraform)
docs/       프로젝트 문서
```

## 주요 기능

### 백엔드 API

- 인증: Kakao 소셜 로그인 + Admin ID/PW 로그인 + JWT/Refresh
- 초대코드: 생성/조회/비활성화/검증, 대소문자/공백 정규화
- 찬양/에셋: 관리자 CRUD, presign/confirm 기반 업로드
- 멤버 기능: 즐겨찾기, 메모, 히스토리, 이벤트 기록
- 감사 로그: 필터/집계 조회 + 동기/비동기 CSV export + 운영 지표

### 관리자 웹

- 찬양/에셋/사용자/초대코드 관리 UI
- 감사 로그 조회, 최근 N일 집계, CSV 내보내기(동기/비동기)
- 운영자 ID/PW 로그인(기본) + localhost Dev 로그인(개발 보조)

### 모바일 앱

- 로그인: Kakao + 초대코드 입력, 로컬 검증용 계정 로그인 경로
- 온보딩: 첫 로그인 후 교회/이름/구역 입력(로컬 저장)
- 찬양: 목록/검색/필터, 상세(PNG 다중 페이지), MIDI 재생
- 개인화: 즐겨찾기/메모/히스토리
- 오프라인: 캐시 fallback + 재접속 시 변경 동기화

## 시작하기

빠른 로컬 부트스트랩(권장):

```powershell
.\scripts\local-bootstrap.ps1
```

수동 실행:

```bash
docker compose --env-file .env -f infra/docker/docker-compose.yml up -d
curl http://localhost:8080/api/v1/ping

cd apps/admin
npm install
npm run dev
```

## API 엔드포인트

모든 엔드포인트는 `/api/v1` 하위에 있습니다.

| 영역 | 주요 경로 |
|------|-----------|
| 인증 | `POST /auth/social`, `POST /auth/admin/login`, `POST /auth/refresh`, `POST /auth/logout`, `POST /auth/invite/validate`, `POST /auth/dev/login` |
| 찬양(공개) | `GET /hymns`, `GET /hymns/{id}` |
| 찬양(관리자) | `GET /admin/hymns`, `POST /admin/hymns`, `PATCH /admin/hymns/{id}`, `DELETE /admin/hymns/{id}` |
| 에셋(관리자) | `POST /admin/assets/presign`, `POST /admin/assets/confirm`, `DELETE /admin/assets/{id}` |
| 사용자(관리자) | `GET /admin/users`, `POST /admin/users`, `PATCH /admin/users/{id}`, `DELETE /admin/users/{id}` |
| 초대코드(관리자) | `GET /admin/invite-codes`, `POST /admin/invite-codes`, `DELETE /admin/invite-codes/{code}` |
| 감사 로그(관리자) | `GET /admin/events`, `GET /admin/events/export`, `POST /admin/events/export-jobs`, `GET /admin/events/export-jobs/{jobId}`, `GET /admin/events/export-jobs/{jobId}/download`, `GET /admin/events/export-jobs/metrics` |
| 멤버 | `GET /me/profile`, `GET/POST /me/favorites/{hymnId}`, `GET/PUT /me/hymns/{hymnId}/note`, `GET /me/history`, `POST /events` |
| 헬스체크 | `GET /ping` |

## 기술 스택

| 계층 | 기술 |
|------|------|
| Backend | Spring Boot 3.3, Java 17, Gradle 8.7 |
| Database | PostgreSQL + Flyway |
| Storage | AWS S3/LocalStack (presigned URL) |
| Auth | Spring Security + JWT + Kakao OAuth |
| Admin | React 18, TypeScript, Vite, Tailwind CSS v4 |
| Mobile | Flutter |
| CI/CD | GitHub Actions (API CI, Admin CI, Mobile CI, Staging deploy, Mobile release check, Mobile store release readiness) |

## 문서

- 현재 상태 요약: [current_update.md](./current_update.md)
- 현재 사용 가능 범위: [docs/current-usable-scope.md](./docs/current-usable-scope.md)
- 개발 변경 이력: [docs/changelog-dev.md](./docs/changelog-dev.md)
- 배포 준비도 점검: [docs/deployment-readiness-audit.md](./docs/deployment-readiness-audit.md)
- 운영 런북(스테이징): [docs/runbook.md](./docs/runbook.md)
- 로컬 셋업 가이드: [docs/LOCAL_SETUP.md](./docs/LOCAL_SETUP.md)
- 팀 로컬 재시작 가이드: [docs/TEAM_LOCAL_DEVELOPMENT.md](./docs/TEAM_LOCAL_DEVELOPMENT.md)
- 모바일 문서: [docs/mobile/README.md](./docs/mobile/README.md)
- 모바일 앱 README: [apps/mobile/README.md](./apps/mobile/README.md)
- 시크릿 관리 가이드: [docs/SECRETS_MANAGEMENT.md](./docs/SECRETS_MANAGEMENT.md)

## 아키텍처

Clean Architecture 기반 레이어:

```text
Domain (Entity, Repository interface)
  ^
Application (Use Cases)
  ^
Presentation (Controllers, DTOs)
  ^
Infrastructure (JPA, S3, JWT, Security)
```

## Git 브랜치

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 릴리스 |
| `develop` | 개발 통합 |
| `staging` | 스테이징 배포 |
| `feat/*` | 기능 개발 (`develop`으로 PR) |

## 라이선스

[LICENSE](./LICENSE)
