# Eunhye Hymn

교회 찬양팀을 위한 악보(PNG) 및 파트 연습 음원(MIDI) 관리 시스템입니다.

관리자는 웹(Admin)에서 찬양/에셋/사용자/운영 데이터를 관리하고, 멤버는 모바일 앱에서 인증 후 찬양을 탐색하고 연습할 수 있습니다.

## 구조

```text
apps/
  api/      Spring Boot 백엔드 (Java 17, Gradle)
  admin/    React 관리자 웹 (TypeScript, Vite, Tailwind CSS)
  mobile/   Flutter 모바일 앱
infra/
  docker/   로컬 Docker Compose (PostgreSQL + LocalStack + API)
  aws/      AWS Staging 인프라 (Terraform)
docs/       프로젝트 문서
```

## 주요 기능

### 백엔드 API
- 인증: Kakao 소셜 로그인, ID/PW 로그인/회원가입, JWT access/refresh 회전
- 성도 인증 플로우: 초대코드 검증 + SMS 인증 + 탈퇴
- 찬양/에셋: 공개 목록/상세, 관리자 CRUD, S3 presign/confirm
- 멤버 기능: 프로필 조회/수정, 개인정보 변경 요청, 즐겨찾기/메모/히스토리
- 운영 기능: 감사 로그 조회/CSV 내보내기(동기+비동기), export 운영 지표
- AI 추천: `gemini-2.5-flash-lite` 기반 상황별 찬송 추천

### 관리자 웹
- 찬양/에셋/사용자/초대코드 운영 화면
- 개인정보 변경 요청 승인/반려 화면
- 감사 로그/분석 + 비동기 CSV export 상태 추적
- AI 찬송 추천 실험 화면
- Admin ID/PW 로그인 + 토큰 자동 갱신

### 모바일 앱
- 로그인/인증: `Login -> InviteCode -> Phone -> SMS -> Home`
- 찬양 목록/검색/필터, 상세(PNG 다중 페이지 + MIDI 재생)
- AI 상황 기반 찬송 추천
- 프로필/즐겨찾기/메모/히스토리
- 오프라인 캐시 fallback + 오프라인 변경 동기화

## 문서

- 현재 사용 가능 범위: [docs/current-usable-scope.md](./docs/current-usable-scope.md)
- API 계약: [docs/api-contract.md](./docs/api-contract.md)
- 로컬 셋업: [docs/LOCAL_SETUP.md](./docs/LOCAL_SETUP.md)
- 개발 가이드: [docs/dev-guide.md](./docs/dev-guide.md)
- 시크릿 관리: [docs/SECRETS_MANAGEMENT.md](./docs/SECRETS_MANAGEMENT.md)
- 운영 헬스 로그: [docs/ops-health-log.md](./docs/ops-health-log.md)
- 릴리즈 관리(SemVer): [docs/release-management.md](./docs/release-management.md)
- 모노레포 CI/CD 운영: [docs/monorepo-cicd.md](./docs/monorepo-cicd.md)
- 병렬 터미널 PR 운영: [docs/parallel-pr-workflow.md](./docs/parallel-pr-workflow.md)
- 데이터 모델: [docs/data-model.md](./docs/data-model.md)
- 관리자 문서: [docs/admin/README.md](./docs/admin/README.md)
- 모바일 문서: [docs/mobile/README.md](./docs/mobile/README.md), [apps/mobile/README.md](./apps/mobile/README.md)
- 운영 런북(스테이징): [docs/runbook.md](./docs/runbook.md)

## 시작하기

빠른 시작(권장: Docker Compose + Admin):

```bash
docker compose -f infra/docker/docker-compose.yml up -d
curl http://localhost:8080/api/v1/ping

cd apps/admin
npm install
npm run dev
```

모바일 실행은 [docs/mobile/README.md](./docs/mobile/README.md)를 따릅니다.

## 기술 스택

| 계층 | 기술 |
|------|------|
| Backend | Spring Boot 3.3, Java 17, Gradle 8.7 |
| Database | PostgreSQL + Flyway |
| Storage | AWS S3 (presigned URL) |
| Auth | Spring Security + JWT + Kakao OAuth |
| AI | Gemini `gemini-2.5-flash-lite` |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS v4 |
| Mobile | Flutter/Dart |
| CI/CD | GitHub Actions (API/Admin/Mobile + Staging 배포 + Mobile release readiness + 태그 기반 GitHub Release) |

## API 요약 (`/api/v1`)

| 영역 | 주요 경로 |
|------|-----------|
| 인증 | `POST /auth/social`, `POST /auth/login`, `POST /auth/signup`, `POST /auth/refresh`, `POST /auth/invite/validate`, `POST /auth/sms/request`, `POST /auth/sms/verify`, `POST /auth/withdraw` |
| 찬양 | `GET /hymns`, `GET /hymns/{id}`, `POST /ai/hymn-recommendations` |
| 내 정보 | `GET/PUT /me/profile`, `POST /me/profile-change-requests`, `GET /me/profile-change-requests/latest`, `POST /me/favorites/{hymnId}`, `PUT /me/hymns/{hymnId}/note`, `GET /me/history` |
| 관리자 | `POST /auth/admin/login`, `POST /admin/auth/password`, `/admin/hymns`, `/admin/assets/*`, `/admin/users`, `/admin/invite-codes`, `/admin/events/*`, `/admin/profile-change-requests` |

상세 계약은 [docs/api-contract.md](./docs/api-contract.md)를 기준으로 합니다.

## 아키텍처

Clean Architecture 기반으로 레이어를 분리합니다.

```text
Domain (Entity, Repository 인터페이스)
  ^
Application (Use Cases)
  ^
Presentation (Controllers, DTOs)
  ^
Infrastructure (JPA, S3, JWT, Security, External AI)
```

## Git 브랜치

| 브랜치 | 용도 |
|--------|------|
| `production` | 프로덕션 릴리스 |
| `develop` | 개발 통합 |
| `staging` | 스테이징 배포 |
| `feat/*` | 기능 개발 (develop으로 PR) |

## 라이선스

[LICENSE](./LICENSE)를 참고하세요.

