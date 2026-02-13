# Eunhye Hymn

교회 찬양팀을 위한 악보(PNG) 및 파트 연습 음원(MIDI) 관리 시스템입니다.

관리자는 웹에서 찬양과 에셋을 등록하고, 멤버는 모바일 앱으로 악보를 보고 파트별 음원을 연습할 수 있습니다. 초대코드 기반으로 교회 내부 인원만 접근할 수 있습니다.

## 구조

```
apps/
  api/      Spring Boot 백엔드 (Java 17, Gradle)
  admin/    React 관리자 웹 (TypeScript, Vite, Tailwind CSS)
  mobile/   Flutter 모바일 앱 (MVP)
infra/
  docker/   Docker Compose (PostgreSQL + LocalStack + API)
  aws/      AWS Staging 인프라 (Terraform)
docs/       프로젝트 문서
```

## 주요 기능

### 백엔드 API

- 찬양 CRUD + 삭제 (에셋/메모/히스토리 cascade 삭제)
- S3 에셋 관리 (presigned URL로 클라이언트 직접 업로드)
- 소셜 로그인 (Google, Kakao) + JWT 인증 + 토큰 자동 회전
- 초대코드 관리 (생성, 검증, 만료, 사용 횟수 제한)
- 사용자 관리 (역할/상태 변경)
- 멤버 기능 (즐겨찾기, 메모, 히스토리, 이벤트 기록)

### 관리자 웹

- 찬양 목록/생성/수정/삭제 + 검색/필터 + 활성화 토글
- 에셋 업로드 (presign -> S3 업로드 -> confirm 3단계)
- 사용자 관리 (역할/상태 변경)
- 초대코드 관리 (생성/비활성화/만료일 설정)
- Google/Kakao 소셜 로그인 + Dev 로그인 (개발용)
- Access Token 만료 시 자동 갱신

### 모바일 앱 (Flutter MVP)

- 소셜 토큰 기반 로그인 + Dev 로그인
- 찬양 목록 조회 + 검색
- 찬양 상세 조회 (PNG 에셋 미리보기)
- 즐겨찾기 토글
- 메모 조회/저장
- 최근 열람 히스토리
- Access Token 만료 시 자동 갱신

## 문서

- 현재 사용 가능 범위: [docs/current-usable-scope.md](./docs/current-usable-scope.md)
- 운영 런북(스테이징): [docs/runbook.md](./docs/runbook.md)
- 모바일 문서: [docs/mobile/README.md](./docs/mobile/README.md)
- 로컬 셋업 가이드: [docs/LOCAL_SETUP.md](./docs/LOCAL_SETUP.md)
- 모바일 앱 README: [apps/mobile/README.md](./apps/mobile/README.md)
- 작업 기준 문서: [CLAUDE.md](./CLAUDE.md)

## 시작하기

로컬 실행/테스트/트러블슈팅 가이드는 아래 문서를 기준으로 사용하세요.

- 로컬 셋업 가이드: [docs/LOCAL_SETUP.md](./docs/LOCAL_SETUP.md)

빠른 시작(권장: Docker Compose 전체 실행):

```bash
docker compose -f infra/docker/docker-compose.yml up -d
curl http://localhost:8080/api/v1/ping

cd apps/admin
npm install
npm run dev
```

## 기술 스택

| 계층 | 기술 |
|------|------|
| Backend | Spring Boot 3.3, Java 17, Gradle 8.7 |
| Database | PostgreSQL + Flyway 마이그레이션 |
| Storage | AWS S3 (presigned URL) |
| Auth | Spring Security + JWT + Google/Kakao OAuth |
| Frontend | React 18, TypeScript, Vite, Tailwind CSS v4 |
| CI/CD | GitHub Actions (API 테스트 + Admin 빌드/타입체크 + Mobile lint/test) |

## API 엔드포인트

모든 엔드포인트는 `/api/v1` 아래에 있습니다.

| 영역 | 주요 경로 | 설명 |
|------|-----------|------|
| 인증 | `POST /auth/social` | Google/Kakao 소셜 로그인 |
| | `POST /auth/invite/validate` | 초대코드 검증 |
| | `POST /auth/refresh` | 토큰 갱신 |
| 찬양 (공개) | `GET /hymns` | 활성 찬양 목록 |
| | `GET /hymns/{id}` | 찬양 상세 + 에셋 |
| 찬양 (관리자) | `POST /admin/hymns` | 찬양 생성 |
| | `PATCH /admin/hymns/{id}` | 찬양 수정 |
| | `DELETE /admin/hymns/{id}` | 찬양 삭제 |
| 에셋 (관리자) | `POST /admin/assets/presign` | 업로드 URL 발급 |
| | `POST /admin/assets/confirm` | 업로드 확인 |
| 사용자 (관리자) | `GET /admin/users` | 사용자 목록 |
| | `PATCH /admin/users/{id}` | 역할/상태 변경 |
| 초대코드 (관리자) | `POST /admin/invite-codes` | 초대코드 생성 |
| | `DELETE /admin/invite-codes/{code}` | 비활성화 |
| 멤버 | `GET /me/favorites/{hymnId}` | 즐겨찾기 상태 조회 |
| | `POST /me/favorites/{hymnId}` | 즐겨찾기 토글 |
| | `PUT /me/hymns/{hymnId}/note` | 메모 저장 |
| | `GET /me/history` | 히스토리 |
| 헬스체크 | `GET /ping` | `{ "ok": true }` |

전체 API 명세는 [CLAUDE.md](./CLAUDE.md) 7장을 참고하세요.

## 아키텍처

Clean Architecture 기반으로 레이어를 분리합니다.

```
Domain (Entity, Repository 인터페이스)
  ^
Application (Use Cases)
  ^
Presentation (Controllers, DTOs)
  ^
Infrastructure (JPA, S3, JWT, Security)
```

의존성은 항상 안쪽(Domain)을 향합니다. 외부 서비스 호출이 필요한 경우 Application 레이어에 인터페이스(Port)를 정의하고 Infrastructure에서 구현합니다.

## Git 브랜치

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 릴리스 |
| `develop` | 개발 통합 |
| `staging` | 스테이징 배포 |
| `feat/*` | 기능 개발 (develop으로 PR) |

## 라이선스

[LICENSE](./LICENSE) 파일을 참고하세요.

