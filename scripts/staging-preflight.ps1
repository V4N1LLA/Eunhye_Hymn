param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$TerraformDir = "infra/aws",
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

  Add-Check $Name $false ("failed: " + $result.Output.Split("`n")[0])
}

Add-Check "aws cli" (Test-CommandExists "aws") "required"
Add-Check "terraform cli" (Test-CommandExists "terraform") "required"
Add-Check "gh cli" (Test-CommandExists "gh") "required"

if (-not (Test-CommandExists "aws") -or -not (Test-CommandExists "terraform") -or -not (Test-CommandExists "gh")) {
  $checks | Format-Table -AutoSize
  exit 1
}

$identityJson = aws sts get-caller-identity 2>$null
if ($LASTEXITCODE -ne 0) {
  Add-Check "aws sts get-caller-identity" $false "failed"
  $checks | Format-Table -AutoSize
  exit 1
}

$identity = $identityJson | ConvertFrom-Json
Add-Check "aws identity" $true ("arn=" + $identity.Arn)

gh auth status 1>$null 2>$null
Add-Check "gh auth status" ($LASTEXITCODE -eq 0) "github auth"

Test-AwsPermission "aws ec2 describe-availability-zones" "aws ec2 describe-availability-zones --output json"
Test-AwsPermission "aws ec2 describe-images" "aws ec2 describe-images --owners amazon --max-results 1 --output json"
Test-AwsPermission "aws ec2 describe-key-pairs" "aws ec2 describe-key-pairs --output json"

$init = Run-CommandCapture "terraform -chdir=$TerraformDir init -backend=false -input=false"
Add-Check "terraform init (backend=false)" ($init.ExitCode -eq 0) ($(if ($init.ExitCode -eq 0) { "ok" } else { "failed" }))

$validate = Run-CommandCapture "terraform -chdir=$TerraformDir validate"
Add-Check "terraform validate" ($validate.ExitCode -eq 0) ($(if ($validate.ExitCode -eq 0) { "ok" } else { "failed" }))

if (-not $SkipTerraformPlan) {
  $tfvarsPath = Join-Path $TerraformDir "terraform.tfvars"
  if (-not (Test-Path $tfvarsPath)) {
    Add-Check "terraform plan" $false "missing: $tfvarsPath"
  } else {
    $plan = Run-CommandCapture "terraform -chdir=$TerraformDir plan -input=false -lock=false -out=tfplan.preflight"
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
