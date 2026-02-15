# AWS 무료 티어 온보딩 가이드

본 문서는 AWS를 처음 사용하는 팀원이 무료 티어 한도 내에서 `infra/aws` Terraform 구성을 배포하기 위해 따라야 하는 전체 절차를 설명합니다. 각 절차는 순차적으로 수행하며, 완료 여부를 체크박스로 관리하세요.

## 0. 사전 준비
- **루트 계정**: 결제·지원 작업에만 사용하고, Security Credentials 화면에서 MFA(보안키·가상 MFA 등)를 반드시 연결합니다.
- **과금 알림**: Billing → Preferences에서 “Receive AWS Free Tier alerts”를 켜고, Billing → Budgets → Create budget → Use template → `Zero spend` 또는 `Monthly cost`로 0~1달러 예산을 만들어 이메일을 등록합니다.
- **브라우저 언어**: 콘솔의 IAM/청구 화면 언어를 한국어로 맞추면 필드명이 아래 설명과 일치합니다.

## 1. IAM 관리자 사용자 만들기
1. IAM → 사용자 → 사용자 추가.
2. 사용자 이름: `admin` 등 개인 식별 가능한 값 입력.
3. **AWS Management Console 액세스** 선택 → 임시 비밀번호 자동 생성 + "사용자가 다음 로그인 시 새 비밀번호를 생성해야 함" 체크.
4. 권한 단계에서 **그룹에 사용자 추가** → `Administrators` 그룹 새로 생성 → `AdministratorAccess` 정책 추가 후 사용자에 연결.
5. 태그/검토 단계를 통과하여 생성 완료. 완료 화면의 콘솔 로그인 URL과 임시 비밀번호를 안전한 저장소(예: 1Password)에 기록.
6. 새 사용자로 다시 로그인하여 비밀번호를 변경하고, 사용자명 → 보안 자격 증명 → MFA 장치 할당에서 별도 MFA를 활성화.

## 2. AWS CLI 설치 및 프로필 구성
```powershell
# 설치 (이미 설치됐다면 생략)
winget install Amazon.AWSCLI

# 프로필 생성
aws configure --profile eunhye-staging
# 프롬프트 입력 예시
# AWS Access Key ID [None]: <IAM admin access key>
# AWS Secret Access Key [None]: <secret key>
# Default region name [None]: ap-northeast-2
# Default output format [None]: json

# 정상 구성 확인
aws sts get-caller-identity --profile eunhye-staging
```
- SSO를 사용할 경우 `aws configure sso --profile eunhye-staging`을 대신 실행합니다.
- CLI가 저장한 `~/.aws/credentials`와 `config`는 노출하지 말고, 필요 시만 편집하세요.

## 3. Terraform 변수 파일 작성
```powershell
cd infra/aws
Copy-Item terraform.tfvars.example terraform.tfvars
```
| 변수 | 설명 | 예시 |
| --- | --- | --- |
| `aws_region` | 기본 리전. 변경 이유 없으면 `ap-northeast-2`. | `ap-northeast-2` |
| `ec2_key_pair_name` | 곧 만들 SSH 키 이름과 동일 | `eunhye-staging` |
| `db_password` | RDS postgres 마스터 비밀번호(영문·숫자·특수문자 혼합 16자 이상) | `Eunhye2026!Safe` |
| `s3_bucket_name` | 전 세계 유일한 버킷 이름 | `eunhye-hymn-assets-20260213` |
| `jwt_secret` | 32~64자 랜덤 문자열 | `p9v...` |
| `invite_code` | 서비스 가입 초대 코드 | `SING-2026` |
| `allowed_ssh_cidrs` | SSH 허용 IP. 가능하면 본인 공인 IP/32 | `["123.45.67.89/32"]` |
| `alert_email` | CloudWatch 알람을 받을 주소 | `ops@example.com` |

> ⚠️ `terraform.tfvars`는 `.gitignore`에 포함되어 있으므로 Git에 커밋하지 않습니다. 대신 보안 저장소에 복사본을 보관하세요.

## 4. EC2 SSH 키 페어 생성
```powershell
$KeyPath = "$env:USERPROFILE\.ssh\eunhye-staging.pem"
aws ec2 create-key-pair `
  --profile eunhye-staging `
  --key-name eunhye-staging `
  --query 'KeyMaterial' `
  --output text > $KeyPath

# 권한 축소
icacls $KeyPath /inheritance:r
icacls $KeyPath /grant:r "${env:USERNAME}:R"
```
- 이미 키가 있다면 `aws ec2 describe-key-pairs --profile eunhye-staging --key-names eunhye-staging`으로 존재 여부를 확인하고 중복 생성을 피합니다.
- 키 파일은 Git/클라우드에 업로드하지 말고, 비밀번호 관리자나 안전한 파일 금고에 저장합니다.

## 5. Terraform으로 인프라 생성
```powershell
cd infra/aws
terraform init
terraform plan -var-file=terraform.tfvars
terraform apply -var-file=terraform.tfvars
```
- `plan` 결과에서 생성 대상이 모두 Free Tier 자원(`t2.micro`, `db.t3.micro`, 20GB gp2 등)인지 확인하세요.
- `apply`가 끝나면 `terraform output`으로 `ec2_public_ip`, `rds_endpoint`, `s3_bucket_name`, `ecr_registry` 등을 메모합니다.
- 상태 파일(`terraform.tfstate`)은 현재 로컬 백엔드를 사용합니다. 팀 공유가 필요하면 나중에 S3/DynamoDB 백엔드로 이전합니다.

## 6. 애플리케이션 배포 파이프라인
1. **ECR 로그인**
   ```powershell
   $REGISTRY = terraform output -raw ecr_registry
   aws ecr get-login-password --region ap-northeast-2 --profile eunhye-staging |
     docker login --username AWS --password-stdin $REGISTRY
   ```
2. **이미지 빌드 및 푸시**
   ```powershell
   cd apps/api
   docker build -t $REGISTRY/eunhye-hymn/api:latest .
   docker push $REGISTRY/eunhye-hymn/api:latest

   cd ..\admin
   docker build -t $REGISTRY/eunhye-hymn/admin:latest .
   docker push $REGISTRY/eunhye-hymn/admin:latest
   ```
3. **EC2에 배포 스크립트/환경 변수 배치**
   ```powershell
   $EC2 = terraform output -raw ec2_public_ip
   scp -i $KeyPath infra/aws/docker-compose.prod.yml ec2-user@${EC2}:/home/ec2-user/app/
   scp -i $KeyPath infra/aws/docker-compose.prod.awslogs.yml ec2-user@${EC2}:/home/ec2-user/app/
   scp -i $KeyPath infra/aws/deploy.sh ec2-user@${EC2}:/home/ec2-user/app/
   scp -i $KeyPath infra/aws/.env ec2-user@${EC2}:/home/ec2-user/app/.env
   ```
4. **원격에서 배포 실행**
   ```bash
   ssh -i ~/.ssh/eunhye-staging.pem ec2-user@${EC2} \
     "cd /home/ec2-user/app && export ECR_REGISTRY=$REGISTRY && export ENABLE_AWSLOGS=false && chmod +x deploy.sh && ./deploy.sh"
   ```
5. **Health Check**
   - API: `curl http://${EC2}/api/v1/ping`
   - Admin: 브라우저에서 `http://${EC2}` 접속

## 7. 운영 및 비용 모니터링 체크리스트
- **주 1회** Billing → Bills/Cost Explorer에서 사용량 확인 후 이상 징후가 있으면 즉시 리소스 중지.
- **CloudWatch 알람**: Terraform이 생성한 SNS 토픽에 `alert_email` 구독 확인. 이메일 수신 후 Confirm 누르지 않으면 알람이 전달되지 않습니다.
- **리소스 정리**: 실험 종료 시 `terraform destroy -var-file=terraform.tfvars`로 반드시 정리하고, ECR 이미지와 S3 객체도 필요 시 삭제.
- **보안**: Access Key는 주기적으로 교체하고, 사용하지 않는 키/사용자는 비활성화합니다.

## 8. 흔한 문제
| 증상 | 조치 |
| --- | --- |
| `UnauthorizedOperation` | CLI 프로필이 루트가 아닌지, 올바른 IAM 권한이 있는지 확인. 필요 시 `aws configure` 다시 실행. |
| `InsufficientInstanceCapacity` | 같은 리전에 동일 타입이 몰렸을 수 있으므로 잠시 후 재시도하거나 `t3.micro`로 변경. |
| `BucketAlreadyExists` | 전역 이름 충돌이므로 `s3_bucket_name`을 날짜/랜덤 접미사로 바꾼 뒤 `terraform apply -replace aws_s3_bucket.assets`. |
| SSH 접속 실패 | `allowed_ssh_cidrs`가 현재 IP를 포함하는지, Window 방화벽/회사 VPN 정책을 확인. |

## 9. 실제 실행 권한에 대한 안내
이 문서와 스크립트는 내부 개발자가 직접 AWS 콘솔/CLI에서 실행해야 합니다. 원격 에이전트는 계정 자격 증명에 접근할 수 없으므로, 위 절차를 따라 사용자 본인이 명령을 수행한 뒤 결과를 공유하면 추가 지원이 가능합니다.

