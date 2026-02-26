# Eunhye Hymn 개발 가이드

## 1. 로컬 개발 원칙
- API는 `apps/api`에서 실행하고, DB/S3는 Docker(LocalStack) 또는 로컬 대체를 사용한다.
- 환경변수는 템플릿(`.env.example`, `apps/api/.env.example`)에서 시작한다.
- 인증/운영 기능 변경 시 문서(`README`, `docs/api-contract.md`, `docs/data-model.md`)를 함께 갱신한다.

## 2. 사전 준비
- Java 17+
- Node.js 20+, npm 10+
- PostgreSQL (로컬 또는 Docker)
- Docker Desktop (권장)

## 3. API 로컬 실행
1. 환경 변수 파일 생성:
   ```bash
   cp apps/api/.env.example apps/api/.env
   ```
2. PowerShell에서 환경 변수 로드:
   ```powershell
   Get-Content apps/api/.env | ForEach-Object {
     if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
     $name, $value = $_ -split '=', 2
     Set-Item -Path "Env:$name" -Value $value
   }
   ```
3. PostgreSQL 실행 (`DB_URL` 확인).
4. API 실행:
   ```powershell
   cd apps/api
   .\gradlew.bat bootRun
   ```
5. 헬스 체크:
   ```powershell
   curl http://localhost:8080/api/v1/actuator/health
   ```

필수값이 비어 있으면 부팅 실패:
- `JWT_SECRET`
- `JWT_ACCESS_TTL_SECONDS`
- `JWT_REFRESH_TTL_SECONDS`
- `INVITE_CODE`

## 4. 테스트 실행

### API
```powershell
cd apps/api
.\gradlew.bat test --no-daemon --stacktrace
```

### Admin
```powershell
cd apps/admin
npm install
npm run test
npx tsc --noEmit
npm run build
```

### Mobile
```powershell
.\scripts\flutterw.ps1 analyze apps/mobile/lib
cd apps/mobile
..\..\scripts\flutterw.ps1 test
```

## 5. 주요 환경변수 그룹

### 5.1 DB/인증
- `DB_URL`, `DB_USER`, `DB_PASS`
- `JWT_SECRET`, `JWT_ACCESS_TTL_SECONDS`, `JWT_REFRESH_TTL_SECONDS`
- `INVITE_CODE`
- `ADMIN_EMAILS`, `ADMIN_KAKAO_SUBJECTS`, `ADMIN_ENFORCE_ADMIN_ONLY`
- `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`

### 5.2 SMS 인증
- `SMS_CODE_LENGTH`, `SMS_EXPIRES_SECONDS`, `SMS_COOLDOWN_SECONDS`, `SMS_MAX_ATTEMPTS`
- `SMS_TWILIO_ENABLED`
- `SMS_TWILIO_ACCOUNT_SID`, `SMS_TWILIO_AUTH_TOKEN`, `SMS_TWILIO_FROM_NUMBER`
- `SMS_TWILIO_MESSAGE_TEMPLATE`

### 5.3 스토리지
- `S3_BUCKET`, `S3_REGION`, `S3_ENDPOINT`, `S3_PUBLIC_BASE_URL`
- `S3_PRESIGN_EXPIRES_MINUTES`

### 5.4 이벤트 export 운영
- `EVENT_EXPORT_JOB_RETENTION_DAYS`
- `EVENT_EXPORT_JOB_CLEANUP_CRON`, `EVENT_EXPORT_JOB_CLEANUP_ZONE`
- `EVENT_EXPORT_JOB_RECOVERY_*`
- `EVENT_EXPORT_JOB_ALERT_*`

### 5.5 AI 추천 (Gemini Flash-Lite)
- `AI_GEMINI_ENABLED`, `AI_GEMINI_API_KEY`, `AI_GEMINI_MODEL`
- `AI_GEMINI_CONNECT_TIMEOUT_SECONDS`, `AI_GEMINI_READ_TIMEOUT_SECONDS`
- `AI_GEMINI_TEMPERATURE`, `AI_GEMINI_MAX_OUTPUT_TOKENS`
- `AI_RECOMMEND_MAX_CANDIDATE_HYMNS`, `AI_RECOMMEND_MAX_RESULTS`, `AI_RECOMMEND_MAX_SITUATION_CHARS`
- 운영 관측 지표(Micrometer): `ai_recommend_requests_total`, `ai_recommend_latency_seconds`, `ai_recommend_fallback_total`, `ai_recommend_candidate_count`, `ai_recommend_response_items`

### 5.6 Auth/AI 운영 알림 임계값
- `OPS_AUTH_AI_ALERT_ENABLED`
- `OPS_AUTH_AI_ALERT_MIN_AUTH_REQUESTS`, `OPS_AUTH_AI_ALERT_MIN_AI_REQUESTS`
- `OPS_AUTH_AI_ALERT_MAX_AUTH_FAILURE_RATE_PERCENT`
- `OPS_AUTH_AI_ALERT_MAX_AI_FAILURE_RATE_PERCENT`
- `OPS_AUTH_AI_ALERT_MAX_AI_FALLBACK_RATE_PERCENT`
- `OPS_AUTH_AI_ALERT_AUTH_SCOPES` (기본: `admin_login,social_login,user_signup,user_login,invite_validate`)
- `OPS_AUTH_AI_ALERT_CRON`, `OPS_AUTH_AI_ALERT_ZONE`
- 기본 판정(10분 주기):
  - 인증 실패율(`auth_requests_total`) > 5% and 샘플 >= 100 -> WARN 로그
  - AI 실패율(`ai_recommend_requests_total`) > 1% and 샘플 >= 30 -> WARN 로그
  - AI fallback 비율(`ai_recommend_fallback_total`) > 5% and 샘플 >= 30 -> WARN 로그

## 6. Flyway/테스트 DB
- `V1__baseline.sql`은 베이스라인용 빈 파일이다.
- 실제 스키마는 `V2__init.sql` 이후 마이그레이션에서 생성한다.
- 테스트는 H2 인메모리 DB + Flyway를 사용한다.

## 7. Gradle Wrapper 정책
- 로컬/CI 모두 Wrapper 사용: `./gradlew` (`gradlew.bat`)
- 시스템 Gradle 설치는 필수가 아니다.

## 8. CI 기준
- API: 테스트
- Admin: 테스트 + 타입체크 + 빌드
- Mobile: analyze + test
- Staging deploy 및 mobile release readiness workflow는 별도 문서(`docs/runbook.md`, `docs/mobile/README.md`) 기준으로 운영한다.

## 9. Codex 스킬 동기화 (다른 PC 포함)

저장소의 `skills/`를 로컬 Codex 스킬 디렉터리(`$CODEX_HOME/skills` 또는 `~/.codex/skills`)로 동기화한다.

전체 동기화:

```powershell
.\scripts\sync-skills.ps1
```

특정 스킬만 동기화:

```powershell
.\scripts\sync-skills.ps1 -Skill staging-ops-runner,docs-sync-enforcer
```

적용 전 확인(드라이런):

```powershell
.\scripts\sync-skills.ps1 -DryRun
```

동기화 후 Codex를 재시작한다.
