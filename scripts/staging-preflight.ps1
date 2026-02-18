param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$TerraformDir = "infra/aws",
  [string]$AwsProfile = "",
  [switch]$SkipTerraformPlan
)

$ErrorActionPreference = "Stop"

$requiredSecrets = @(
  "AWS_ACCESS_KEY_ID",
  "AWS_SECRET_ACCESS_KEY",
  "AWS_REGION",
  "ECR_REGISTRY",
  "EC2_HOST",
  "EC2_SSH_KEY",
  "DEPLOY_ENV_FILE"
)

$checks = @()

function Add-Check {
  param(
    [string]$Name,
    [bool]$Passed,
    [string]$Detail
  )

  $script:checks += [PSCustomObject]@{
    Name   = $Name
    Passed = $Passed
    Detail = $Detail
  }
}

function Set-ProcessEnvVar {
  param(
    [string]$Name,
    [string]$Value
  )

  if ([string]::IsNullOrWhiteSpace($Value)) {
    Remove-Item ("Env:" + $Name) -ErrorAction SilentlyContinue
    return
  }

  Set-Item ("Env:" + $Name) $Value
}

function Test-CommandExists {
  param([string]$Name)
  $cmd = Get-Command $Name -ErrorAction SilentlyContinue
  return $null -ne $cmd
}

function Run-CommandCapture {
  param([string]$Command)
  $previousErrorAction = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $output = & $env:ComSpec /d /c $Command 2>&1
    $exitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $previousErrorAction
  }
  return [PSCustomObject]@{
    ExitCode = $exitCode
    Output   = (($output -join "`n") -replace "`e\[[\d;]*[A-Za-z]", "")
  }
}

function Get-FirstUsefulLine {
  param([string]$Output)

  if ([string]::IsNullOrWhiteSpace($Output)) {
    return ""
  }

  $lines = $Output -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
  if ($null -eq $lines -or $lines.Count -eq 0) {
    return ""
  }

  foreach ($line in $lines) {
    if ($line -eq "System.Management.Automation.RemoteException") {
      continue
    }
    if ($line -like "At line:*") {
      continue
    }
    if ($line -like "+ CategoryInfo:*") {
      continue
    }
    if ($line -like "+ FullyQualifiedErrorId:*") {
      continue
    }
    return $line
  }

  return $lines[0]
}

function Test-AwsPermission {
  param(
    [string]$Name,
    [string]$Command
  )

  $result = Run-CommandCapture $Command
  if ($result.ExitCode -eq 0) {
    Add-Check $Name $true "ok"
    return
  }

  if ($result.Output -match "UnauthorizedOperation|AccessDenied|AccessDeniedException") {
    Add-Check $Name $false "permission denied"
    return
  }

  $detailLine = Get-FirstUsefulLine -Output $result.Output
  if ([string]::IsNullOrWhiteSpace($detailLine)) {
    $detailLine = "command failed"
  }
  Add-Check $Name $false ("failed: " + $detailLine)
}

function Build-AwsCommand {
  param([string]$Inner)
  if ([string]::IsNullOrWhiteSpace($AwsProfile)) {
    return "aws $Inner"
  }
  return "aws --profile $AwsProfile $Inner"
}

function Export-AwsCredentialsToEnvironment {
  $exportArgs = @("configure", "export-credentials", "--format", "process")
  if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) {
    $exportArgs += @("--profile", $AwsProfile)
  }

  $output = & aws @exportArgs 2>$null
  if ($LASTEXITCODE -ne 0) {
    return $false
  }

  $json = $output -join "`n"
  if ([string]::IsNullOrWhiteSpace($json)) {
    return $false
  }

  try {
    $creds = $json | ConvertFrom-Json
  } catch {
    return $false
  }

  if ([string]::IsNullOrWhiteSpace($creds.AccessKeyId) -or [string]::IsNullOrWhiteSpace($creds.SecretAccessKey)) {
    return $false
  }

  Set-ProcessEnvVar -Name "AWS_ACCESS_KEY_ID" -Value $creds.AccessKeyId
  Set-ProcessEnvVar -Name "AWS_SECRET_ACCESS_KEY" -Value $creds.SecretAccessKey
  Set-ProcessEnvVar -Name "AWS_SESSION_TOKEN" -Value $creds.SessionToken
  Set-ProcessEnvVar -Name "AWS_CREDENTIAL_EXPIRATION" -Value $creds.Expiration
  return $true
}

Add-Check "aws cli" (Test-CommandExists "aws") "required"
Add-Check "terraform cli" (Test-CommandExists "terraform") "required"
Add-Check "gh cli" (Test-CommandExists "gh") "required"

if (-not (Test-CommandExists "aws") -or -not (Test-CommandExists "terraform") -or -not (Test-CommandExists "gh")) {
  $checks | Format-Table -AutoSize
  exit 1
}

$identityCommand = Build-AwsCommand "sts get-caller-identity"
$identityResult = Run-CommandCapture $identityCommand
if ($identityResult.ExitCode -ne 0) {
  $identityDetail = "failed"
  if ($identityResult.Output -match "Unable to locate credentials|NoCredentialProviders") {
    $identityDetail = "credentials missing (run aws configure / aws configure sso / aws login)"
  } elseif ($identityResult.Output -match "Your session has expired|ExpiredToken|Token has expired and refresh failed|The SSO session associated with this profile has expired") {
    $identityDetail = "session expired (run aws sso login / aws login)"
  } elseif ($identityResult.Output -match "The config profile .* could not be found") {
    $identityDetail = "aws profile not found"
  } elseif (-not [string]::IsNullOrWhiteSpace($identityResult.Output)) {
    $detailLine = Get-FirstUsefulLine -Output $identityResult.Output
    if ([string]::IsNullOrWhiteSpace($detailLine)) {
      $detailLine = "command failed"
    }
    $identityDetail = "failed: " + $detailLine
  }

  Add-Check "aws sts get-caller-identity" $false $identityDetail
  $checks | Format-Table -AutoSize
  exit 1
}

try {
  $identity = $identityResult.Output | ConvertFrom-Json
} catch {
  Add-Check "aws sts get-caller-identity" $false "failed: invalid json response"
  $checks | Format-Table -AutoSize
  exit 1
}

Add-Check "aws identity" $true ("arn=" + $identity.Arn)

gh auth status 1>$null 2>$null
Add-Check "gh auth status" ($LASTEXITCODE -eq 0) "github auth"

Test-AwsPermission "aws ec2 describe-availability-zones" (Build-AwsCommand "ec2 describe-availability-zones --output json")
Test-AwsPermission "aws ec2 describe-images" (Build-AwsCommand "ec2 describe-images --owners amazon --query ""Images[0].ImageId"" --output text")
Test-AwsPermission "aws ec2 describe-key-pairs" (Build-AwsCommand "ec2 describe-key-pairs --output json")

$terraformEnvNames = @(
  "AWS_ACCESS_KEY_ID",
  "AWS_SECRET_ACCESS_KEY",
  "AWS_SESSION_TOKEN",
  "AWS_CREDENTIAL_EXPIRATION",
  "AWS_PROFILE",
  "AWS_DEFAULT_PROFILE"
)
$terraformEnvBackup = @{}
foreach ($name in $terraformEnvNames) {
  $terraformEnvBackup[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
}

try {
  $null = Export-AwsCredentialsToEnvironment
  if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) {
    Set-ProcessEnvVar -Name "AWS_PROFILE" -Value $AwsProfile
    Set-ProcessEnvVar -Name "AWS_DEFAULT_PROFILE" -Value $AwsProfile
  }

  $init = Run-CommandCapture ('terraform -chdir="{0}" init -backend=false -input=false' -f $TerraformDir)
  Add-Check "terraform init (backend=false)" ($init.ExitCode -eq 0) ($(if ($init.ExitCode -eq 0) { "ok" } else { "failed" }))

  $validate = Run-CommandCapture ('terraform -chdir="{0}" validate' -f $TerraformDir)
  Add-Check "terraform validate" ($validate.ExitCode -eq 0) ($(if ($validate.ExitCode -eq 0) { "ok" } else { "failed" }))

  if (-not $SkipTerraformPlan) {
    $tfvarsPath = Join-Path $TerraformDir "terraform.tfvars"
    if (-not (Test-Path $tfvarsPath)) {
      Add-Check "terraform plan" $false "missing: $tfvarsPath"
    } else {
      $plan = Run-CommandCapture ('terraform -chdir="{0}" plan -detailed-exitcode -input=false -lock=false -out tfplan.preflight' -f $TerraformDir)
      if ($plan.ExitCode -eq 0) {
        Add-Check "terraform plan" $true "no changes"
      } elseif ($plan.ExitCode -eq 2) {
        Add-Check "terraform plan" $true "changes detected"
      } else {
        Add-Check "terraform plan" $false "failed (run terraform plan for details)"
      }
      if (Test-Path "$TerraformDir/tfplan.preflight") {
        Remove-Item "$TerraformDir/tfplan.preflight" -Force
      }
    }
  }
} finally {
  foreach ($name in $terraformEnvNames) {
    Set-ProcessEnvVar -Name $name -Value $terraformEnvBackup[$name]
  }
}

$secretNames = gh secret list --repo $Repo --json name --jq '.[].name' 2>$null
if ($LASTEXITCODE -ne 0) {
  Add-Check "gh secret list" $false "failed"
} else {
  $existing = @{}
  foreach ($name in $secretNames) {
    $existing[$name.Trim()] = $true
  }

  $missing = @()
  foreach ($required in $requiredSecrets) {
    if (-not $existing.ContainsKey($required)) {
      $missing += $required
    }
  }

  if ($missing.Count -eq 0) {
    Add-Check "github required secrets" $true "all set"
  } else {
    Add-Check "github required secrets" $false ("missing: " + ($missing -join ", "))
  }
}

$checks | Format-Table -AutoSize

$failed = @($checks | Where-Object { -not $_.Passed })
if ($failed.Count -gt 0) {
  exit 1
}

exit 0
