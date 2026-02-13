# 현재 사용 가능 범위 정리

- 작성일: 2026-02-13
- 기준 브랜치: `develop` (통합/배포 기준)
- 기준 커밋: `22011b7` (2026-02-13 로컬 확인 시점 `develop` HEAD)
- 근거 문서: `README.md`, `CLAUDE.md`, `docs/WORK_CYCLE.md`
- 근거 PR: #42, #41, #40, #38, #37, #36, #31

## 1. 요약

현재 시점 기준으로 프로젝트 상태는 다음과 같다.

- 로컬 기능: 관리자 웹(Admin) + 백엔드 API + 모바일 앱(MVP + 모바일 고도화 3건) 사용 가능
- 배포 기능: AWS 스테이징 자동 배포 파이프라인 코드 구성 완료
- CI 기능: API/Admin/Mobile 검증 워크플로우 구성 완료
- 현재 우선 과제: 스테이징 실배포 전환(Terraform 적용, Secrets 설정, EC2 권한/접근 확인)
- 모바일 배포 상태: 현재 저장소 기준 웹 실행(`-d chrome`) 중심이며, 앱스토어 배포(Android/iOS)는 별도 준비가 필요

## 2. 지금 바로 검증 가능한 범위 (로컬)

### 2.1 관리자 웹(Admin)

다음 기능을 UI에서 바로 검증할 수 있다.

- 찬양 관리: 목록/생성/수정/삭제, 검색/필터, 활성화 토글
- 에셋 관리: Presign -> 업로드 -> Confirm 3단계, 에셋 삭제
- 사용자 관리: 사용자 목록, 역할/상태 변경
- 초대코드 관리: 생성/비활성화/만료일 설정 및 표시
- 감사 로그/분석: 이벤트 로그 필터/페이지네이션 조회, 이벤트 타입별 집계(최근 N일), CSV 내보내기
- 인증: Google/Kakao 소셜 로그인 UI, Dev 로그인
- 토큰: 401 발생 시 Access Token 자동 갱신 후 재시도

관련 파일:

- `apps/admin/src/App.tsx`
- `apps/admin/src/pages/HymnListPage.tsx`
- `apps/admin/src/pages/HymnEditPage.tsx`
- `apps/admin/src/pages/InviteCodePage.tsx`
- `apps/admin/src/pages/AdminEventPage.tsx`
- `apps/admin/src/pages/LoginPage.tsx`
- `apps/admin/src/api/client.ts`
- `apps/admin/src/api/adminEvents.ts`

### 2.2 백엔드 API

다음 도메인 기능이 동작 범위에 포함된다.

- 인증/인가: JWT, Refresh Token 회전, 소셜 로그인(Google/Kakao), Dev 로그인
- 찬양: 공개 조회 + 관리자 CRUD + 삭제(cascade)
- 에셋: 관리자 Presign/Confirm/Delete (AssetType: `PNG`, `MIDI`)
- 사용자/초대코드 관리자 기능
- 관리자 감사 로그: `GET /admin/events` 조회/필터/페이지네이션 + 최근 N일 이벤트 타입 집계
- 관리자 감사 로그 내보내기: `GET /admin/events/export` CSV 다운로드
- 멤버 기능: 즐겨찾기, 메모, 히스토리, 이벤트 기록

엔드포인트 기준은 `CLAUDE.md` 7장과 현재 컨트롤러/유즈케이스 구현 상태를 따른다.

### 2.3 모바일 앱 (Flutter MVP + 고도화)

다음 기능을 앱에서 바로 검증할 수 있다.

- 로그인: 소셜 SDK 직접 로그인(Google/Kakao 모바일), Kakao 웹/미지원 플랫폼 토큰 입력 fallback, Dev 로그인
- 찬양: 목록 조회/검색, 상세 조회, PNG 에셋 표시, MIDI 앱 내 재생(재생/일시정지/정지/속도)
- 개인화: 즐겨찾기 토글, 메모 조회/저장, 최근 열람 히스토리
- 오프라인: 목록/상세/메모/즐겨찾기/히스토리 캐시 fallback + 오프라인 변경 동기화 큐
- 인증: 토큰 저장, 401 시 자동 refresh 후 재시도

관련 파일:

- `apps/mobile/lib/src/app.dart`
- `apps/mobile/lib/src/features/auth/login_page.dart`
- `apps/mobile/lib/src/features/auth/social_sdk_service.dart`
- `apps/mobile/lib/src/features/hymn/hymn_list_page.dart`
- `apps/mobile/lib/src/features/hymn/hymn_detail_page.dart`
- `apps/mobile/lib/src/features/history/history_page.dart`
- `apps/mobile/lib/src/core/network/api_client.dart`

## 3. 조건부로 검증 가능한 범위 (스테이징)

AWS 스테이징 인프라 및 CI/CD 자동 배포는 코드 기준으로 구성 완료 상태다.

- `develop` push 트리거
- API 테스트 + Admin 타입체크/빌드 + Mobile analyze/test
- API/Admin Docker 이미지 ECR push
- EC2 SSH 배포 및 헬스체크

관련 파일:

- `.github/workflows/deploy-staging.yml`
- `.github/workflows/mobile-ci.yml`
- `infra/aws/*`
- `apps/admin/Dockerfile`
- `apps/admin/nginx.conf`
- `apps/api/Dockerfile`

실제 동작을 위해 아래 선행 조건이 필요하다.

- [ ] AWS 리소스 생성(Terraform)
- [ ] GitHub Secrets 설정(`AWS_*`, `ECR_REGISTRY`, `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`, 선택: `ENABLE_AWSLOGS`)
- [ ] EC2 접근 가능 상태 및 배포 계정 권한 확인
- [ ] 배포 서버 `.env` 준비 (`infra/aws/docker-compose.prod.yml`의 `env_file: .env` 요구)
- [ ] 첫 `develop` 배포 후 `GET /api/v1/ping` + 관리자 로그인 + 핵심 API 스모크 테스트

2026-02-13 기준 진행 현황:
- 완료: GitHub Secrets 일부 등록 (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `ECR_REGISTRY`, `ENABLE_AWSLOGS`)
- 미완료: `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`
- 차단: `terraform-deployer` IAM 권한 부족 (`ec2:DescribeAvailabilityZones`, `ec2:DescribeImages`, `ec2:DescribeKeyPairs`)
- 대응: `scripts/staging-preflight.ps1`, `scripts/staging-sync-secrets.ps1`, `infra/aws/terraform-deployer-iam-policy.json` 추가

## 4. 최근 PR 기준 변경 포인트

### PR #38 (mobile-offline-cache-sync)
- 목록/상세/메모/즐겨찾기/히스토리 캐시 fallback, 오프라인 동기화 큐
- 리뷰 반영: 캐시/큐 세션 스코프(`userId`) 분리

### PR #37 (mobile-midi-player-ux)
- 상세 화면 MIDI 재생/일시정지/정지/속도 UI
- 리뷰 반영: paused 상태에서 `resume()` 사용

### PR #36 (mobile-social-sdk)
- Google/Kakao SDK 직접 로그인 흐름 추가
- 리뷰 반영: KakaoTalk 실패 시 `loginWithKakaoAccount()` fallback

### PR #31 (aws-staging)
- AWS Free Tier 스테이징 인프라(Terraform) 추가
- Staging 자동 배포 워크플로우 추가
- Admin Dockerfile + Nginx 설정 추가
- API Dockerfile 보완(헬스체크 관련)

## 5. 미완료 범위 (우선순위)

- 스테이징 실가동 전환
  - Terraform apply 및 리소스 활성화
  - GitHub Secrets 구성
  - 배포/롤백/장애 대응 실동작 검증
- 운영 기능 백로그
  - 감사 로그 고도화(집계 기간 커스텀, 대용량 비동기 export)

## 6. 빠른 사용 체크리스트

### 로컬 실행

1. API 실행
   - `cd apps/api`
   - `./gradlew bootRun` (Windows는 `gradlew.bat bootRun`)
2. Admin 실행
   - `cd apps/admin`
   - `npm install`
   - `npm run dev`
3. Mobile 실행
   - `.\scripts\flutterw.ps1 --version`
   - `cd apps/mobile`
   - `..\..\scripts\flutterw.ps1 pub get`
   - `..\..\scripts\flutterw.ps1 run -d chrome --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1`
4. 접속
   - Admin: `http://localhost:5173`
   - API Base: `http://localhost:8080/api/v1`

### 주의

- `JWT_SECRET`, `JWT_ACCESS_TTL_SECONDS`, `JWT_REFRESH_TTL_SECONDS`, `INVITE_CODE` 미설정 시 API 부팅 실패
- 소셜 로그인 실사용 검증 시 provider 토큰/클라이언트 설정 필요
- 모바일은 현재 문서/실행 가이드 기준으로 `flutter run -d chrome` 경로를 우선 지원

## 7. 2026-02-13 실검증 로그 (명령 기반)

- API 테스트: `./gradlew.bat test --no-daemon --stacktrace` 성공 (58 tests, failed 0, skipped 0)
- Admin 빌드: `npm run build` 성공
- Mobile 검증:
  - `..\..\scripts\flutterw.ps1 analyze` 성공 (`No issues found`)
  - `..\..\scripts\flutterw.ps1 test --reporter expanded` 성공
- Terraform: `terraform -chdir=infra/aws validate` 성공
- Compose:
  - 로컬(`infra/docker/docker-compose.yml`) config 파싱 성공
  - 스테이징(`infra/aws/docker-compose.prod.yml`)은 `.env` 파일 준비 전 실행 불가

상세 결과와 Go/No-Go 판단은 `docs/deployment-readiness-audit.md`를 기준으로 한다.

