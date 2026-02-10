# CLAUDE.md - Eunhye Hymn 프로젝트 컨텍스트

> 이 파일은 Claude Code가 프로젝트를 빠르게 파악하고 작업할 수 있도록 작성된 종합 레퍼런스입니다.
> 마지막 업데이트: 2026-02-10

---

## 1. 프로젝트 개요

**Eunhye Hymn**은 교회 내부용 찬양 악보(PDF) 및 파트 연습 음원(MP3) 관리 시스템입니다.

- **모노레포 구조**: 백엔드 API, 관리자 웹, 모바일 앱, 인프라를 한 저장소에서 관리
- **대상 사용자**: 관리자(콘텐츠/초대 관리), 멤버(검색/연습)
- **성공 기준**: 초대코드로 3분 내 접근, 검색 응답 1초 이내

---

## 2. 디렉토리 구조

```
Eunhye_Hymn/
├── apps/
│   ├── api/          # Spring Boot 백엔드 (Java 17, Gradle)  ← MVP 완료
│   ├── admin/        # React + Vite + TypeScript 관리자 웹    ← 에셋 업로드 페이지만 구현
│   └── mobile/       # Flutter 모바일 (placeholder)
├── infra/
│   ├── docker/       # Docker Compose (placeholder)
│   └── aws/          # AWS IaC (placeholder)
├── docs/             # 프로젝트 문서 (요구사항, 아키텍처, API 계약 등)
├── .github/workflows/  # CI/CD (api-ci.yml)
├── CLAUDE.md         # 이 파일
├── README.md
└── LICENSE
```

---

## 3. 기술 스택

| 계층 | 기술 | 버전 |
|------|------|------|
| Backend | Spring Boot | 3.3.3 |
| Language | Java | 17 |
| Build | Gradle (wrapper) | 8.7 |
| Database | PostgreSQL + Flyway | - |
| Test DB | H2 in-memory | - |
| Storage | AWS S3 (presigned URL) | SDK 2.26.21 |
| Security | Spring Security + JWT | - |
| Frontend | React + TypeScript | 18.3.1 / 5.6.3 |
| Frontend Build | Vite | 5.4.10 |
| CI/CD | GitHub Actions | - |

---

## 4. 빌드 및 실행

### 4.1 백엔드 API

```bash
# 테스트 실행 (H2 인메모리 DB 사용, 외부 의존성 불필요)
cd apps/api
./gradlew test --no-daemon --stacktrace

# 로컬 실행 (PostgreSQL + 환경변수 필요)
cd apps/api
./gradlew bootRun
```

**서버**: `http://localhost:8080/api/v1`

### 4.2 Admin 프론트엔드

```bash
cd apps/admin
npm install
npm run dev    # http://localhost:5173
npm run build  # dist/ 출력
```

### 4.3 필수 환경변수 (.env.example 참조)

| 변수 | 설명 | 필수 | 기본값 |
|------|------|------|--------|
| `DB_URL` | JDBC PostgreSQL URL | O | `jdbc:postgresql://localhost:5432/eunhye_hymn` |
| `DB_USER` | DB 사용자 | O | `postgres` |
| `DB_PASS` | DB 비밀번호 | O | `postgres` |
| `JWT_SECRET` | JWT 서명 키 | **필수 (미설정 시 부팅 실패)** | 없음 |
| `JWT_ACCESS_TTL_SECONDS` | Access 토큰 TTL | **필수** | 없음 |
| `JWT_REFRESH_TTL_SECONDS` | Refresh 토큰 TTL | **필수** | 없음 |
| `INVITE_CODE` | 초대코드 | **필수 (미설정 시 부팅 실패)** | 없음 |
| `S3_BUCKET` | S3 버킷명 | - | `local-bucket` |
| `S3_REGION` | AWS 리전 | - | `ap-northeast-2` |
| `S3_ENDPOINT` | S3 엔드포인트 오버라이드 | - | (빈 문자열) |
| `S3_PUBLIC_BASE_URL` | 에셋 공개 URL 베이스 | - | (빈 문자열) |
| `S3_PRESIGN_EXPIRES_MINUTES` | presigned URL 만료 | - | `15` |

---

## 5. 아키텍처 (Clean Architecture)

```
Domain (model, repository interfaces)
  ↑
Application (use cases)
  ↑
Interface Adapters (controllers, DTOs, mappers)
  ↑
Infrastructure (JPA adapters, S3, JWT, Security config)
```

**패키지 구조** (`apps/api/src/main/java/com/eunhyehymn/`):

```
com.eunhyehymn/
├── domain/
│   ├── model/          # Entity records, Enums
│   └── repository/     # Repository 인터페이스
├── application/
│   └── usecases/       # 비즈니스 로직 Use Cases (15개)
├── presentation/
│   ├── controllers/    # REST 컨트롤러 (8개)
│   └── dto/            # Request/Response DTOs
├── infrastructure/
│   ├── persistence/    # JPA Repository Adapters (8개)
│   ├── security/       # JWT, Security Config
│   └── storage/        # S3 Storage Service
└── config/             # Spring Bean 설정
```

---

## 6. 도메인 모델

### 6.1 Entity Records

| 엔티티 | 필드 | 비고 |
|--------|------|------|
| **User** | `id: UUID, displayName, role: Role, status: UserStatus, createdAt, lastLoginAt` | |
| **AuthIdentity** | `id: UUID, userId, provider, providerSubject, email, createdAt` | UNIQUE(provider, providerSubject) |
| **RefreshToken** | `id: UUID, userId, tokenHash, expiresAt, revokedAt, createdAt` | SHA-256 해시 저장 |
| **Hymn** | `id: UUID, title, number, tags, enabled: boolean, createdAt` | tags: 쉼표 구분 |
| **Asset** | `id: UUID, hymnId, type: AssetType, part: PartType, url, objectKey, checksum, version, createdAt` | |
| **HymnNote** | `id: UUID, userId, hymnId, content, updatedAt` | UNIQUE(userId, hymnId) |
| **UserHymnState** | `userId, hymnId (복합 PK), favorite, lastOpenedAt, lastPartPlayed: PartType, lastPlayPositionMs` | |
| **Event** | `id: UUID, userId, eventType: EventType, hymnId, part: PartType, metadataJson, createdAt` | |

### 6.2 Enums

| Enum | 값 |
|------|-----|
| **Role** | `USER`, `ADMIN` |
| **UserStatus** | `ACTIVE`, `DISABLED` |
| **AssetType** | `PDF`, `AUDIO` |
| **PartType** | `S`, `A`, `T`, `B`, `ALL` |
| **EventType** | `HYMN_OPENED`, `PART_PLAYED`, `NOTE_SAVED`, `FAVORITE_TOGGLED` |

---

## 7. API 엔드포인트 (Base: `/api/v1`)

### 7.1 인증 (`AuthController`, `DevAuthController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/auth/invite/validate` | 없음 | 초대코드 검증 |
| POST | `/auth/social` | 없음 | 소셜 로그인 (Google/Kakao) |
| POST | `/auth/refresh` | 없음 | 토큰 갱신 (RefreshRequest → TokenResponse) |
| POST | `/auth/logout` | 없음 | 로그아웃 (LogoutRequest) |
| POST | `/auth/dev/login` | 없음 | **dev 프로필 전용** (DevLoginRequest → TokenResponse) |

### 7.2 찬양 - 공개 (`HymnController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/hymns` | **없음** | enabled 찬양 목록 → `List<HymnSummary>` |
| GET | `/hymns/{id}` | 필요 | 찬양 상세 + 에셋 → `HymnDetailResponse` (lastOpenedAt 갱신) |

### 7.3 찬양 - 관리자 (`AdminHymnController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/admin/hymns` | ADMIN | 찬양 생성 (CreateRequest → HymnResponse) |
| PATCH | `/admin/hymns/{id}` | ADMIN | 찬양 수정 (UpdateRequest, null이 아닌 필드만 변경) |
| GET | `/admin/hymns` | ADMIN | 전체 목록 (disabled 포함) |

### 7.4 에셋 - 관리자 (`AdminAssetController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/admin/assets/presign` | ADMIN | S3 업로드 URL 발급 (PresignRequest → PresignResponse) |
| POST | `/admin/assets/confirm` | ADMIN | 업로드 확인 (ConfirmRequest → ConfirmResponse) |

**에셋 업로드 플로우**: presign → 클라이언트가 S3에 직접 PUT → confirm

**objectKey 형식**: `hymns/{hymnId}/{type}/{part}/{uuid}-{filename}`

### 7.5 사용자 개인 (`MeController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/me/profile` | 필요 | 프로필 → `{userId, role}` |
| POST | `/me/favorites/{hymnId}` | 필요 | 즐겨찾기 토글 → `FavoriteResponse` |
| GET | `/me/hymns/{hymnId}/note` | 필요 | 메모 조회 → `NoteResponse` |
| PUT | `/me/hymns/{hymnId}/note` | 필요 | 메모 저장 (NoteRequest) |
| GET | `/me/history` | 필요 | 최근 본 찬양 → `List<HistoryItemResponse>` |

### 7.6 이벤트 (`EventController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/events` | 필요 | 이벤트 기록 (단건 또는 배열, JsonNode 파싱) |

### 7.7 시스템

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/ping` | 없음 | `{ "ok": true }` |
| GET | `/actuator/health` | 없음 | Spring Actuator 헬스 |

---

## 8. Use Cases (15개)

| Use Case | 메서드 | 핵심 로직 |
|----------|--------|-----------|
| `ListHymnsUseCase` | `listEnabled()` | enabled 찬양만 조회 |
| `GetHymnDetailUseCase` | `getDetail(hymnId, userId)` | 상세+에셋 조회, lastOpenedAt 갱신 |
| `AdminCreateHymnUseCase` | `create(title, number, tags, enabled)` | 찬양 생성 |
| `AdminUpdateHymnUseCase` | `update(hymnId, ...)` | non-null 필드만 업데이트 |
| `AdminListHymnsUseCase` | `listAll()` | disabled 포함 전체 조회 |
| `AdminPresignAssetUseCase` | `presign(hymnId, type, part, filename, contentType)` | S3 presigned URL 생성 |
| `AdminConfirmAssetUseCase` | `confirm(hymnId, type, part, publicUrl, objectKey, ...)` | objectKey 검증, 기존 에셋 교체 |
| `GetHymnNoteUseCase` | `get(userId, hymnId)` | 메모 조회 |
| `SaveHymnNoteUseCase` | `save(userId, hymnId, content)` | 메모 upsert |
| `ToggleFavoriteUseCase` | `toggle(userId, hymnId)` | 즐겨찾기 토글 (없으면 true, 있으면 반전) |
| `GetHistoryUseCase` | `getHistory(userId)` | lastOpenedAt desc 정렬 |
| `RefreshTokenUseCase` | `refresh(rawRefreshToken)` | 해시 검증, 만료 확인, 토큰 회전 |
| `LogoutUseCase` | `logout(rawRefreshToken)` | revokedAt 설정 |
| `DevLoginUseCase` | `login(userId, role, displayName)` | 사용자 생성/갱신, 토큰 발급 |
| `RecordEventsUseCase` | `record(userId, List<EventInput>)` | EventType/PartType 검증 후 저장 |

---

## 9. 데이터베이스 스키마 (Flyway)

마이그레이션 파일: `apps/api/src/main/resources/db/migration/`

| 파일 | 내용 |
|------|------|
| `V1__baseline.sql` | 빈 베이스라인 |
| `V2__init.sql` | 8개 테이블 전체 생성 (users, auth_identities, refresh_tokens, hymns, assets, hymn_notes, user_hymn_state, events) |
| `V3__asset_object_key.sql` | assets에 object_key 컬럼 추가 (NOT NULL) |
| `V4__asset_part_not_null.sql` | assets.part NULL → 'ALL' 변환 후 NOT NULL 제약 |

### 주요 인덱스
- `idx_refresh_tokens_user_id` ON refresh_tokens(user_id)
- `idx_assets_hymn_id` ON assets(hymn_id)
- `idx_user_hymn_state_user_opened` ON user_hymn_state(user_id, last_opened_at)
- `idx_events_user_created` ON events(user_id, created_at)

### 주요 제약조건
- `uq_auth_identity_provider_subject` UNIQUE(provider, provider_subject)
- `uq_hymn_notes_user_hymn` UNIQUE(user_id, hymn_id)
- `pk_user_hymn_state` PRIMARY KEY(user_id, hymn_id) — 복합 키

---

## 10. Security 설정

### 10.1 Spring Security (SecurityConfig)

**공개 경로**: `/ping`, `/actuator/health`, `/auth/**`, `GET /hymns`

**인증 필요**: `GET /hymns/**`, `POST /events`, `/me/**`

**ADMIN 전용**: `/admin/**`

**세션**: STATELESS (JWT 기반)

**필터 체인**: `JwtAuthenticationFilter` → `UsernamePasswordAuthenticationFilter` 이전에 등록

### 10.2 JWT 구조
- Access Token: 짧은 TTL, userId + role 클레임
- Refresh Token: 긴 TTL, SHA-256 해시로 DB 저장, 사용 시 회전(rotation)

### 10.3 부팅 시 검증 (`SecurityRequiredProperties`)
- `JWT_SECRET`, `JWT_ACCESS_TTL_SECONDS`, `JWT_REFRESH_TTL_SECONDS`, `INVITE_CODE` 미설정 시 **즉시 부팅 실패**
- test 프로필에서는 검증 비활성화

---

## 11. S3 Storage 구현

**S3StorageService.java**:
- `presignUpload()`: objectKey 생성 → S3Presigner로 PUT presigned URL 발급
- objectKey: `hymns/{hymnId}/{type}/{part}/{uuid}-{filename}`
- publicUrl: S3_PUBLIC_BASE_URL 우선, 없으면 endpoint 또는 AWS 기본 URL

**에셋 교체 정책**: 동일 (hymnId, type, part) 조합 시 기존 에셋 삭제 후 새로 저장

---

## 12. 테스트

| 테스트 파일 | 대상 |
|-------------|------|
| `ContextLoadTest` | 애플리케이션 컨텍스트 로딩 |
| `PingControllerTest` | `/ping` 엔드포인트 |
| `HymnApiTest` | 찬양 목록/상세 API |
| `AdminHymnApiTest` | 관리자 찬양 CRUD API |
| `AdminAssetApiTest` | 에셋 presign/confirm API |
| `AuthFlowTest` | 인증/토큰 갱신 플로우 |
| `FlywayRepositoryIntegrationTest` | DB 마이그레이션 통합 |
| `GetHistoryUseCaseTest` | 히스토리 Use Case 단위 |

- **테스트 DB**: H2 인메모리 (test 프로필)
- **전체 테스트 통과** 확인 (2026-02-10 기준)

---

## 13. Admin 프론트엔드 현황

### 기술 스택
- React 18 + TypeScript + Vite (ESM, `"type": "module"`)
- react-router-dom v7 — 클라이언트 라우팅
- Tailwind CSS v4 (`@tailwindcss/vite` 플러그인)
- `@vitejs/plugin-react` — React HMR/Fast Refresh
- Vite dev server `/api/v1` → `http://localhost:8080` 프록시

### 구현된 기능

| 파일 | 역할 |
|------|------|
| `src/api/client.ts` | 공통 fetch wrapper (`apiGet`, `apiPost`, `apiPatch`), Bearer 토큰 자동 추가, 401 시 로그인 redirect |
| `src/api/auth.ts` | Dev 로그인, 토큰 갱신, 로그아웃 API |
| `src/api/hymns.ts` | 찬양 목록/생성/수정/상세 API |
| `src/api/adminAssets.ts` | 에셋 presign/confirm API (공통 클라이언트 사용) |
| `src/auth/AuthContext.tsx` | AuthProvider + `useAuth()` 훅, JWT 파싱, localStorage 토큰 관리 |
| `src/auth/ProtectedRoute.tsx` | 미인증 시 `/login` redirect |
| `src/components/Layout.tsx` | 사이드바(찬양 관리, 에셋 업로드) + 로그아웃 |
| `src/pages/LoginPage.tsx` | Dev Login 폼 (ADMIN 역할, UUID 자동생성) |
| `src/pages/HymnListPage.tsx` | 찬양 목록 테이블 (번호, 제목, 태그, 활성 상태) |
| `src/pages/HymnCreatePage.tsx` | 찬양 생성 폼 (title, number, tags, enabled) |
| `src/pages/HymnEditPage.tsx` | 찬양 수정 폼 + 에셋 목록 + 임베디드 업로드 |
| `src/pages/AdminAssetUploadPage.tsx` | 에셋 업로드 3단계 UI (Tailwind 스타일, hymnId props 지원) |
| `src/App.tsx` | BrowserRouter 라우팅 설정 |

### 라우팅 구조

| 경로 | 페이지 | 인증 |
|------|--------|------|
| `/login` | LoginPage | 공개 |
| `/` | → `/hymns` redirect | 필요 |
| `/hymns` | HymnListPage | 필요 |
| `/hymns/new` | HymnCreatePage | 필요 |
| `/hymns/:id/edit` | HymnEditPage | 필요 |
| `/assets/upload` | AdminAssetUploadPage | 필요 |

### 미구현 기능
- 소셜 로그인 (현재 Dev Login만 지원)
- 초대코드 관리
- 찬양 검색/필터
- 사용자/역할 관리

---

## 14. CI/CD

**워크플로우**: `.github/workflows/api-ci.yml`

- **트리거**: PR 및 develop 브랜치 push
- **환경**: ubuntu-latest, Java 17 (temurin)
- **캐시**: Gradle (gradle/actions/setup-gradle@v3)
- **실행**: `./gradlew test --no-daemon --stacktrace` (apps/api 디렉토리)

---

## 15. Git 브랜치

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 릴리스 |
| `develop` | 개발 통합 (현재 작업 브랜치) |
| `staging` | 스테이징 배포 |

---

## 16. 현재 진행 상태 및 남은 작업

### 완료
- Spring Boot API 전체 구현 (15개 UseCase, 8개 Controller)
- JWT 인증 + 소셜 로그인 + 초대코드
- 찬양 CRUD + S3 에셋 관리
- 사용자 기능 (즐겨찾기, 메모, 히스토리)
- DB 스키마 + Flyway 마이그레이션
- 테스트 전체 통과
- CI/CD 파이프라인
- 문서화
- **Admin 웹 프론트엔드 기반**: 라우팅, Tailwind 스타일링, 인증 컨텍스트, 공통 API 클라이언트, Dev 로그인, 찬양 목록/생성/수정, 에셋 업로드, 사이드바 레이아웃

### 미완료 (우선순위순)
1. **Admin 추가 기능**: 소셜 로그인 연동, 초대코드 관리, 찬양 검색/필터, 사용자/역할 관리
2. **Docker Compose**: 로컬 개발 환경 (PostgreSQL, LocalStack)
3. **AWS 인프라**: VPC, RDS, S3, ECS/EKS, IAM (전부 placeholder)
4. **배포 자동화**: Staging/Production 파이프라인
5. **모니터링/알림**: CloudWatch, 에러 추적
6. **모바일 앱**: Flutter (코드 없음)
7. **추가 API**: 초대코드 CRUD, 사용자/역할 관리

---

## 17. 작업 시 주의사항

- **Clean Architecture 준수**: 의존성 방향은 반드시 외부→내부 (infrastructure → application → domain)
- **API 베이스 경로**: 항상 `/api/v1` 접두사 사용
- **환경변수**: JWT_SECRET, INVITE_CODE는 기본값 없음 — 반드시 설정해야 부팅
- **테스트**: H2 인메모리 DB 사용, test 프로필에서 SecurityRequiredProperties 비활성화
- **Gradle**: 시스템 gradle이 아닌 wrapper (`./gradlew`) 사용
- **Windows**: gradlew.bat에서 JAVA_HOME 공백 경로 처리 수정 적용됨
- **에셋 교체**: 동일 (hymnId, type, part) 조합 시 기존 레코드 삭제 후 새로 생성
