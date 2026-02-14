param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$TerraformDir = "infra/aws",
  [string]$Ec2Host,
  [string]$Ec2SshKeyPath,
  [string]$DbPassword,
  [string]$JwtSecret,
  [string]$InviteCode,
  [string]$GoogleClientId = "",
  [string]$AdminEmails = "",
  [string]$AdminKakaoSubjects = "",
  [bool]$AdminEnforceAdminOnly = $false,
  [string]$AwsProfile = "",
  [string]$AwsAccessKeyId = "",
  [string]$AwsSecretAccessKey = "",
  [string]$AwsRegion = "",
  [switch]$EnableAwsLogs
)

$ErrorActionPreference = "Stop"

if (-not $Ec2Host) { throw "Ec2Host is required." }
if (-not $Ec2SshKeyPath) { throw "Ec2SshKeyPath is required." }
if (-not $DbPassword) { throw "DbPassword is required." }
if (-not $JwtSecret) { throw "JwtSecret is required." }
if (-not $InviteCode) { throw "InviteCode is required." }
if (-not (Test-Path $Ec2SshKeyPath)) { throw "EC2 SSH key file not found: $Ec2SshKeyPath" }

function Get-AwsProfileArgs {
  param([string]$Profile)
  if ([string]::IsNullOrWhiteSpace($Profile)) {
    return @()
  }
  return @("--profile", $Profile)
}

function Get-AwsConfigValue {
  param(
    [string]$Key,
    [string]$Profile
  )
  if ([string]::IsNullOrWhiteSpace($Profile)) {
    return (aws configure get $Key).Trim()
  }
  return (aws configure get $Key --profile $Profile).Trim()
}

function Get-TerraformOutputRaw {
  param(
    [string]$Dir,
    [string]$Name
  )
  $value = terraform "-chdir=$Dir" output -raw $Name
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($value)) {
    throw "Failed to read terraform output: $Name"
  }
  return $value.Trim()
}

function Escape-ComposeEnvValue {
  param(
    [string]$Value
  )
  if ($null -eq $Value) { return "" }
  return $Value.Replace('$', '$$')
}

$awsArgs = Get-AwsProfileArgs -Profile $AwsProfile
if ([string]::IsNullOrWhiteSpace($AwsAccessKeyId)) {
  $AwsAccessKeyId = Get-AwsConfigValue -Key "aws_access_key_id" -Profile $AwsProfile
}
if ([string]::IsNullOrWhiteSpace($AwsSecretAccessKey)) {
  $AwsSecretAccessKey = Get-AwsConfigValue -Key "aws_secret_access_key" -Profile $AwsProfile
}
if ([string]::IsNullOrWhiteSpace($AwsRegion)) {
  $AwsRegion = Get-AwsConfigValue -Key "region" -Profile $AwsProfile
}
if ([string]::IsNullOrWhiteSpace($AwsRegion)) {
  $AwsRegion = "ap-northeast-2"
}

if ([string]::IsNullOrWhiteSpace($AwsAccessKeyId) -or [string]::IsNullOrWhiteSpace($AwsSecretAccessKey)) {
  throw "AWS access key/secret is empty. Pass -AwsAccessKeyId/-AwsSecretAccessKey or configure aws cli profile."
}

$account = (aws @awsArgs sts get-caller-identity --query Account --output text).Trim()
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($account)) {
  throw "Failed to resolve AWS account. Check AWS credentials/profile."
}

$rdsAddress = Get-TerraformOutputRaw -Dir $TerraformDir -Name "rds_address"
$s3Bucket = Get-TerraformOutputRaw -Dir $TerraformDir -Name "s3_bucket_name"
$s3PublicUrl = Get-TerraformOutputRaw -Dir $TerraformDir -Name "s3_bucket_url"

try {
  $ecrRegistry = Get-TerraformOutputRaw -Dir $TerraformDir -Name "ecr_registry"
} catch {
  $ecrRegistry = "$account.dkr.ecr.$AwsRegion.amazonaws.com"
}

$dbPasswordEscaped = Escape-ComposeEnvValue -Value $DbPassword
$jwtSecretEscaped = Escape-ComposeEnvValue -Value $JwtSecret
$inviteCodeEscaped = Escape-ComposeEnvValue -Value $InviteCode
$googleClientIdEscaped = Escape-ComposeEnvValue -Value $GoogleClientId
$adminEmailsEscaped = Escape-ComposeEnvValue -Value $AdminEmails
$adminKakaoSubjectsEscaped = Escape-ComposeEnvValue -Value $AdminKakaoSubjects
$adminEnforceValue = $(if ($AdminEnforceAdminOnly) { "true" } else { "false" })

$deployEnvFile = @"
ECR_REGISTRY=$ecrRegistry
DB_URL=jdbc:postgresql://${rdsAddress}:5432/eunhye_hymn
DB_USER=postgres
DB_PASS=$dbPasswordEscaped
JWT_SECRET=$jwtSecretEscaped
JWT_ACCESS_TTL_SECONDS=3600
JWT_REFRESH_TTL_SECONDS=604800
INVITE_CODE=$inviteCodeEscaped
GOOGLE_CLIENT_ID=$googleClientIdEscaped
ADMIN_EMAILS=$adminEmailsEscaped
ADMIN_KAKAO_SUBJECTS=$adminKakaoSubjectsEscaped
ADMIN_ENFORCE_ADMIN_ONLY=$adminEnforceValue
S3_BUCKET=$s3Bucket
S3_REGION=$AwsRegion
S3_ENDPOINT=
S3_PUBLIC_BASE_URL=$s3PublicUrl
S3_PRESIGN_EXPIRES_MINUTES=15
SPRING_PROFILES_ACTIVE=prod
"@

$sshKeyContent = Get-Content $Ec2SshKeyPath -Raw
if ([string]::IsNullOrWhiteSpace($sshKeyContent)) {
  throw "EC2 SSH key file is empty: $Ec2SshKeyPath"
}

function Set-GhSecret {
  param(
    [string]$Name,
    [string]$Value
  )
  if ([string]::IsNullOrWhiteSpace($Value)) {
    throw "Secret value is empty: $Name"
  }
  gh secret set $Name --repo $Repo --body $Value
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to set secret: $Name"
  }
}

Set-GhSecret -Name "AWS_ACCESS_KEY_ID" -Value $AwsAccessKeyId
Set-GhSecret -Name "AWS_SECRET_ACCESS_KEY" -Value $AwsSecretAccessKey
Set-GhSecret -Name "AWS_REGION" -Value $AwsRegion
Set-GhSecret -Name "ECR_REGISTRY" -Value $ecrRegistry
Set-GhSecret -Name "EC2_HOST" -Value $Ec2Host
Set-GhSecret -Name "EC2_SSH_KEY" -Value $sshKeyContent
Set-GhSecret -Name "DEPLOY_ENV_FILE" -Value $deployEnvFile
Set-GhSecret -Name "ENABLE_AWSLOGS" -Value ($(if ($EnableAwsLogs) { "true" } else { "false" }))

Write-Host "GitHub staging secrets updated for $Repo"
