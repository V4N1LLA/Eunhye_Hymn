# 현재 사용 가능 범위 정리

- 작성일: 2026-02-13
- 기준 브랜치: `develop`
- 기준 커밋: `6b3d8cc` (Merge pull request #32)
- 근거 문서: `CLAUDE.md` (마지막 업데이트: 2026-02-13)
- 근거 PR: #32, #31, #30, #29, #28, #27

## 1. 요약

현재 시점에서 이 프로젝트는 다음 범위까지 사용 가능하다.

- 로컬 기준: 관리자 웹(Admin) + 백엔드 API + 모바일 앱 MVP 사용 가능
- 배포 기준: AWS 스테이징 자동 배포 파이프라인 구성 완료(인프라/시크릿 준비 필요)
- 미완료: 모바일 고도화 항목(소셜 SDK 직접 연동, MIDI 재생 UX, 오프라인 캐시)

## 2. 지금 바로 써볼 수 있는 범위 (로컬)

### 2.1 관리자 웹(Admin)

다음 기능을 UI에서 바로 검증할 수 있다.

- 찬양 관리: 목록/생성/수정/삭제, 검색/필터, 활성화 토글
- 에셋 관리: Presign -> 업로드 -> Confirm 3단계, 에셋 삭제
- 사용자 관리: 사용자 목록, 역할/상태 변경
- 초대코드 관리: 생성/비활성화/만료일 설정 및 표시
- 인증: Google/Kakao 소셜 로그인 UI, Dev 로그인
- 토큰: 401 발생 시 Access Token 자동 갱신 후 재시도

관련 파일:

- `apps/admin/src/App.tsx`
- `apps/admin/src/pages/HymnListPage.tsx`
- `apps/admin/src/pages/HymnEditPage.tsx`
- `apps/admin/src/pages/InviteCodePage.tsx`
- `apps/admin/src/pages/LoginPage.tsx`
- `apps/admin/src/api/client.ts`

### 2.2 백엔드 API

다음 도메인 기능이 동작 범위에 포함된다.

- 인증/인가: JWT, Refresh Token 회전, 소셜 로그인(Google/Kakao), Dev 로그인
- 찬양: 공개 조회 + 관리자 CRUD + 삭제(cascade)
- 에셋: 관리자 Presign/Confirm/Delete
- 사용자/초대코드 관리자 기능
- 멤버 기능: 즐겨찾기, 메모, 히스토리, 이벤트 기록

엔드포인트 기준은 `CLAUDE.md`의 API 섹션(7장)과 현재 컨트롤러/유즈케이스 구현 상태를 따른다.

### 2.3 모바일 앱 (Flutter MVP)

다음 기능을 앱에서 바로 검증할 수 있다.

- 로그인: 소셜 토큰 입력 방식(Google/Kakao), Dev 로그인
- 찬양: 목록 조회/검색, 상세 조회
- 개인화: 즐겨찾기 토글, 메모 조회/저장, 최근 열람 히스토리
- 인증: 토큰 저장, 401 시 자동 refresh 후 재시도

관련 파일:

- `apps/mobile/lib/src/app.dart`
- `apps/mobile/lib/src/features/auth/login_page.dart`
- `apps/mobile/lib/src/features/hymn/hymn_list_page.dart`
- `apps/mobile/lib/src/features/hymn/hymn_detail_page.dart`
- `apps/mobile/lib/src/features/history/history_page.dart`
- `apps/mobile/lib/src/core/network/api_client.dart`

## 3. 조건부로 써볼 수 있는 범위 (스테이징)

AWS 스테이징 인프라 및 CI/CD 자동 배포는 코드 기준으로 구성 완료 상태다.

- `develop` push 트리거
- API 테스트 + Admin 타입체크/빌드
- API/Admin Docker 이미지 ECR push
- EC2 SSH 배포 및 헬스체크

관련 파일:

- `.github/workflows/deploy-staging.yml`
- `infra/aws/*`
- `apps/admin/Dockerfile`
- `apps/admin/nginx.conf`
- `apps/api/Dockerfile`

단, 실제 동작을 위해 아래가 선행되어야 한다.

- AWS 리소스 생성(Terraform)
- GitHub Secrets 설정(`AWS_*`, `ECR_REGISTRY`, `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`)
- EC2 접근 가능 상태 및 배포 계정 권한 확인

## 4. 최근 PR 기준 변경 포인트

### PR #31 (aws-staging)

- AWS Free Tier 스테이징 인프라(Terraform) 추가
- Staging 자동 배포 워크플로우 추가
- Admin Dockerfile + Nginx 설정 추가
- API Dockerfile 보완(헬스체크 관련)

### PR #30 (social-login-ui)

- Admin 로그인 화면에 Google/Kakao 소셜 로그인 UI 추가
- 인증 관련 클라이언트 흐름 정리

### PR #29 (token-auto-refresh)

- 401 응답 시 Access Token 자동 갱신 및 재시도 로직 추가
- 인증 실패 처리 모드 정리(`redirect` / `throw`)

### PR #28 (invite-code-expiry-ui)

- 초대코드 만료일 입력/표시 UI 추가

### PR #27 (hymn-delete-ui)

- 찬양 삭제 UI 추가(목록/상세 화면)

## 5. 미완료 범위

- 모바일 고도화:
  - 소셜 SDK 직접 연동 (현재는 토큰 입력 방식)
  - MIDI 재생 UX
  - 오프라인 캐시/동기화

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
   - `cd apps/mobile`
   - `flutter pub get`
   - `flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api/v1`
4. 접속
   - Admin: `http://localhost:5173`
   - API Base: `http://localhost:8080/api/v1`

### 주의

- `JWT_SECRET`, `JWT_ACCESS_TTL_SECONDS`, `JWT_REFRESH_TTL_SECONDS`, `INVITE_CODE` 미설정 시 API 부팅 실패
- 소셜 로그인 실사용 검증 시 provider 토큰/클라이언트 설정 필요

