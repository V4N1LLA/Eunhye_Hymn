param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Ref = "develop",
  [string]$Workflow = "deploy-staging.yml",
  [string]$AwsProfile = "",
  [switch]$AutoLogin,
  [switch]$SkipPreflight,
  [switch]$SkipTerraformPlan,
  [switch]$EnableAwsLogs,
  [switch]$DryRun,
  [int]$RunDetectRetries = 24,
  [int]$RunDetectIntervalSeconds = 5,
  [int]$WatchIntervalSeconds = 10,
  [string]$LogFile = "docs/staging-rehearsal-log.md"
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-CommandStrict {
  param(
    [string]$Name,
    [scriptblock]$Command
  )
  & $Command
  if ($LASTEXITCODE -ne 0) {
    throw "$Name failed with exit code $LASTEXITCODE"
  }
}

function Test-CommitShaRef {
  param([string]$Value)
  if ([string]::IsNullOrWhiteSpace($Value)) {
    return $false
  }
  return $Value -match "^[0-9a-fA-F]{40}$"
}

function Ensure-LogFile {
  param([string]$Path)
  if (Test-Path $Path) {
    return
  }

  @"
# Staging Rehearsal Log

스테이징 리허설 실행 기록 문서다. `scripts/staging-rehearsal.ps1`가 자동으로 행을 추가한다.

| UTC Time | Repo | Ref | Preflight | Workflow Run | Result | Notes |
|----------|------|-----|-----------|--------------|--------|-------|
"@ | Set-Content -Path $Path -Encoding utf8
}

function Append-LogRow {
  param(
    [string]$Path,
    [string]$RepoName,
    [string]$BranchRef,
    [string]$PreflightStatus,
    [string]$RunUrl,
    [string]$Result,
    [string]$Notes
  )

  Ensure-LogFile -Path $Path
  $utc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  Add-Content -Path $Path -Value "| $utc | $RepoName | $BranchRef | $PreflightStatus | $RunUrl | $Result | $Notes |" -Encoding utf8
}

if (-not (Test-CommandExists "gh")) {
  throw "gh CLI is required."
}

$preflightStatus = "SKIPPED"
$notes = "enable_awslogs=$($EnableAwsLogs.IsPresent.ToString().ToLower())"

if (-not $SkipPreflight) {
  $preflightScript = Join-Path $PSScriptRoot "staging-preflight.ps1"
  if (-not (Test-Path $preflightScript)) {
    throw "preflight script not found: $preflightScript"
  }

  $preflightArgs = @(
    "-File", $preflightScript,
    "-Repo", $Repo
  )
  if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) {
    $preflightArgs += @("-AwsProfile", $AwsProfile)
  }
  if ($AutoLogin) {
    $preflightArgs += "-AutoLogin"
  }
  if ($SkipTerraformPlan) {
    $preflightArgs += "-SkipTerraformPlan"
  }

  & powershell.exe @preflightArgs
  if ($LASTEXITCODE -ne 0) {
    $preflightStatus = "FAILED"
    Append-LogRow -Path $LogFile -RepoName $Repo -BranchRef $Ref -PreflightStatus $preflightStatus -RunUrl "-" -Result "ABORTED" -Notes "preflight failed"
    throw "staging preflight failed; rehearsal aborted."
  }
  $preflightStatus = "PASS"
}

$triggeredAfter = (Get-Date).ToUniversalTime()
$enableValue = if ($EnableAwsLogs) { "true" } else { "false" }

if (Test-CommitShaRef -Value $Ref) {
  throw "Ref must be a branch or tag for workflow_dispatch. Commit SHA is not accepted: $Ref"
}

if ($DryRun) {
  Write-Host "[DryRun] Repo: $Repo"
  Write-Host "[DryRun] Ref: $Ref"
  Write-Host "[DryRun] Workflow: $Workflow"
  Write-Host "[DryRun] enable_awslogs=$enableValue"
  Write-Host "[DryRun] Preflight: $preflightStatus"
  Write-Host "[DryRun] LogFile: $LogFile"
  exit 0
}

Invoke-CommandStrict -Name "gh workflow run" -Command {
  gh workflow run $Workflow --repo $Repo --ref $Ref -f "enable_awslogs=$enableValue"
}

$runId = $null
$runUrl = $null
for ($i = 0; $i -lt $RunDetectRetries; $i++) {
  $runJson = gh run list --repo $Repo --workflow $Workflow --branch $Ref --event workflow_dispatch --limit 5 --json databaseId,createdAt,url,status
  if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($runJson)) {
    $runs = $runJson | ConvertFrom-Json
    $candidate = $runs |
      Where-Object { ([DateTime]$_.createdAt).ToUniversalTime() -ge $triggeredAfter.AddSeconds(-5) } |
      Sort-Object { [DateTime]$_.createdAt } -Descending |
      Select-Object -First 1

    if ($null -ne $candidate) {
      $runId = "$($candidate.databaseId)"
      $runUrl = "$($candidate.url)"
      break
    }
  }
  Start-Sleep -Seconds $RunDetectIntervalSeconds
}

if ([string]::IsNullOrWhiteSpace($runId)) {
  Append-LogRow -Path $LogFile -RepoName $Repo -BranchRef $Ref -PreflightStatus $preflightStatus -RunUrl "-" -Result "FAILED" -Notes "could not detect workflow run id"
  throw "failed to detect workflow run id after trigger."
}

gh run watch $runId --repo $Repo --interval $WatchIntervalSeconds --exit-status
$watchExitCode = $LASTEXITCODE

$runView = gh run view $runId --repo $Repo --json conclusion,status,url,headSha,workflowName,createdAt,updatedAt
if ($LASTEXITCODE -ne 0) {
  $result = if ($watchExitCode -eq 0) { "SUCCESS" } else { "FAILED" }
  Append-LogRow -Path $LogFile -RepoName $Repo -BranchRef $Ref -PreflightStatus $preflightStatus -RunUrl $runUrl -Result $result -Notes "$notes, run_id=$runId"
  if ($watchExitCode -ne 0) {
    throw "workflow run failed (run id: $runId)."
  }
  Write-Host "Rehearsal completed: $runUrl"
  exit 0
}

$runInfo = $runView | ConvertFrom-Json
$conclusion = if ([string]::IsNullOrWhiteSpace($runInfo.conclusion)) { "unknown" } else { $runInfo.conclusion }
$result = if ($watchExitCode -eq 0) { "SUCCESS" } else { "FAILED" }
$summary = "$notes, run_id=$runId, conclusion=$conclusion, head_sha=$($runInfo.headSha)"

Append-LogRow -Path $LogFile -RepoName $Repo -BranchRef $Ref -PreflightStatus $preflightStatus -RunUrl $runInfo.url -Result $result -Notes $summary

if ($watchExitCode -ne 0) {
  throw "workflow run failed (run id: $runId, conclusion: $conclusion)."
}

Write-Host "Rehearsal completed successfully."
Write-Host "Run: $($runInfo.url)"
Write-Host "Recorded at: $LogFile"
exit 0
