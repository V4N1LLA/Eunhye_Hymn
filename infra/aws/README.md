# AWS Staging 배포 가이드

AWS Free Tier를 사용한 스테이징 환경 배포 가이드입니다.

## 아키텍처

```
[사용자] → http://<EC2-IP>
                │
         ┌──────┴──────┐
         │   Nginx     │  ← Admin 정적 파일 serve + API 리버스 프록시
         │  (port 80)  │
         └──────┬──────┘
                │ /api/v1 → proxy
         ┌──────┴──────┐
         │  Spring Boot│  ← API 컨테이너 (ECR 이미지)
         │  (port 8080)│
         └──────┬──────┘
                │
         ┌──────┴──────┐
         │ RDS Postgres│  ← db.t3.micro (Free Tier)
         │  (port 5432)│
         └─────────────┘

S3 버킷 ← 에셋(PNG/MIDI) 저장
ECR     ← Docker 이미지 저장
```

## 사전 준비

### 1. AWS CLI 설정

```bash
# AWS CLI 설치 (Windows)
winget install Amazon.AWSCLI

# 자격 증명 설정
aws configure
# Access Key ID, Secret Access Key, Region (ap-northeast-2) 입력
```

### 2. Terraform 설치

```bash
# Windows
winget install HashiCorp.Terraform

# 확인
terraform version
```

### 3. EC2 키페어 생성

```bash
aws ec2 create-key-pair \
  --key-name eunhye-staging \
  --query 'KeyMaterial' \
  --output text > eunhye-staging.pem

# Linux/Mac: chmod 400 eunhye-staging.pem
```

### 4. Terraform 실행 IAM 권한 확인

현재 Terraform은 VPC/EC2/RDS/S3/ECR/CloudWatch/SNS/IAM 리소스를 생성한다.
`terraform plan` 단계에서 `UnauthorizedOperation`이 발생하면 먼저 IAM 정책을 보강해야 한다.

- 정책 샘플: `infra/aws/terraform-deployer-iam-policy.json`
- 빠른 점검: `.\scripts\staging-preflight.ps1`

## 인프라 배포

### 1. 변수 설정

```bash
cd infra/aws
cp terraform.tfvars.example terraform.tfvars
# terraform.tfvars 편집: db_password, jwt_secret, invite_code 등 설정
```

### 2. Terraform 실행

```bash
terraform init
terraform plan     # 리소스 확인
terraform apply    # 생성 (yes 입력)
```

### 3. 출력값 확인

```bash
terraform output
# ec2_public_ip    = "3.xx.xx.xx"
# rds_endpoint     = "eunhye-hymn-db.xxxx.rds.amazonaws.com:5432"
# s3_bucket_name   = "eunhye-hymn-assets-staging"
# ecr_registry     = "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com"
# ecr_api_url      = "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/eunhye-hymn/api"
# ecr_admin_url    = "123456789012.dkr.ecr.ap-northeast-2.amazonaws.com/eunhye-hymn/admin"
```

## GitHub Actions 자동 배포 설정

### 필수 Secrets 등록

GitHub 리포지토리 → Settings → Secrets and variables → Actions에 등록:

| Secret 이름 | 값 | 출처 |
|---|---|---|
| `AWS_ACCESS_KEY_ID` | AWS IAM 사용자 Access Key | AWS Console |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM 사용자 Secret Key | AWS Console |
| `AWS_REGION` | `ap-northeast-2` | 고정 |
| `ECR_REGISTRY` | `terraform output ecr_registry` | Terraform |
| `EC2_HOST` | `terraform output ec2_public_ip` | Terraform |
| `EC2_SSH_KEY` | `eunhye-staging.pem` 파일 내용 | 키페어 생성 시 |
| `DEPLOY_ENV_FILE` | `.env` 파일 전체 내용 (아래 참조) | 직접 작성 |
| `ENABLE_AWSLOGS` | `true` 또는 빈값(기본) | 선택 (Terraform 적용 후만 `true`) |

### Secrets 자동 동기화 스크립트 (권장)

Terraform 적용 후 아래 스크립트로 필수 Secrets를 한 번에 동기화할 수 있다.

```powershell
.\scripts\staging-sync-secrets.ps1 `
  -Repo V4N1LLA/Eunhye_Hymn `
  -TerraformDir infra/aws `
  -Ec2Host <terraform output ec2_public_ip> `
  -Ec2SshKeyPath <eunhye-staging.pem 경로> `
  -DbPassword <terraform.tfvars의 db_password 값> `
  -JwtSecret <terraform.tfvars의 jwt_secret 값> `
  -InviteCode <terraform.tfvars의 invite_code 값>
```

옵션:
- `-AwsProfile <profile>`: 기본 프로필이 아닌 AWS CLI 프로필 사용
- `-AwsAccessKeyId`/`-AwsSecretAccessKey`/`-AwsRegion`: AWS 자격증명/리전 직접 지정

사전 점검:

```powershell
.\scripts\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn [-AwsProfile eunhye-staging]
```

리허설 자동 실행(권장):

```powershell
.\scripts\staging-rehearsal.ps1 `
  -Repo V4N1LLA/Eunhye_Hymn `
  -Ref develop `
  [-AwsProfile eunhye-staging]
```

이 스크립트는 `staging-preflight.ps1` 실행 후 `deploy-staging.yml`을 `workflow_dispatch`로 트리거하고 run 완료까지 대기한 다음 `docs/staging-rehearsal-log.md`에 결과를 기록한다.

로컬 문법/파라미터 검증만 필요하면 `-DryRun` 옵션을 사용한다.

최신 배포 run 상태(특히 deploy/verify 성공 여부) 확인:

```powershell
.\scripts\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120
```

JSON 출력이 필요하면 `-AsJson` 옵션을 사용한다.
리허설/수동 실행 내역 확인 시에는 `-Branch <branch>` 또는 `-Event workflow_dispatch` 옵션을 함께 사용한다.
문서/티켓 첨부용 표가 필요하면 `-AsMarkdown` 옵션을 사용한다.
최신 성공 run이 오래되면 `-MaxAgeMinutes <minutes>` 값으로 스모크 진입 전 신선도 기준을 강제한다.

주의:
- `-Ref`에는 branch/tag만 사용할 수 있다 (`workflow_dispatch` 제약).
- 특정 커밋 SHA 기준 리허설이 필요하면 임시 브랜치를 만든 뒤 해당 브랜치명을 `-Ref`로 전달한다.

### 수동 실행 (권장 점검 루트)

`deploy-staging.yml`은 `workflow_dispatch`를 지원합니다.

- 브랜치 검증 시:
  - GitHub Actions > `Deploy Staging` > `Run workflow`
  - `Use workflow from`: 검증 브랜치 선택
  - `enable_awslogs`: 기본 `false`로 실행
- 운영 반영 시:
  - `develop` 머지 후 push 트리거 자동 실행

### DEPLOY_ENV_FILE 내용

`infra/aws/.env.example`을 참고하여 작성:

```
ECR_REGISTRY=<terraform output ecr_registry>
DB_URL=jdbc:postgresql://<terraform output rds_address>:5432/eunhye_hymn
DB_USER=postgres
DB_PASS=<your-db-password>
JWT_SECRET=<your-jwt-secret>
JWT_ACCESS_TTL_SECONDS=3600
JWT_REFRESH_TTL_SECONDS=604800
INVITE_CODE=<your-invite-code>
GOOGLE_CLIENT_ID=
ADMIN_EMAILS=
ADMIN_KAKAO_SUBJECTS=
ADMIN_ENFORCE_ADMIN_ONLY=false
ADMIN_LOGIN_ID=
ADMIN_LOGIN_PASSWORD=
S3_BUCKET=<terraform output s3_bucket_name>
S3_REGION=ap-northeast-2
S3_ENDPOINT=
S3_PUBLIC_BASE_URL=<terraform output s3_bucket_url>
S3_PRESIGN_EXPIRES_MINUTES=15
SPRING_PROFILES_ACTIVE=prod
```

## 수동 배포

GitHub Actions 없이 수동으로 배포하는 방법:

```bash
# 1. ECR 로그인
ECR_REGISTRY=$(terraform output -raw ecr_registry)
aws ecr get-login-password --region ap-northeast-2 | \
  docker login --username AWS --password-stdin ${ECR_REGISTRY}

# 2. API 이미지 빌드 & 푸시
cd apps/api
docker build -t ${ECR_REGISTRY}/eunhye-hymn/api:latest .
docker push ${ECR_REGISTRY}/eunhye-hymn/api:latest

# 3. Admin 이미지 빌드 & 푸시
cd apps/admin
docker build -t ${ECR_REGISTRY}/eunhye-hymn/admin:latest .
docker push ${ECR_REGISTRY}/eunhye-hymn/admin:latest

# 4. EC2에 파일 복사
EC2_IP=$(terraform output -raw ec2_public_ip)
scp -i eunhye-staging.pem infra/aws/docker-compose.prod.yml ec2-user@${EC2_IP}:/home/ec2-user/app/
scp -i eunhye-staging.pem infra/aws/docker-compose.prod.awslogs.yml ec2-user@${EC2_IP}:/home/ec2-user/app/
scp -i eunhye-staging.pem infra/aws/deploy.sh ec2-user@${EC2_IP}:/home/ec2-user/app/
# .env 파일도 복사 (infra/aws/.env.example 기반으로 작성)

# 5. EC2에서 배포 실행
ssh -i eunhye-staging.pem ec2-user@${EC2_IP} \
  "cd /home/ec2-user/app && export ECR_REGISTRY=${ECR_REGISTRY} && export ENABLE_AWSLOGS=false && chmod +x deploy.sh && ./deploy.sh"
```

`ENABLE_AWSLOGS`는 기본값 `false`입니다. 기존 스테이징 인스턴스에서 Terraform(IAM/Log Group) 적용 전에 `true`로 배포하면 컨테이너 시작이 실패할 수 있으므로, 인프라 적용 이후에만 활성화하세요.

참고: `deploy.sh`는 `ENABLE_AWSLOGS=true`라도 호스트가 awslogs 드라이버를 지원하지 않거나 awslogs 모드 재기동에 실패하면 기본 logging(`json-file`)으로 자동 fallback 후 재시도합니다.

## 검증

```bash
EC2_IP=$(terraform output -raw ec2_public_ip)

# 1. SSH 접속 & Docker 확인
ssh -i eunhye-staging.pem ec2-user@${EC2_IP} "docker --version && docker compose version"

# 2. API 헬스 체크
curl http://${EC2_IP}/api/v1/ping
# → {"ok":true}

# 3. Admin 웹 접속
# 브라우저에서 http://<EC2_IP> 접속
```

## 인프라 삭제

```bash
cd infra/aws
terraform destroy   # yes 입력
```

## Free Tier 비용 참고

| 서비스 | 스펙 | Free Tier |
|---|---|---|
| EC2 | t2.micro (1 vCPU, 1GB) | 750시간/월 (12개월) |
| RDS | db.t3.micro (2 vCPU, 1GB) | 750시간/월 (12개월), 20GB |
| S3 | 표준 | 5GB, 20K GET, 2K PUT |
| ECR | 프라이빗 | 500MB |
| Elastic IP | 1개 | 실행 중 인스턴스 연결 시 무료 |

12개월 내 인스턴스 1개만 유지하면 추가 비용 없음.

## 파일 구조

```
infra/aws/
├── main.tf                    # Provider, backend 설정
├── variables.tf               # 입력 변수 정의
├── terraform.tfvars.example   # 변수 예시 (복사해서 사용)
├── vpc.tf                     # VPC, 서브넷, IGW, 라우트 테이블
├── security.tf                # EC2/RDS 보안 그룹
├── rds.tf                     # RDS PostgreSQL (Free Tier)
├── s3.tf                      # S3 버킷 + public read 정책
├── ecr.tf                     # ECR 레포지토리 (api, admin)
├── ec2.tf                     # EC2 t2.micro + Elastic IP + IAM
├── outputs.tf                 # 출력값 (IP, 엔드포인트, URL)
├── docker-compose.prod.yml    # 프로덕션 컨테이너 구성
├── docker-compose.prod.awslogs.yml # CloudWatch 로그 오버레이(옵션)
├── deploy.sh                  # EC2 배포 스크립트
├── terraform-deployer-iam-policy.json # Terraform 실행 IAM 정책 샘플
├── .env.example               # 환경변수 예시
├── .gitignore                 # tfstate, .terraform 제외
└── README.md                  # 이 파일
```
