param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$TerraformDir = "infra/aws",
  [string]$Ec2Host,
  [string]$Ec2SshKeyPath,
  [string]$DbPassword,
  [string]$JwtSecret,
  [string]$InviteCode,
  [string]$GoogleClientId = "",
  [switch]$EnableAwsLogs
)

$ErrorActionPreference = "Stop"

if (-not $Ec2Host) { throw "Ec2Host is required." }
if (-not $Ec2SshKeyPath) { throw "Ec2SshKeyPath is required." }
if (-not $DbPassword) { throw "DbPassword is required." }
if (-not $JwtSecret) { throw "JwtSecret is required." }
if (-not $InviteCode) { throw "InviteCode is required." }
if (-not (Test-Path $Ec2SshKeyPath)) { throw "EC2 SSH key file not found: $Ec2SshKeyPath" }

$awsAccessKeyId = aws configure get aws_access_key_id
$awsSecretAccessKey = aws configure get aws_secret_access_key
$awsRegion = aws configure get region
if ([string]::IsNullOrWhiteSpace($awsRegion)) {
  $awsRegion = "ap-northeast-2"
}

$account = (aws sts get-caller-identity --query Account --output text).Trim()
$ecrRegistry = "$account.dkr.ecr.$awsRegion.amazonaws.com"

$rdsEndpoint = terraform -chdir=$TerraformDir output -raw rds_endpoint
$s3Bucket = terraform -chdir=$TerraformDir output -raw s3_bucket_name
$s3PublicUrl = terraform -chdir=$TerraformDir output -raw s3_bucket_url

$deployEnvFile = @"
ECR_REGISTRY=$ecrRegistry
DB_URL=jdbc:postgresql://$rdsEndpoint:5432/eunhye_hymn
DB_USER=postgres
DB_PASS=$DbPassword
JWT_SECRET=$JwtSecret
JWT_ACCESS_TTL_SECONDS=3600
JWT_REFRESH_TTL_SECONDS=604800
INVITE_CODE=$InviteCode
GOOGLE_CLIENT_ID=$GoogleClientId
S3_BUCKET=$s3Bucket
S3_REGION=$awsRegion
S3_ENDPOINT=
S3_PUBLIC_BASE_URL=$s3PublicUrl
S3_PRESIGN_EXPIRES_MINUTES=15
SPRING_PROFILES_ACTIVE=prod
"@

$sshKeyContent = Get-Content $Ec2SshKeyPath -Raw

gh secret set AWS_ACCESS_KEY_ID --repo $Repo --body $awsAccessKeyId
gh secret set AWS_SECRET_ACCESS_KEY --repo $Repo --body $awsSecretAccessKey
gh secret set AWS_REGION --repo $Repo --body $awsRegion
gh secret set ECR_REGISTRY --repo $Repo --body $ecrRegistry
gh secret set EC2_HOST --repo $Repo --body $Ec2Host
gh secret set EC2_SSH_KEY --repo $Repo --body $sshKeyContent
gh secret set DEPLOY_ENV_FILE --repo $Repo --body $deployEnvFile
gh secret set ENABLE_AWSLOGS --repo $Repo --body ($(if ($EnableAwsLogs) { "true" } else { "false" }))

Write-Host "GitHub staging secrets updated for $Repo"
