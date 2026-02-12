# CLAUDE.md - Eunhye Hymn 프로젝트 컨텍스트

> 이 파일은 Claude Code가 프로젝트를 빠르게 파악하고 작업할 수 있도록 작성된 종합 레퍼런스입니다.
> 마지막 업데이트: 2026-02-13

---

## 1. 프로젝트 개요

**Eunhye Hymn**은 교회 내부용 찬양 악보(PNG) 및 파트 연습 음원(MIDI) 관리 시스템입니다.

- **모노레포 구조**: 백엔드 API, 관리자 웹, 모바일 앱, 인프라를 한 저장소에서 관리
- **대상 사용자**: 관리자(콘텐츠/초대 관리), 멤버(검색/연습)
- **성공 기준**: 초대코드로 3분 내 접근, 검색 응답 1초 이내

---

## 2. 디렉토리 구조

```
Eunhye_Hymn/
├── apps/
│   ├── api/          # Spring Boot 백엔드 (Java 17, Gradle)  ← MVP 완료 (24 UseCase)
│   ├── admin/        # React + Vite + TypeScript 관리자 웹    ← CRUD + 삭제 + 검색 + 에셋 업로드
│   └── mobile/       # Flutter 모바일 (placeholder)
├── infra/
│   ├── docker/       # Docker Compose (PostgreSQL + LocalStack + API)
│   └── aws/          # AWS IaC (placeholder)
├── docs/             # 프로젝트 문서 (요구사항, 아키텍처, API 계약 등)
├── .github/workflows/  # CI/CD (api-ci.yml, admin-ci.yml)
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
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID | - | (빈 문자열, 미설정 시 aud 검증 생략) |
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
│   ├── ports/          # 외부 서비스 인터페이스 (SocialTokenVerifier)
│   └── usecases/       # 비즈니스 로직 Use Cases (24개)
├── presentation/
│   ├── controllers/    # REST 컨트롤러 (10개)
│   └── dto/            # Request/Response DTOs
├── infrastructure/
│   ├── persistence/    # JPA Repository Adapters (9개)
│   ├── security/       # JWT, Social Login, Security Config
│   └── storage/        # S3 Storage Service
└── common/
    └── config/         # Spring Bean 설정 (UseCaseConfig, AuthConfig, AssetConfig)
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
| **InviteCode** | `code: String (PK), createdBy: UUID, description, maxUses, usedCount, enabled, expiresAt, createdAt` | DB 기반 초대코드 관리 |

### 6.2 Enums

| Enum | 값 |
|------|-----|
| **Role** | `USER`, `ADMIN` |
| **UserStatus** | `ACTIVE`, `DISABLED` |
| **AssetType** | `PNG`, `MIDI` |
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
| DELETE | `/admin/hymns/{id}` | ADMIN | 찬양 삭제 (에셋, 메모, 상태, 이벤트 cascade 삭제) |
| GET | `/admin/hymns` | ADMIN | 전체 목록 (disabled 포함) |

### 7.4 에셋 - 관리자 (`AdminAssetController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/admin/assets/presign` | ADMIN | S3 업로드 URL 발급 (PresignRequest → PresignResponse) |
| POST | `/admin/assets/confirm` | ADMIN | 업로드 확인 (ConfirmRequest → ConfirmResponse) |
| DELETE | `/admin/assets/{id}` | ADMIN | 에셋 삭제 (DB 레코드만, S3 객체는 잔존) |

**에셋 업로드 플로우**: presign → 클라이언트가 S3에 직접 PUT → confirm

**objectKey 형식**: `hymns/{hymnId}/{type}/{part}/{uuid}-{filename}`

### 7.5 사용자 - 관리자 (`AdminUserController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/admin/users` | ADMIN | 사용자 목록 → `List<UserResponse>` |
| PATCH | `/admin/users/{id}` | ADMIN | 역할/상태 변경 (role, status) |

### 7.6 초대코드 - 관리자 (`AdminInviteCodeController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/admin/invite-codes` | ADMIN | 초대코드 생성 (code, description, maxUses, expiresAt) |
| GET | `/admin/invite-codes` | ADMIN | 초대코드 목록 |
| DELETE | `/admin/invite-codes/{code}` | ADMIN | 초대코드 비활성화 |

### 7.7 사용자 개인 (`MeController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/me/profile` | 필요 | 프로필 → `{userId, role}` |
| POST | `/me/favorites/{hymnId}` | 필요 | 즐겨찾기 토글 → `FavoriteResponse` |
| GET | `/me/hymns/{hymnId}/note` | 필요 | 메모 조회 → `NoteResponse` |
| PUT | `/me/hymns/{hymnId}/note` | 필요 | 메모 저장 (NoteRequest) |
| GET | `/me/history` | 필요 | 최근 본 찬양 → `List<HistoryItemResponse>` |

### 7.8 이벤트 (`EventController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/events` | 필요 | 이벤트 기록 (단건 또는 배열, JsonNode 파싱) |

### 7.9 시스템

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/ping` | 없음 | `{ "ok": true }` |
| GET | `/actuator/health` | 없음 | Spring Actuator 헬스 |

---

## 8. Use Cases (24개)

| Use Case | 메서드 | 핵심 로직 |
|----------|--------|-----------|
| `ListHymnsUseCase` | `listEnabled()` | enabled 찬양만 조회 |
| `GetHymnDetailUseCase` | `getDetail(hymnId, userId)` | 상세+에셋 조회, lastOpenedAt 갱신 |
| `AdminCreateHymnUseCase` | `create(title, number, tags, enabled)` | 찬양 생성 |
| `AdminUpdateHymnUseCase` | `update(hymnId, ...)` | non-null 필드만 업데이트 |
| `AdminDeleteHymnUseCase` | `delete(hymnId)` | **@Transactional** 에셋/메모/상태/이벤트 cascade 삭제 |
| `AdminListHymnsUseCase` | `listAll()` | disabled 포함 전체 조회 |
| `AdminPresignAssetUseCase` | `presign(hymnId, type, part, filename, contentType)` | S3 presigned URL 생성 |
| `AdminConfirmAssetUseCase` | `confirm(hymnId, type, part, publicUrl, objectKey, ...)` | objectKey 검증, 기존 에셋 교체 |
| `AdminDeleteAssetUseCase` | `delete(assetId)` | 단일 에셋 삭제 |
| `GetHymnNoteUseCase` | `get(userId, hymnId)` | 메모 조회 |
| `SaveHymnNoteUseCase` | `save(userId, hymnId, content)` | 메모 upsert |
| `ToggleFavoriteUseCase` | `toggle(userId, hymnId)` | 즐겨찾기 토글 (없으면 true, 있으면 반전) |
| `GetHistoryUseCase` | `getHistory(userId)` | lastOpenedAt desc 정렬 |
| `SocialLoginUseCase` | `login(provider, token, inviteCode?)` | Google/Kakao 토큰 검증, 신규 사용자는 초대코드 필요 |
| `RefreshTokenUseCase` | `refresh(rawRefreshToken)` | 해시 검증, 만료 확인, 토큰 회전 |
| `LogoutUseCase` | `logout(rawRefreshToken)` | revokedAt 설정 |
| `DevLoginUseCase` | `login(userId, role, displayName)` | 사용자 생성/갱신, 토큰 발급 |
| `RecordEventsUseCase` | `record(userId, List<EventInput>)` | EventType/PartType 검증 후 저장 |
| `AdminListUsersUseCase` | `listAll()` | 전체 사용자 목록 조회 |
| `AdminUpdateUserUseCase` | `update(userId, role, status)` | 사용자 역할/상태 변경 |
| `AdminCreateInviteCodeUseCase` | `create(code, createdBy, ...)` | 초대코드 생성 (중복 검사) |
| `AdminListInviteCodesUseCase` | `listAll()` | 전체 초대코드 목록 |
| `AdminRevokeInviteCodeUseCase` | `revoke(code)` | 초대코드 비활성화 (enabled=false) |
| `ValidateInviteCodeUseCase` | `validate(code)` | 초대코드 검증 (enabled, 만료, 사용 횟수) |

---

## 9. 데이터베이스 스키마 (Flyway)

마이그레이션 파일: `apps/api/src/main/resources/db/migration/`

| 파일 | 내용 |
|------|------|
| `V1__baseline.sql` | 빈 베이스라인 |
| `V2__init.sql` | 8개 테이블 전체 생성 (users, auth_identities, refresh_tokens, hymns, assets, hymn_notes, user_hymn_state, events) |
| `V3__asset_object_key.sql` | assets에 object_key 컬럼 추가 (NOT NULL) |
| `V4__asset_part_not_null.sql` | assets.part NULL → 'ALL' 변환 후 NOT NULL 제약 |
| `V5__asset_type_png_midi.sql` | assets.type PDF → PNG, AUDIO → MIDI 변환 |
| `V6__invite_codes.sql` | invite_codes 테이블 생성 (code PK, created_by FK, max_uses, used_count, enabled, expires_at) |

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
| `AdminHymnApiTest` | 관리자 찬양 CRUD + 삭제 API |
| `AdminAssetApiTest` | 에셋 presign/confirm/삭제 API |
| `AuthFlowTest` | 인증/토큰 갱신 플로우 |
| `SocialLoginApiTest` | 소셜 로그인 API (Google/Kakao, 초대코드 검증, 기존 사용자) |
| `FlywayRepositoryIntegrationTest` | DB 마이그레이션 통합 |
| `GetHistoryUseCaseTest` | 히스토리 Use Case 단위 |
| `AdminUserApiTest` | 사용자 관리 API (목록, 역할/상태 변경, 권한 검사) |
| `AdminInviteCodeApiTest` | 초대코드 관리 API (생성, 목록, 비활성화, 검증) |

- **테스트 DB**: H2 인메모리 (test 프로필)
- **전체 테스트 통과** 확인 (2026-02-12 기준)

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
| `src/api/client.ts` | 공통 fetch wrapper (`apiGet`, `apiPost`, `apiPatch`, `apiDelete`), Bearer 토큰 자동 추가, **401 시 자동 토큰 갱신 후 재시도** (mutex 패턴), `unauthorized` 모드 (`"redirect"` / `"throw"`), `includeAuth` 옵션 |
| `src/api/auth.ts` | **소셜 로그인** (Google/Kakao), 초대코드 검증, Dev 로그인, 토큰 갱신, 로그아웃 API (`PUBLIC_AUTH_OPTIONS`로 인증 없는 요청 구분) |
| `src/api/hymns.ts` | 찬양 목록/생성/수정/**삭제**/상세 API |
| `src/api/adminAssets.ts` | 에셋 presign/confirm/삭제 API (공통 클라이언트 사용) |
| `src/api/adminUsers.ts` | 사용자 목록/역할·상태 변경 API |
| `src/api/adminInviteCodes.ts` | 초대코드 목록/생성/비활성화 API |
| `src/auth/AuthContext.tsx` | AuthProvider + `useAuth()` 훅, JWT 파싱, localStorage 토큰 관리, **`loginWithSocial`** + `setTokensAndUser` 공통 헬퍼 |
| `src/auth/ProtectedRoute.tsx` | 미인증 시 `/login` redirect |
| `src/components/Layout.tsx` | 사이드바(찬양 관리, 에셋 업로드, 사용자 관리, 초대코드 관리) + 로그아웃 |
| `src/pages/LoginPage.tsx` | **Google/Kakao 소셜 로그인** + 초대코드 입력 + 접이식 Dev Login |
| `src/pages/HymnListPage.tsx` | 찬양 목록 테이블 (번호, 제목, 태그, 활성 상태) + 검색/필터 + 활성화 토글 + **삭제** |
| `src/pages/HymnCreatePage.tsx` | 찬양 생성 폼 (title, number, tags, enabled) |
| `src/pages/HymnEditPage.tsx` | 찬양 수정 폼 + 에셋 목록(URL 링크, 삭제) + 임베디드 업로드 + 업로드 후 자동 새로고침 + **찬양 삭제 버튼** |
| `src/pages/AdminAssetUploadPage.tsx` | 에셋 업로드 3단계 UI (Tailwind 스타일, hymnId/onConfirmed props 지원) |
| `src/pages/UserListPage.tsx` | 사용자 목록 테이블 + 역할/상태 변경 + 검색/필터 |
| `src/pages/InviteCodePage.tsx` | 초대코드 목록 + 생성/비활성화 + **만료일 설정 및 표시** |
| `src/App.tsx` | BrowserRouter 라우팅 설정 |

### 라우팅 구조

| 경로 | 페이지 | 인증 |
|------|--------|------|
| `/login` | LoginPage (소셜 + Dev) | 공개 |
| `/` | → `/hymns` redirect | 필요 |
| `/hymns` | HymnListPage | 필요 |
| `/hymns/new` | HymnCreatePage | 필요 |
| `/hymns/:id/edit` | HymnEditPage | 필요 |
| `/assets/upload` | AdminAssetUploadPage | 필요 |
| `/users` | UserListPage | 필요 |
| `/invite-codes` | InviteCodePage | 필요 |

---

## 14. CI/CD

### API CI (`api-ci.yml`)
- **트리거**: PR 및 develop push (apps/api/** 변경 시)
- **환경**: ubuntu-latest, Java 17 (temurin)
- **캐시**: Gradle (gradle/actions/setup-gradle@v3)
- **실행**: `./gradlew test --no-daemon --stacktrace`

### Admin CI (`admin-ci.yml`)
- **트리거**: PR 및 develop push (apps/admin/** 변경 시)
- **환경**: ubuntu-latest, Node.js 20
- **캐시**: npm
- **실행**: `npm ci` → `tsc --noEmit` → `npm run build`

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

**백엔드 API (24 UseCase, 10 Controller)**
- 찬양 CRUD + 삭제 (cascade: 에셋/메모/상태/이벤트)
- S3 에셋 관리 (presign/confirm/삭제)
- JWT 인증 + 소셜 로그인 (Google/Kakao) + 토큰 회전
- DB 기반 초대코드 관리 (CRUD + 검증 + 원자적 사용 횟수 증가)
- 사용자 관리 (역할/상태 변경)
- 멤버 기능 (즐겨찾기, 메모, 히스토리, 이벤트 기록)
- DB 스키마 Flyway 마이그레이션 (V1~V6)
- 테스트 11개 파일 전체 통과 (SocialLoginApiTest 포함)

**Admin 프론트엔드 (7페이지)**
- 찬양 목록/생성/수정/삭제 + 검색/필터 + 활성화 토글
- 에셋 업로드 3단계 (presign/upload/confirm) + 삭제
- 사용자 관리 (역할/상태 변경, 검색/필터)
- 초대코드 관리 (생성/비활성화/만료일 설정)
- 소셜 로그인 UI (Google/Kakao) + 초대코드 입력 플로우
- Access Token 자동 갱신 (401 → refresh → 재시도, mutex 패턴)
- 인증 컨텍스트 (`loginWithSocial`, `setTokensAndUser`), 공통 API 클라이언트, 사이드바 레이아웃
- Dev Login (개발용, 접이식)

**인프라/CI**
- Docker Compose (PostgreSQL + LocalStack + API)
- API Dockerfile (multi-stage)
- CI/CD (API 테스트 + Admin 빌드/타입체크)

### 미완료 (우선순위순)

**1. AWS 인프라**: VPC, RDS, S3, ECS/EKS, IAM (전부 placeholder)

**2. 배포 자동화**: Staging/Production 파이프라인

**3. 모니터링/알림**: CloudWatch, 에러 추적

**4. 모바일 앱**: Flutter (코드 없음)

---

## 17. 작업 시 주의사항

- **Clean Architecture 준수**: 의존성 방향은 반드시 외부→내부 (infrastructure → application → domain)
- **API 베이스 경로**: 항상 `/api/v1` 접두사 사용
- **환경변수**: JWT_SECRET, INVITE_CODE는 기본값 없음 — 반드시 설정해야 부팅
- **테스트**: H2 인메모리 DB 사용, test 프로필에서 SecurityRequiredProperties 비활성화
- **Gradle**: 시스템 gradle이 아닌 wrapper (`./gradlew`) 사용
- **Windows**: gradlew.bat에서 JAVA_HOME 공백 경로 처리 수정 적용됨
- **에셋 교체**: 동일 (hymnId, type, part) 조합 시 기존 레코드 삭제 후 새로 생성
- **CLAUDE.md 동기화 (필수)**: 모든 PR에 CLAUDE.md 업데이트를 포함할 것. 변경된 기능, 파일, 진행 상태(완료/미완료)를 반영하여 어떤 환경·세션에서든 이 파일만 읽으면 바로 작업을 이어갈 수 있도록 유지한다
- **작업 브랜치**: `develop`에서 `feat/*` 브랜치를 생성하여 작업하고, PR은 `develop`으로 보낸다. `main` merge는 사용자가 명시적으로 요청할 때만 수행

---

## 18. 코드 품질 Skill (개발 시 필수 준수)

개발 과정에서 아래 두 skill의 원칙을 항상 적용한다.

### 18.1 Clean Architecture Skill (`/clean-architecture`)

코드 작성 및 리뷰 시 Robert C. Martin의 Clean Architecture 원칙을 준수한다.

**핵심 규칙**:
- **의존성 규칙**: 소스코드 의존성은 반드시 안쪽(상위 정책)을 향해야 한다
  - Entity(Domain) → Use Case(Application) → Interface Adapters(Presentation) → Infrastructure
  - 내부 레이어는 외부 레이어를 절대 참조하지 않는다
- **경계 횡단 시 DIP 적용**: Use Case가 외부를 호출해야 할 때 Output Port(인터페이스)를 내부에 정의하고, 외부에서 구현
- **경계를 넘는 데이터**: Entity나 DB Row를 직접 전달하지 않고, 각 레이어별 DTO로 변환
- **SOLID 원칙**: 특히 DIP(의존성 역전)는 경계 횡단의 핵심

**실용적 주의점**:
- 레이어 분리 자체가 목적이 아님 — 실질적 이점이 있을 때 적용
- 구현체가 하나뿐인 인터페이스의 과잉 추상화 경계
- 0.001% 확률의 미래 변경을 위한 오버엔지니어링 금지

### 18.2 Kent Beck Style Skill (`/kent-beck-style`)

Kent Beck의 리팩토링 철학과 Simple Design 원칙을 준수한다.

**Simple Design 4규칙** (우선순위순):
1. 테스트를 통과한다
2. 의도를 드러낸다 (Reveals Intent)
3. 중복이 없다 (DRY)
4. 최소 요소만 가진다 (Fewest Elements)

**핵심 원칙**:
- **YAGNI**: 지금 필요한 기능만 구현, 미래를 위한 투기적 일반화 금지
- **KISS**: 영리한 코드보다 단순한 코드, 설명하기 어려우면 너무 복잡한 것
- **점진적 설계**: 작은 변경 → 테스트 → 커밋 리듬으로 작업

**주요 Code Smell 감지 항목**:
- Long Method, Large Class, Primitive Obsession, Long Parameter List
- Feature Envy, Inappropriate Intimacy, Message Chains
- Duplicate Code, Dead Code, Speculative Generality
- Switch Statements (다형성 누락 가능성)

**리팩토링 기법 적용**:
- Extract Method: 의도를 드러내는 이름으로 코드 조각 추출
- Replace Magic Number with Symbolic Constant
- Decompose Conditional, Guard Clauses로 중첩 조건 제거
- Introduce Parameter Object로 긴 파라미터 목록 개선
- 네이밍: 변수=명사, 함수=동사, 불리언=질문형, 클래스=명사

### 18.3 PR Reviewer (`/review-pr`, `/resolve-reviews`)

GitHub PR을 자동으로 리뷰하고 인라인 코멘트를 게시하는 플러그인.

**사용법**:
- `/pr-reviewer:review-pr <PR_URL>` — PR 리뷰 실행 (3개 병렬 에이전트: 버그/보안/코드품질)
- `/pr-reviewer:resolve-reviews [PR_URL 또는 번호]` — PR의 미해결 리뷰 코멘트 분석, 코드 수정, 답글 게시

**리뷰 워크플로우**: PR 정보 수집 → 워크트리 생성 → 기존 리뷰 확인/resolve → 병렬 코드 분석 → 결과 정리 → 사용자 승인 → GitHub 게시

**심각도**: Bug > Warning > Minor > Nit

**리뷰 코멘트 해결 카테고리**: code_change, question, already_done, disagree, unclear

### 18.4 Codex Reviewer (자동 Hook)

파일 수정(Write/Edit) 시 OpenAI Codex가 **자동으로** 코드 리뷰하는 PostToolUse hook.

**동작**: Write/Edit 도구 사용 후 → Codex가 코드 리뷰 → 이슈 발견 시 피드백 / LGTM 시 조용히 통과

**대상 확장자**: ts, tsx, js, jsx, py, go, rs, java, kt, swift 등

**의존성** (설치 완료):
- jq 1.8.1 (winget)
- Codex CLI 0.98.0 (`npm install -g @openai/codex`)
- **OPENAI_API_KEY 설정 필요** — `codex login` 또는 환경변수로 설정

### 18.5 적용 시점

| 시점 | 적용 방법 |
|------|-----------|
| **새 코드 작성** | Clean Architecture + Kent Beck 원칙 기본 적용, Codex 자동 리뷰 |
| **기존 코드 수정** | 수정 범위 내에서 smell 발견 시 함께 개선 (Boy Scout Rule) |
| **코드 리뷰 요청** | `/clean-architecture` 또는 `/kent-beck-style` skill 실행 |
| **PR 리뷰** | `/pr-reviewer:review-pr <PR_URL>` 실행 |
| **PR 리뷰 코멘트 해결** | `/pr-reviewer:resolve-reviews` 실행 |
| **기능 완료 후** | 자체 점검 — 의존성 방향, 중복, 네이밍, 복잡도 확인 |
