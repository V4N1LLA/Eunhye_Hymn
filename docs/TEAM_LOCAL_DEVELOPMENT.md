# Local Android Test Runbook

This guide is the minimal reproducible flow for setting up local verification on a new machine.

## 0) One-command local bootstrap

```powershell
.\scripts\local-bootstrap.ps1
```

This runs:

- env file bootstrap (`.env`, `apps/mobile/.env`)
- optional kakao key sync from root/mobile env
- `local-verify.ps1`
- printed test summary (`.tmp/local-smoke-summary.json`)

## 1) Setup
```bash
git clone <repo-url>
git switch develop
```

Requirements:
- Docker Desktop
- Java 17
- Node.js + npm
- Git
- Android Studio + Emulator

## 2) Env files
```bash
cd Eunhye_Hymn
copy .env.example .env
```

Create `apps/mobile/.env` (minimum) from template:
```bash
copy apps/mobile\.env.example apps/mobile\.env
```

Then set:
```env
API_BASE_URL=http://10.0.2.2:8080/api/v1
KAKAO_NATIVE_APP_KEY=<your_kakao_native_app_key>
```

Important keys in `.env`:
- `POSTGRES_DB`, `DB_USER`, `DB_PASS`
- `JWT_SECRET`
- `INVITE_CODE` (for fallback local invite)
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`
- `ADMIN_LOGIN_ID`, `ADMIN_LOGIN_PASSWORD`

`local-verify.ps1` will fill missing `ADMIN_*` and AWS keys automatically when run.

## 3) Start local stack
```bash
docker compose --env-file .env -f infra/docker/docker-compose.yml up -d
```

Check API health:
```bash
curl http://localhost:8080/api/v1/ping
```

## 4) Run local smoke checks
```bash
.\scripts\local-verify.ps1
```

If the stack is already running, use:
```bash
.\scripts\local-verify.ps1 -SkipDockerUp
```

Checks performed:
- `/ping`
- admin login
- invite create + validate (case/space normalization)
- hymn create + signed upload + asset confirm
- DB user create + `/auth/dev/login`
- asset URL reachability
- summary output: `.tmp/local-smoke-summary.json` (`inviteCode`, `dbUserId`, `hymnId`, `assetUrl`)

If this passes, you can use the printed Invite Code and DB User ID for UI checks.
`local-bootstrap.ps1` is the preferred flow from a new machine.

## 5) Admin page on local
```bash
cd apps/admin
npm install
npm run dev
```

Open: `http://localhost:5173`

Use `ADMIN_LOGIN_ID / ADMIN_LOGIN_PASSWORD` from `.env`.

> Note: local DB is separate from staging. Staging invite codes are not valid in local DB.
- If you ever copied invite codes from staging, they will not be valid in local env.

## 6) Run mobile (Android emulator)
```bash
.\scripts\run-mobile-emulator.ps1 -Environment local -DeviceId emulator-5554
```

Flow in app:
- Enter invite code then tap `카카오로 시작하기` for Kakao
- Or tap `아이디/비밀번호로 시작하기` for `/auth/login`
- If account does not exist yet, tap `회원가입` to create one via `/auth/signup`
- Use an invite code created in local DB (from `local-verify.ps1` or admin page)

## 7) Useful DB checks
```bash
docker exec -it eunhye-postgres psql -U postgres -d eunhye_hymn -c "\dt"

docker exec -it eunhye-postgres psql -U postgres -d eunhye_hymn -c "SELECT id,role,status,created_at FROM users ORDER BY created_at DESC LIMIT 20;"

docker exec -it eunhye-postgres psql -U postgres -d eunhye_hymn -c "SELECT code,enabled,used_count,max_uses,expires_at FROM invite_codes ORDER BY created_at DESC LIMIT 20;"
```

## 8) Stop local stack
```bash
docker compose --env-file .env -f infra/docker/docker-compose.yml down
# keep data but clear all (optional):
# docker compose --env-file .env -f infra/docker/docker-compose.yml down -v
```

## 9) Cost note
- Local stack (PostgreSQL + LocalStack + API) does not add AWS charge by itself.
- Any calls to staging or real AWS resources follow staging billing policies.
- For Android Emulator asset access, set in `.env` before restart:
  `S3_PUBLIC_BASE_URL=http://10.0.2.2:4566/local-bucket`

## 10) Fast check list
- [ ] `docker compose up` running
- [ ] `/ping` returns OK
- [ ] `local-verify` passes
- [ ] admin can create invite in `admin`
- [ ] app accepts invite code
- [ ] user login and hymn detail asset load are visible

## 11) 다른 컴퓨터 재동기화 루틴(가장 빠른)

1. 새 PC에서 저장소만 `git pull`로 갱신
2. 한 번만 실행:
   ```powershell
   Copy-Item .env.example .env
   Copy-Item apps/mobile/.env.example apps/mobile/.env
   # KAKAO 키 직접 반영 (실제 키로 한 번만 교체)
   (Get-Content apps/mobile/.env) -replace '^KAKAO_NATIVE_APP_KEY=.*$', 'KAKAO_NATIVE_APP_KEY=<your_kakao_native_app_key>' | Set-Content apps/mobile/.env
   ```
3. 한 번에 로컬 환경 점검:
   ```powershell
   .\scripts\local-bootstrap.ps1
   ```
4. 터미널에 출력되는 `Invite Code`를 앱의 초대 코드 입력창에 붙여 넣고 테스트
5. 테스트 후 종료:
   ```powershell
   docker compose --env-file .env -f infra/docker/docker-compose.yml down
   ```

### 왜 스테이징 코드는 로컬에서 안 맞는가

- 스테이징과 로컬 DB는 별도 입니다. 스테이징에서 발급한 초대코드를 로컬 DB로 가져오지 않습니다.
- 로컬 검증은 `local-bootstrap.ps1`가 매번 로컬 DB에 새 초대코드를 만들고 바로 확인합니다.
