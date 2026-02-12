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
scp -i eunhye-staging.pem infra/aws/deploy.sh ec2-user@${EC2_IP}:/home/ec2-user/app/
# .env 파일도 복사 (infra/aws/.env.example 기반으로 작성)

# 5. EC2에서 배포 실행
ssh -i eunhye-staging.pem ec2-user@${EC2_IP} \
  "cd /home/ec2-user/app && export ECR_REGISTRY=${ECR_REGISTRY} && chmod +x deploy.sh && ./deploy.sh"
```

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
├── deploy.sh                  # EC2 배포 스크립트
├── .env.example               # 환경변수 예시
├── .gitignore                 # tfstate, .terraform 제외
└── README.md                  # 이 파일
```
