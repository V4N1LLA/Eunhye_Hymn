# CLAUDE.md - Eunhye Hymn 프로젝트 컨텍스트

> 이 파일은 Claude Code가 프로젝트를 빠르게 파악하고 작업할 수 있도록 작성된 종합 레퍼런스입니다.
> 마지막 업데이트: 2026-02-20

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
│   ├── api/              # Spring Boot 백엔드 (Java 17, Gradle) ← MVP 완료 (29 UseCase)
│   │   └── Dockerfile    # Multi-stage (JDK build → JRE run + curl for healthcheck)
│   ├── admin/            # React + Vite + TypeScript 관리자 웹  ← 8페이지 완료
│   │   ├── Dockerfile    # Multi-stage (Node build → Nginx serve)
│   │   └── nginx.conf    # 정적 파일 serve + /api/v1 프록시 + SPA fallback
│   └── mobile/           # Flutter 모바일 앱 (MVP: 로그인/목록/상세/메모/히스토리)
├── infra/
│   ├── docker/           # 로컬 Docker Compose (PostgreSQL + LocalStack + API)
│   └── aws/              # AWS Staging 인프라 (아래 상세)
│       ├── main.tf                   # Provider (AWS ~>5.0), local backend
│       ├── variables.tf              # 입력 변수 정의
│       ├── terraform.tfvars.example  # 사용자용 변수 템플릿
│       ├── vpc.tf                    # VPC, 퍼블릭 서브넷 2개, IGW, 라우트 테이블
│       ├── security.tf               # EC2 SG (80/22), RDS SG (5432 from EC2)
│       ├── rds.tf                    # RDS PostgreSQL db.t3.micro (Free Tier)
│       ├── s3.tf                     # S3 버킷 + public read + CORS
│       ├── ecr.tf                    # ECR 레포 2개 (api, admin) + lifecycle
│       ├── ec2.tf                    # EC2 t2.micro + EIP + IAM Role + user_data
│       ├── monitoring.tf             # SNS 알림 + CloudWatch 알람/로그/대시보드
│       ├── outputs.tf                # EC2 IP, RDS 엔드포인트, ECR URL, SNS ARN
│       ├── docker-compose.prod.yml   # 프로덕션 컨테이너 (api + nginx)
│       ├── docker-compose.prod.awslogs.yml # CloudWatch 로그 오버레이 (옵션)
│       ├── deploy.sh                 # ECR 로그인 → pull → up -d
│       ├── .env.example              # 환경변수 템플릿
│       └── .gitignore                # tfstate, .terraform 제외
├── docs/                 # 프로젝트 문서 (요구사항, 아키텍처, API 계약 등)
├── .github/workflows/
│   ├── api-ci.yml        # API 테스트 (PR + develop push)
│   ├── admin-ci.yml      # Admin 타입체크 + 빌드 (PR + develop push)
│   ├── mobile-ci.yml     # Mobile lint/test (PR + develop push)
│   ├── mobile-release-check.yml # Mobile Android release APK 빌드 검증 + artifact
│   ├── mobile-store-release.yml # Mobile store release readiness (manual: android/ios)
│   └── deploy-staging.yml # Staging 자동 배포 (develop push)
├── CLAUDE.md             # 이 파일
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
| Mobile | Flutter + Dart | 3.24+ / 3.4+ |
| CI/CD | GitHub Actions | - |
| IaC | Terraform | ~> 5.0 (AWS provider) |
| Container | Docker + Nginx | - |

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

### 4.3 Mobile 앱 (로컬)

```powershell
# 저장소 루트에서 최초 1회 (Flutter SDK 자동 설치 + 버전 확인)
.\scripts\flutterw.ps1 --version

cd apps/mobile
..\..\scripts\flutterw.ps1 pub get
..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1
```

실기기에서는 `10.0.2.2` 대신 로컬 서버 IP를 사용한다.

### 4.4 필수 환경변수 (.env.example 참조)

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
| `EVENT_EXPORT_JOB_RETENTION_DAYS` | 비동기 export 결과 보관 일수 | - | `7` |
| `EVENT_EXPORT_JOB_CLEANUP_CRON` | 비동기 export 정리 스케줄(cron) | - | `0 15 3 * * *` |
| `EVENT_EXPORT_JOB_CLEANUP_ZONE` | 비동기 export 정리 스케줄 타임존 | - | `UTC` |

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
│   └── usecases/       # 비즈니스 로직 Use Cases (29개)
├── presentation/
│   ├── controllers/    # REST 컨트롤러 (11개)
│   └── dto/            # Request/Response DTOs
├── infrastructure/
│   ├── persistence/    # JPA Repository Adapters (10개)
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
| POST | `/auth/social` | 없음 | 소셜 로그인 (Kakao) |
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

### 7.7 감사 로그 - 관리자 (`AdminEventController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/admin/events` | ADMIN | 이벤트 로그 조회 + 최근 N일 이벤트 타입 집계 (`summaryDays` 1~90) |
| GET | `/admin/events/export` | ADMIN | 이벤트 로그 CSV 내보내기 |
| POST | `/admin/events/export-jobs` | ADMIN | 비동기 대용량 CSV 작업 생성 (202 Accepted) |
| GET | `/admin/events/export-jobs/{jobId}` | ADMIN | 비동기 CSV 작업 상태 조회 |
| GET | `/admin/events/export-jobs/{jobId}/download` | ADMIN | 완료된 비동기 CSV 다운로드 |
| GET | `/admin/events/export-jobs/metrics` | ADMIN | 비동기 export 운영 지표(실패율/처리시간/정리량) 조회 |

### 7.8 사용자 개인 (`MeController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/me/profile` | 필요 | 프로필 → `{userId, role}` |
| GET | `/me/favorites/{hymnId}` | 필요 | 즐겨찾기 상태 조회 → `FavoriteResponse` |
| POST | `/me/favorites/{hymnId}` | 필요 | 즐겨찾기 토글 → `FavoriteResponse` |
| GET | `/me/hymns/{hymnId}/note` | 필요 | 메모 조회 → `NoteResponse` |
| PUT | `/me/hymns/{hymnId}/note` | 필요 | 메모 저장 (NoteRequest) |
| GET | `/me/history` | 필요 | 최근 본 찬양 → `List<HistoryItemResponse>` |

### 7.9 이벤트 (`EventController`)

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| POST | `/events` | 필요 | 이벤트 기록 (단건 또는 배열, JsonNode 파싱) |

### 7.10 시스템

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| GET | `/ping` | 없음 | `{ "ok": true }` |
| GET | `/actuator/health` | 없음 | Spring Actuator 헬스 |

---

## 8. Use Cases (29개)

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
| `GetFavoriteUseCase` | `get(userId, hymnId)` | 즐겨찾기 상태 조회 (없으면 false) |
| `ToggleFavoriteUseCase` | `toggle(userId, hymnId)` | 즐겨찾기 토글 (없으면 true, 있으면 반전) |
| `GetHistoryUseCase` | `getHistory(userId)` | lastOpenedAt desc 정렬 |
| `SocialLoginUseCase` | `login(provider, token, inviteCode?)` | Kakao 토큰 검증, 신규 사용자는 초대코드 필요 |
| `RefreshTokenUseCase` | `refresh(rawRefreshToken)` | 해시 검증, 만료 확인, 토큰 회전 |
| `LogoutUseCase` | `logout(rawRefreshToken)` | revokedAt 설정 |
| `DevLoginUseCase` | `login(userId, role, displayName)` | 사용자 생성/갱신, 토큰 발급 |
| `RecordEventsUseCase` | `record(userId, List<EventInput>)` | EventType/PartType 검증 후 저장 |
| `AdminListUsersUseCase` | `listAll()` | 전체 사용자 목록 조회 |
| `AdminUpdateUserUseCase` | `update(userId, role, status)` | 사용자 역할/상태 변경 |
| `AdminListEventsUseCase` | `execute(query)` | 관리자 이벤트 로그 조회 + 이벤트 타입 집계 |
| `AdminEventExportJobUseCase` | `create/get/process/getDownload` | 관리자 비동기 대용량 CSV 작업 생성/상태/처리/다운로드 |
| `CleanupEventExportJobsUseCase` | `cleanup(now)` | 완료/실패 비동기 export 작업 보관기한 정리 |
| `GetEventExportOpsMetricsUseCase` | `execute(windowDays)` | 비동기 export 운영 지표(실패율/처리시간/정리량) 집계 |
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
| `V7__events_admin_indexes.sql` | 관리자 이벤트 조회/CSV 최적화 인덱스 3개 추가 |
| `V8__event_export_jobs.sql` | 비동기 대용량 CSV 작업 테이블(event_export_jobs) 추가 |
| `V9__event_export_job_cleanup_runs.sql` | 비동기 export 정리 실행 이력 테이블(event_export_job_cleanup_runs) 추가 |

### 주요 인덱스
- `idx_refresh_tokens_user_id` ON refresh_tokens(user_id)
- `idx_assets_hymn_id` ON assets(hymn_id)
- `idx_user_hymn_state_user_opened` ON user_hymn_state(user_id, last_opened_at)
- `idx_events_user_created` ON events(user_id, created_at)
- `idx_events_created_at_desc` ON events(created_at DESC)
- `idx_events_event_type_created` ON events(event_type, created_at DESC)
- `idx_events_hymn_created` ON events(hymn_id, created_at DESC)

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
| `SocialLoginApiTest` | 소셜 로그인 API (Kakao, 초대코드 검증, 기존 사용자) |
| `FlywayRepositoryIntegrationTest` | DB 마이그레이션 통합 |
| `GetHistoryUseCaseTest` | 히스토리 Use Case 단위 |
| `CleanupEventExportJobsUseCaseTest` | 비동기 export 정리 Use Case 단위 |
| `CleanupEventExportJobsUseCaseIntegrationTest` | 비동기 export 정리 정책 통합 |
| `GetEventExportOpsMetricsUseCaseTest` | 비동기 export 운영 지표 Use Case 단위 |
| `AdminUserApiTest` | 사용자 관리 API (목록, 역할/상태 변경, 권한 검사) |
| `AdminEventApiTest` | 관리자 이벤트 API (로그 조회, 집계, 페이지네이션, 동기/비동기 CSV export, 접근 제어 검증) |
| `AdminInviteCodeApiTest` | 초대코드 관리 API (생성, 목록, 비활성화, 검증) |

- **테스트 DB**: H2 인메모리 (test 프로필)
- **전체 테스트 통과** 확인 (2026-02-14 기준)

---

## 13. 프론트엔드 현황 (Admin + Mobile)

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
| `src/api/auth.ts` | **소셜 로그인** (Kakao), 초대코드 검증, Dev 로그인, 토큰 갱신, 로그아웃 API (`PUBLIC_AUTH_OPTIONS`로 인증 없는 요청 구분) |
| `src/api/hymns.ts` | 찬양 목록/생성/수정/**삭제**/상세 API |
| `src/api/adminAssets.ts` | 에셋 presign/confirm/삭제 API (공통 클라이언트 사용) |
| `src/api/adminUsers.ts` | 사용자 목록/역할·상태 변경 API |
| `src/api/adminEvents.ts` | 관리자 이벤트 로그/집계 조회 API |
| `src/api/adminInviteCodes.ts` | 초대코드 목록/생성/비활성화 API |
| `src/auth/AuthContext.tsx` | AuthProvider + `useAuth()` 훅, JWT 파싱, sessionStorage 기반 토큰 관리, **`loginWithSocial`** + `setTokensAndUser` 공통 헬퍼 |
| `src/auth/tokenStore.ts` | Admin 토큰 저장소(sessionStorage), 구 localStorage 토큰 마이그레이션/정리 |
| `src/auth/ProtectedRoute.tsx` | 미인증 시 `/login` redirect |
| `src/components/Layout.tsx` | 사이드바(찬양 관리, 에셋 업로드, 사용자 관리, 초대코드 관리, 감사 로그/분석) + 로그아웃 |
| `src/pages/LoginPage.tsx` | **Kakao 소셜 로그인** + 초대코드 입력 + 접이식 Dev Login |
| `src/pages/HymnListPage.tsx` | 찬양 목록 테이블 (번호, 제목, 태그, 활성 상태) + 검색/필터 + 활성화 토글 + **삭제** |
| `src/pages/HymnCreatePage.tsx` | 찬양 생성 폼 (title, number, tags, enabled) |
| `src/pages/HymnEditPage.tsx` | 찬양 수정 폼 + 에셋 목록(URL 링크, 삭제) + 임베디드 업로드 + 업로드 후 자동 새로고침 + **찬양 삭제 버튼** |
| `src/pages/AdminAssetUploadPage.tsx` | 에셋 업로드 3단계 UI (Tailwind 스타일, hymnId/onConfirmed props 지원) |
| `src/pages/UserListPage.tsx` | 사용자 목록 테이블 + 역할/상태 변경 + 검색/필터 |
| `src/pages/InviteCodePage.tsx` | 초대코드 목록 + 생성/비활성화 + **만료일 설정 및 표시** |
| `src/pages/AdminEventPage.tsx` | 감사 로그 필터/페이지네이션 조회 + 집계 기간(1~90일) 커스텀 + CSV 내보내기 |
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
| `/events` | AdminEventPage | 필요 |

### Mobile 앱 (Flutter MVP)

#### 기술 스택
- Flutter (Material 3) + Dart
- `http` 기반 API 클라이언트
- `shared_preferences` 토큰 저장
- API Base URL: `--dart-define=API_BASE_URL=...` 주입

#### 구현된 기능

| 파일 | 역할 |
|------|------|
| `lib/src/core/network/api_client.dart` | 공통 HTTP 클라이언트, API envelope 파싱, **401 시 자동 토큰 갱신 후 재시도** |
| `lib/src/core/storage/token_storage.dart` | Access/Refresh 토큰 로컬 저장/조회/삭제 |
| `lib/src/features/auth/auth_repository.dart` | 소셜/Dev 로그인, 프로필 조회, 로그아웃 |
| `lib/src/features/auth/login_page.dart` | 소셜 SDK 직접 로그인 + 수동 토큰 fallback + Dev 로그인 UI |
| `lib/src/features/hymn/hymn_repository.dart` | 찬양 목록/상세, 즐겨찾기, 메모, 히스토리 API |
| `lib/src/features/hymn/hymn_list_page.dart` | 찬양 목록 + 검색/태그 필터 + 빈 상태/오류 배너 UX |
| `lib/src/features/hymn/hymn_detail_page.dart` | 찬양 상세 + PNG 에셋 표시 + 즐겨찾기 + 메모 저장 |
| `lib/src/features/history/history_page.dart` | 최근 열람 히스토리 + 검색/기간 필터 + 빈 상태/오류 배너 UX |
| `lib/src/app.dart` | 앱 부트스트랩, 세션 복구, 탭 네비게이션(찬양/히스토리), 로그아웃 |

#### 화면/네비게이션

| 화면 | 경로(개념) | 설명 |
|------|------------|------|
| LoginPage | 앱 시작 | 소셜 SDK/수동 토큰 fallback/Dev 로그인 |
| Home(탭) | 로그인 후 기본 | 찬양 목록 탭 + 최근 열람 탭 |
| HymnDetailPage | 목록/히스토리 진입 | 상세 정보, 에셋, 메모, 즐겨찾기 |

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

### Mobile CI (`mobile-ci.yml`)
- **트리거**: PR 및 develop push (apps/mobile/** 변경 시)
- **환경**: ubuntu-latest, Flutter stable
- **캐시**: Flutter SDK + Pub cache
- **실행**: `flutter pub get` → `flutter analyze` → `flutter test`

### Mobile Release Check (`mobile-release-check.yml`)
- **트리거**: PR 및 develop push (apps/mobile/** 변경 시) + `workflow_dispatch`
- **환경**: ubuntu-latest, Java 17 + Flutter stable
- **실행**: `flutter pub get` → `flutter build apk --release --dart-define=API_BASE_URL=...`
- **산출물**: `app-release.apk`를 GitHub Actions artifact로 업로드

### Mobile Store Release Readiness (`mobile-store-release.yml`)
- **트리거**: `workflow_dispatch` (manual)
- **입력**: `target=android|ios|both`, `android_distribution_mode=build_only|play_upload`, `ios_distribution_mode=build_only|testflight`, `api_base_url`
- **Android 경로**:
  - signed AAB 빌드(`flutter build appbundle --release`)
  - `android_distribution_mode=play_upload` 시 Google Play 업로드
  - required secrets: `MOBILE_ANDROID_KEYSTORE_BASE64`, `MOBILE_ANDROID_KEY_ALIAS`, `MOBILE_ANDROID_KEY_PASSWORD`, `MOBILE_ANDROID_STORE_PASSWORD`
- **iOS 경로**:
  - `ios_distribution_mode=build_only`: release no-codesign 빌드(`flutter build ios --release --no-codesign`)
  - `ios_distribution_mode=testflight`: signed IPA 빌드 + TestFlight 업로드
- **목적**: 스토어 업로드 전 빌드/서명 readiness 검증
- **운영 스크립트**:
  - `scripts/mobile-store-preflight.ps1`
  - `scripts/mobile-store-cycle.ps1`
  - 실행 로그: `docs/mobile-store-release-log.md`

### Deploy Staging (`deploy-staging.yml`)
- **트리거**: develop push + `workflow_dispatch`
- **Jobs**: `preflight-secrets` → `test-api` → `check-admin` → `build-and-push` → `deploy`
- **단계**: Secrets 유효성 검사 → API 테스트 → Admin 타입체크+빌드 → Docker 이미지 빌드 & ECR push (api:latest + admin:latest) → EC2 SSH 배포 (compose + 선택적 awslogs 오버레이 + deploy.sh) → 헬스체크
- **필수 Secrets**:

| Secret | 설명 |
|--------|------|
| `AWS_ACCESS_KEY_ID` | AWS IAM Access Key |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM Secret Key |
| `AWS_REGION` | AWS 리전 (ap-northeast-2) |
| `ECR_REGISTRY` | ECR 레지스트리 URL (`terraform output ecr_registry`) |
| `EC2_HOST` | EC2 Elastic IP (`terraform output ec2_public_ip`) |
| `EC2_SSH_KEY` | EC2 SSH 프라이빗 키 (PEM) |
| `DEPLOY_ENV_FILE` | `.env` 파일 전체 내용 (DB, JWT, S3 등) |
| `ENABLE_AWSLOGS` | CloudWatch 로그 전송 활성화 여부 (`true` 시 활성화, 미설정 시 기본 `false`) |

- **수동 검증 실행**: `workflow_dispatch`로 브랜치 기준 배포 검증 가능 (`enable_awslogs` 입력)
- **리허설 자동 실행**: `scripts/staging-rehearsal.ps1`로 preflight + workflow_dispatch + run 대기 + 로그 기록 자동화 (`-AutoLogin` 지원)

---

## 15. AWS Staging 인프라

### 15.1 아키텍처

```
[사용자] → http://<EC2-IP>
                │
         ┌──────┴──────┐
         │   Nginx     │  ← Admin 정적 파일 serve + API 리버스 프록시
         │  (port 80)  │
         └──────┬──────┘
                │ /api/v1 → proxy_pass http://api:8080
         ┌──────┴──────┐
         │  Spring Boot│  ← ECR 이미지, 환경변수로 RDS/S3 연결
         │  (port 8080)│
         └──────┬──────┘
                │
         ┌──────┴──────┐
         │ RDS Postgres│  ← db.t3.micro, EC2 SG에서만 접근
         │  (port 5432)│
         └─────────────┘

S3 버킷 ← 에셋(PNG/MIDI), public read + CORS
ECR     ← Docker 이미지 (api, admin), lifecycle 최근 5개 유지
```

### 15.2 Terraform 리소스

| 리소스 | 스펙 | Free Tier |
|--------|------|-----------|
| VPC | 10.0.0.0/16 + 퍼블릭 서브넷 2개 + IGW | 무료 |
| EC2 | t2.micro (1 vCPU, 1GB), Amazon Linux 2023, 20GB gp3 | 750시간/월 (12개월) |
| Elastic IP | 1개, EC2에 연결 | 실행 중 무료 |
| RDS | db.t3.micro, PostgreSQL 15.8, 20GB gp2, 단일 AZ | 750시간/월 (12개월) |
| S3 | 표준, public read, CORS 허용 | 5GB |
| ECR | 프라이빗 레포 2개 (api, admin) | 500MB |

### 15.3 EC2 구성

- **IAM Role**: ECR pull (`ecr:GetAuthorizationToken`, `ecr:BatchGetImage` 등) + S3 access (`s3:PutObject/GetObject/DeleteObject/ListBucket`)
- **user_data**: Docker + Docker Compose 자동 설치, `/home/ec2-user/app` 디렉토리 생성
- **보안 그룹**: HTTP(80) 전체 허용, SSH(22) `allowed_ssh_cidrs` 변수로 제한 가능

### 15.4 배포 플로우

```
develop push → GitHub Actions
  1. test-api (Gradle test)
  2. check-admin (tsc + vite build)
  3. build-and-push (Docker build → ECR push, :latest + :sha 태그)
  4. deploy (SCP compose + awslogs 오버레이 + deploy.sh → SSH deploy.sh 실행)
     └─ ECR login → docker compose pull → up -d → image prune
```

### 15.5 프로덕션 Docker Compose (`docker-compose.prod.yml`)

| 서비스 | 이미지 | 포트 | 의존성 |
|--------|--------|------|--------|
| `api` | `${ECR_REGISTRY}/eunhye-hymn/api:latest` | 8080 (internal) | - |
| `nginx` | `${ECR_REGISTRY}/eunhye-hymn/admin:latest` | 80 → 80 | api (healthy) |

- `api`는 `.env` 파일에서 DB_URL, JWT_SECRET 등 환경변수 로드
- `nginx`는 `nginx.conf`에서 `/api/v1` → `http://api:8080` 프록시
- 기본 로깅 드라이버는 `json-file`; `ENABLE_AWSLOGS=true`일 때 `docker-compose.prod.awslogs.yml` 오버레이로 CloudWatch 로그 전송 활성화
- `deploy.sh`는 awslogs 드라이버 미지원/재기동 실패 시 기본 로깅으로 자동 fallback 후 재시도

### 15.6 Nginx 설정 (`apps/admin/nginx.conf`)

- `/api/v1/` → `proxy_pass http://api:8080` (리버스 프록시)
- `/actuator/` → `proxy_pass http://api:8080` (헬스체크 프록시)
- `/` → `try_files $uri $uri/ /index.html` (SPA fallback)
- 정적 에셋 1년 캐시, gzip 압축

### 15.7 모니터링/알림 (`monitoring.tf`)

**SNS 알림**: `alert_email` 변수 설정 시 이메일 구독 자동 생성 (구독 확인 필요)

**CloudWatch 알람** (5개):

| 알람 | 조건 | 기간 |
|------|------|------|
| EC2 CPU High | CPU > 80% | 5분 × 2회 |
| EC2 Status Check | StatusCheckFailed > 0 | 5분 × 2회 |
| RDS CPU High | CPU > 80% | 5분 × 2회 |
| RDS Storage Low | FreeStorage < 5GB | 5분 × 1회 |
| RDS Connections High | Connections > 30 | 5분 × 2회 |

**CloudWatch 로그**: `docker-compose.prod.awslogs.yml` 오버레이 + `ENABLE_AWSLOGS=true` 설정 시 API/Nginx 컨테이너 로그를 CloudWatch로 전송 (14일 보관, 멀티라인 패턴 적용)

**CloudWatch 대시보드**: EC2 CPU/네트워크, RDS CPU/연결수/스토리지, API 에러 로그, Nginx 5xx 로그

### 15.8 초기 설정 (사용자 작업)

1. AWS CLI + Terraform 설치
2. `aws ec2 create-key-pair --key-name eunhye-staging` → PEM 저장
3. `cp terraform.tfvars.example terraform.tfvars` → 값 편집
4. `terraform init && terraform plan && terraform apply`
5. GitHub Secrets 등록 (terraform output 값 사용)
6. develop push → 자동 배포

상세 가이드: `infra/aws/README.md`, `docs/admin/aws-free-tier-onboarding.md`

---

## 16. Git 브랜치

| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 릴리스 |
| `develop` | 개발 통합 (기본 기준 브랜치) |
| `staging` | 스테이징 배포 |

---

## 17. 현재 진행 상태 및 남은 작업

### 완료

**백엔드 API (29 UseCase, 11 Controller)**
- 찬양 CRUD + 삭제 (cascade: 에셋/메모/상태/이벤트)
- S3 에셋 관리 (presign/confirm/삭제)
- JWT 인증 + 소셜 로그인 (Kakao) + 토큰 회전
- DB 기반 초대코드 관리 (CRUD + 검증 + 원자적 사용 횟수 증가)
- 사용자 관리 (역할/상태 변경)
- 멤버 기능 (즐겨찾기, 메모, 히스토리, 이벤트 기록)
- 비동기 export 결과 정리 배치 (완료/실패 작업 기본 7일 보관 후 정리)
- 비동기 export 운영 지표 API (`/admin/events/export-jobs/metrics`) + cleanup 실행 이력 기록
- DB 스키마 Flyway 마이그레이션 (V1~V9)
- 테스트 15개 파일 전체 통과 (SocialLoginApiTest 포함)

**Admin 프론트엔드 (8페이지)**
- 찬양 목록/생성/수정/삭제 + 검색/필터 + 활성화 토글
- 에셋 업로드 3단계 (presign/upload/confirm) + 삭제
- 사용자 관리 (역할/상태 변경, 검색/필터)
- 초대코드 관리 (생성/비활성화/만료일 설정)
- 감사 로그/분석 화면 (`GET /admin/events`) - 필터/페이지네이션 조회 + 집계 기간(1~90일) 커스텀 + 동기 CSV 내보내기 + 비동기 대용량 CSV 작업/다운로드 + 운영 지표 카드(실패율/처리시간/정리량)
- 소셜 로그인 UI (Kakao) + 초대코드 입력 플로우
- Access Token 자동 갱신 (401 → refresh → 재시도, mutex 패턴)
- 인증 컨텍스트 (`loginWithSocial`, `setTokensAndUser`), 공통 API 클라이언트, 사이드바 레이아웃
- Dev Login (개발용, 접이식)

**Mobile 앱 (Flutter MVP)**
- 소셜 SDK 직접 로그인 (Kakao 모바일, Kakao 웹은 토큰 입력 fallback) + Dev 로그인
- 찬양 목록/검색 + 상세 조회
- 목록/히스토리 UX 개선 (검색어 즉시 지우기, 태그/기간 필터, 빈 상태 가이드, 소프트 에러 배너)
- PNG 에셋 표시 + 메모 조회/저장 + MIDI 에셋 앱 내 재생 UX
- 즐겨찾기 토글 + 최근 열람 히스토리
- 오프라인 캐시 fallback + 오프라인 변경(메모/즐겨찾기) 동기화 큐
- Access Token 자동 갱신 (401 → refresh → 재시도)
- 토큰 로컬 저장(shared_preferences) + 세션 복구

**인프라/CI**
- Docker Compose (PostgreSQL + LocalStack + API)
- API Dockerfile (multi-stage)
- Admin Dockerfile (multi-stage: Node build + Nginx serve)
- Nginx 설정 (Admin 정적 파일 serve + API 리버스 프록시 + SPA fallback)
- CI/CD (API 테스트 + Admin 빌드/타입체크 + Mobile lint/test + Staging 자동 배포)

**AWS 인프라 (Terraform)**
- VPC + 퍼블릭 서브넷 2개 + IGW + 라우트 테이블
- EC2 t2.micro (Free Tier) + Elastic IP + IAM Role (ECR pull + S3 access)
- RDS PostgreSQL db.t3.micro (Free Tier, EC2 SG에서만 접근)
- S3 버킷 (에셋 저장, public read + CORS)
- ECR 레포지토리 2개 (api, admin) + lifecycle policy
- 보안 그룹 (EC2: 80/22, RDS: 5432 from EC2 only)
- 프로덕션 Docker Compose + 배포 스크립트
- GitHub Actions 자동 배포 워크플로우 (develop push → ECR push → EC2 deploy)

**모니터링/알림 (CloudWatch)**
- SNS 토픽 + 이메일 구독 (alert_email 변수)
- CloudWatch 알람 5개 (EC2 CPU/StatusCheck, RDS CPU/Storage/Connections)
- CloudWatch 로그 그룹 2개 (API, Nginx) + 선택적 Docker awslogs 오버레이 (기본 json-file)
- CloudWatch 대시보드 (EC2/RDS 메트릭 + 에러 로그 쿼리)
- EC2 IAM 정책 (CloudWatch Logs 전송 권한, account ID 스코핑)

**문서/준비도 점검**
- 문서 정합성 및 배포 준비도 점검 리포트 추가
  - `docs/deployment-readiness-audit.md`
- 기준 문서 최신화
  - `docs/current-usable-scope.md`
  - `docs/mobile/README.md`
  - `README.md`
- 최신 기준점 문서 동기화 (2026-02-17)
  - `docs/current-usable-scope.md` (`ee49a0e` 기준 커밋/근거 PR/실행 run 반영)
  - `docs/deployment-readiness-audit.md` (최신 Actions 실행 근거/잔여 리스크 갱신)
- 개발 변경 이력 동기화
  - `docs/changelog-dev.md`
- 스테이징 실가동 체크리스트/런북 동기화
  - `docs/staging-smoke-checklist.md`
  - `docs/runbook.md`
- 스테이징 운영 사이클 로그 문서 추가
  - `docs/staging-smoke-log.md`
- 스테이징 피드백 루프 체크리스트 추가
  - `docs/staging-feedback-checklist.md`
- 스테이징 리허설 실행 로그 문서 추가
  - `docs/staging-rehearsal-log.md`
- 2026-02-14 실리허설/롤백/복구 실행 기록 반영
  - `22010284332` (develop)
  - `22010387328` (rollback rehearsal)
  - `22010470389` (develop restore)
- preflight 스크립트 실패 원인 가시성 보강
  - `scripts/staging-preflight.ps1` (`aws sts get-caller-identity` 실패 원인 상세 출력)

### 미완료 (우선순위순)

**1. 스테이징 실가동 전환**
- Terraform 실제 적용 및 AWS 리소스 활성화
- GitHub Actions Secrets/권한 상태 주기 점검 및 preflight 통과 환경 유지
- EC2 접근 권한/배포 계정 권한 점검 및 운영 체크리스트 정기 실행
- Admin/Mobile 런타임 수동 스모크 정례 실행
- 단계별 온보딩: `docs/admin/aws-free-tier-onboarding.md`
- 보조 스크립트:
  - `scripts/staging-preflight.ps1`
  - `scripts/staging-sync-secrets.ps1`
  - `scripts/staging-rehearsal.ps1`
  - `scripts/staging-ops-cycle.ps1`
- IAM 정책 샘플:
  - `infra/aws/terraform-deployer-iam-policy.json`
- 진행 상태는 preflight 결과(`scripts/staging-preflight.ps1`)와 `gh secret list` 기준으로 최신화한다.
- 최신 점검(2026-02-20): deploy run `22207213127` 기준 preflight/deploy/verify 모두 PASS, 운영 판정 `CONDITIONAL_GO`.

**2. 운영 문서/절차 고도화**
- `docs/runbook.md` + `docs/staging-smoke-checklist.md` + `docs/staging-feedback-checklist.md` 기준으로 롤백/장애 대응 리허설 수행 후 결과 반영
- 배포 후 스모크 테스트 항목과 점검 결과를 `docs/staging-rehearsal-log.md`, `docs/staging-smoke-log.md`에 주기적으로 갱신

**3. 기능 백로그**
- 비동기 export 운영 지표(실패율/처리시간/정리량) 정례화 완료 (2026-02-14)
- 후속: 지표 임계치 기반 알림/대시보드 연동 설계

**4. 모바일 배포 패키징**
- 현재 저장소 기준 실행은 `flutter run -d chrome` 중심
- Android release APK 빌드 검증 워크플로우 추가 완료 (`mobile-release-check.yml`, 2026-02-16)
- Android signed AAB / iOS no-codesign 수동 readiness 워크플로우 추가 완료 (`mobile-store-release.yml`, 2026-02-17)
- 운영 루프 자동화 완료: preflight + dispatch + run watch + 로그 적재(`scripts/mobile-store-*.ps1`)
- Android build-only 실검증 완료: run `22210175274`, `22210322592` 성공
- 남은 과제:
  - Google Play 실제 업로드 모드(`play_upload`) 실행 승인/검증
  - iOS TestFlight 업로드용 시크릿(`MOBILE_IOS_*`) 프로비저닝 후 실검증

---

## 18. 작업 시 주의사항

- **Clean Architecture 준수**: 의존성 방향은 반드시 외부→내부 (infrastructure → application → domain)
- **API 베이스 경로**: 항상 `/api/v1` 접두사 사용
- **환경변수**: JWT_SECRET, INVITE_CODE는 기본값 없음 — 반드시 설정해야 부팅
- **테스트**: H2 인메모리 DB 사용, test 프로필에서 SecurityRequiredProperties 비활성화
- **Gradle**: 시스템 gradle이 아닌 wrapper (`./gradlew`) 사용
- **Windows**: gradlew.bat에서 JAVA_HOME 공백 경로 처리 수정 적용됨
- **에셋 교체**: 동일 (hymnId, type, part) 조합 시 기존 레코드 삭제 후 새로 생성
- **CLAUDE.md 동기화 (필수)**: 모든 PR에 CLAUDE.md 업데이트를 포함할 것. 변경된 기능, 파일, 진행 상태(완료/미완료)를 반영하여 어떤 환경·세션에서든 이 파일만 읽으면 바로 작업을 이어갈 수 있도록 유지한다
- **작업 브랜치**: `develop`에서 `feat/*` 브랜치를 생성하여 작업하고, PR은 `develop`으로 보낸다. `main` merge는 사용자가 명시적으로 요청할 때만 수행
- **자동화 작업 사이클**: 사용자가 작업을 요청하면 아래 전체 사이클을 자동으로 수행한다. 사용자 개입을 최소화하는 것이 목표다
- **사이클 기준 문서**: 반복 설명을 줄이기 위해 `docs/WORK_CYCLE.md`를 단일 기준으로 최신 상태 유지한다
- **기본 완료 기준**: 작업 요청은 기본적으로 `구현 → 검증 → 커밋 → PR 생성`까지 완료한다 (사용자 명시 예외 제외)
- **PR 본문 작성 규칙**: `gh pr create/edit --body` 인라인 문자열보다 `--body-file` 사용을 기본으로 하고, 반영 후 `gh pr view`로 줄바꿈/포맷을 확인한다
- **리뷰 답글 기록 (필수)**: 리뷰 코멘트를 처리할 때는 각 코멘트 thread에 처리 내역을 답글로 남긴다. (수정 내용, 검증 결과, 미반영 사유)

### 18.1 작업 사이클 (한 기능 = 한 사이클)

사용자가 기능/수정을 요청하면 다음 단계를 **자동으로 끝까지** 수행한다:

1. **구현**: `feat/*` 브랜치 생성 → 코드 작성 → 빌드/타입체크 확인
2. **자체 검수**: 코드 리뷰 (Clean Architecture + Kent Beck 원칙), 버그/보안/품질 점검
3. **CLAUDE.md 업데이트**: 변경된 기능·파일·진행 상태 반영
4. **커밋 & Push**: 의미 있는 커밋 메시지, `origin`에 push
5. **PR 생성**: `develop` 대상 PR 생성 (제목 + 요약 + 테스트 계획, 본문은 `--body-file` 사용 후 `gh pr view`로 렌더링 확인)
6. **코드 리뷰 & 리팩토링**: `/pr-reviewer:review-pr` 실행 → 이슈 발견 시 수정/재검증 후 재push, 각 리뷰 코멘트 thread에 처리 내역 답글 작성
7. **다음 작업 추천**: CLAUDE.md 17장 미완료 목록 기반으로 다음 우선순위 작업을 제안

사용자는 최종 결과만 확인하면 된다. 중간에 판단이 필요한 경우에만 질문한다.

---

## 19. 코드 품질 Skill (개발 시 필수 준수)

개발 과정에서 아래 두 skill의 원칙을 항상 적용한다.

### 19.1 Clean Architecture Skill (`/clean-architecture`)

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

### 19.2 Kent Beck Style Skill (`/kent-beck-style`)

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

### 19.3 PR Reviewer (`/review-pr`, `/resolve-reviews`)

GitHub PR을 자동으로 리뷰하고 인라인 코멘트를 게시하는 플러그인.

**사용법**:
- `/pr-reviewer:review-pr <PR_URL>` — PR 리뷰 실행 (3개 병렬 에이전트: 버그/보안/코드품질)
- `/pr-reviewer:resolve-reviews [PR_URL 또는 번호]` — PR의 미해결 리뷰 코멘트 분석, 코드 수정, 답글 게시

**리뷰 워크플로우**: PR 정보 수집 → 워크트리 생성 → 기존 리뷰 확인/resolve → 병렬 코드 분석 → 결과 정리 → 사용자 승인 → GitHub 게시

**심각도**: Bug > Warning > Minor > Nit

**리뷰 코멘트 해결 카테고리**: code_change, question, already_done, disagree, unclear

### 19.4 Codex Reviewer (자동 Hook)

파일 수정(Write/Edit) 시 OpenAI Codex가 **자동으로** 코드 리뷰하는 PostToolUse hook.

**동작**: Write/Edit 도구 사용 후 → Codex가 코드 리뷰 → 이슈 발견 시 피드백 / LGTM 시 조용히 통과

**대상 확장자**: ts, tsx, js, jsx, py, go, rs, java, kt, swift 등

**의존성** (설치 완료):
- jq 1.8.1 (winget)
- Codex CLI 0.98.0 (`npm install -g @openai/codex`)
- **OPENAI_API_KEY 설정 필요** — `codex login` 또는 환경변수로 설정

### 19.5 적용 시점

| 시점 | 적용 방법 |
|------|-----------|
| **새 코드 작성** | Clean Architecture + Kent Beck 원칙 기본 적용, Codex 자동 리뷰 |
| **기존 코드 수정** | 수정 범위 내에서 smell 발견 시 함께 개선 (Boy Scout Rule) |
| **코드 리뷰 요청** | `/clean-architecture` 또는 `/kent-beck-style` skill 실행 |
| **PR 리뷰** | `/pr-reviewer:review-pr <PR_URL>` 실행 |
| **PR 리뷰 코멘트 해결** | `/pr-reviewer:resolve-reviews` 실행 |
| **기능 완료 후** | 자체 점검 — 의존성 방향, 중복, 네이밍, 복잡도 확인 |



