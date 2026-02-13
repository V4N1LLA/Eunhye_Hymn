# 배포 준비도 점검 리포트

- 작성일: 2026-02-13
- 점검 브랜치: `feat/docs-readiness-audit` (base: `develop`)
- 점검 목적: "현재 개발 상태가 문서와 일치하는지"와 "실제 배포 가능 여부"를 코드/실행 기준으로 확인

## 1. 결론 요약

- 로컬 개발/검증: **가능**
- 스테이징 자동배포 파이프라인: **코드 구성 완료 (조건부 가능)**
- 스테이징 실가동: **미완료** (인프라 생성/시크릿/접근 검증 필요)
- 프로덕션 배포: **준비 전**
- 모바일 스토어 배포(Android/iOS): **준비 전**

## 2. 실행 검증 결과

### 2.1 API

- 명령: `./gradlew.bat test --no-daemon --stacktrace`
- 결과: 성공 (58 tests, 0 failed, 0 skipped)
- 근거 파일:
  - `apps/api/src/test/java/com/eunhyehymn/presentation/controllers/AdminEventApiTest.java`
  - `apps/api/src/test/java/com/eunhyehymn/presentation/controllers/SocialLoginApiTest.java`

### 2.2 Admin

- 명령: `npm run build` (`apps/admin`)
- 결과: 성공 (Vite production build 완료)
- 근거 파일:
  - `apps/admin/package.json`
  - `.github/workflows/admin-ci.yml`

### 2.3 Mobile

- 명령:
  - `..\..\scripts\flutterw.ps1 analyze`
  - `..\..\scripts\flutterw.ps1 test --reporter expanded`
- 결과: 성공 (`No issues found`, 테스트 통과)
- 근거 파일:
  - `apps/mobile/test/app_config_test.dart`
  - `.github/workflows/mobile-ci.yml`

### 2.4 Infra (Terraform/Compose)

- 명령:
  - `terraform -chdir=infra/aws init -backend=false -input=false`
  - `terraform -chdir=infra/aws validate`
  - `docker compose -f infra/docker/docker-compose.yml config`
- 결과:
  - Terraform config 유효성 통과
  - 로컬 compose 파싱 통과
  - 스테이징 compose는 `.env` 파일 없이 실행 불가 (`infra/aws/docker-compose.prod.yml`에서 `env_file: .env` 요구)

## 3. 문서 정합성 점검 결과

- 일치:
  - 로컬 기능 범위(관리자/API/모바일 MVP+고도화)
  - 스테이징 배포 워크플로우 존재 및 단계 구성
  - 선행 조건(Terraform/Secrets/EC2 접근) 필요
- 보완:
  - Flyway 마이그레이션 문서에 `V7__events_admin_indexes.sql` 반영 필요
  - 기준 커밋 고정값 문서(`docs/current-usable-scope.md`) 최신화 필요
  - 모바일 배포 범위(현재 웹 실행 기준, 스토어 배포 준비 전) 명시 강화 필요

## 4. 실배포 전 필수 체크리스트

- [ ] `infra/aws`에 대해 `terraform apply` 완료
- [ ] GitHub Actions Secrets 등록
  - `AWS_ACCESS_KEY_ID`
  - `AWS_SECRET_ACCESS_KEY`
  - `AWS_REGION`
  - `ECR_REGISTRY`
  - `EC2_HOST`
  - `EC2_SSH_KEY`
  - `DEPLOY_ENV_FILE`
  - `ENABLE_AWSLOGS` (선택)
- [ ] EC2 SSH 접근 및 배포 계정 권한 확인
- [ ] `develop` 기준 1회 자동배포 후 스모크 테스트
  - `GET /api/v1/ping`
  - 관리자 로그인
  - 찬양 목록/상세, 에셋 업로드, 감사 로그 조회
- [ ] 롤백 리허설 1회 수행 (`docs/runbook.md`)

### 4.1 2026-02-13 진행 업데이트

- 완료:
  - GitHub Secrets 일부 등록 완료
    - `AWS_ACCESS_KEY_ID`
    - `AWS_SECRET_ACCESS_KEY`
    - `AWS_REGION`
    - `ECR_REGISTRY`
    - `ENABLE_AWSLOGS=false`
- 미완료:
  - `EC2_HOST`, `EC2_SSH_KEY`, `DEPLOY_ENV_FILE`
- 차단 이슈:
  - 현재 IAM 사용자(`terraform-deployer`)에서 아래 조회 권한 부족으로 `terraform plan/apply` 진행 불가
    - `ec2:DescribeAvailabilityZones`
    - `ec2:DescribeImages`
    - `ec2:DescribeKeyPairs`
- 조치:
  - 권한 정책 샘플 추가: `infra/aws/terraform-deployer-iam-policy.json`
  - 사전 점검 스크립트 추가: `scripts/staging-preflight.ps1`
  - Secrets 동기화 스크립트 추가: `scripts/staging-sync-secrets.ps1`

## 5. 현재 판단 (Go/No-Go)

- 로컬 데모/개발: **Go**
- 스테이징 실운영 검증: **Conditional Go** (체크리스트 완료 후)
- 프로덕션 공개 배포: **No-Go**
- 모바일 앱스토어 배포: **No-Go**

## 6. 관련 문서

- `docs/current-usable-scope.md`
- `docs/runbook.md`
- `infra/aws/README.md`
- `.github/workflows/deploy-staging.yml`
- `CLAUDE.md`
