param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Branch = "staging",
  [int]$MaxAgeMinutes = 120,
  [string]$AwsProfile = "",
  [switch]$AutoLogin,
  [string]$Owner = "codex",
  [string]$LogFile = "docs/staging-smoke-log.md",
  [switch]$SkipPreflight,
  [switch]$SkipTerraformPlan,
  [switch]$WaitForCompletion
)

$ErrorActionPreference = "Stop"

function Escape-MarkdownCell {
  param([string]$Value)

  if ($null -eq $Value) {
    return ""
  }

  return $Value.Replace("|", "\|").Trim()
}

function Ensure-LogFile {
  param([string]$Path)

  if (Test-Path $Path) {
    return
  }

  @"
# Staging Smoke Cycle Log

Operational cycle log (Preflight + deploy gates + manual smoke result).

| UTC Time | Repo | Branch | RunId | HeadSha | Preflight | Run | Deploy | Verify | AgeMin | Decision | Manual Smoke | Evidence | Owner | Notes |
|----------|------|--------|-------|---------|-----------|-----|--------|--------|--------|----------|--------------|----------|-------|-------|
"@ | Set-Content -Path $Path -Encoding utf8
}

function Append-LogRow {
  param(
    [string]$Path,
    [hashtable]$Row
  )

  Ensure-LogFile -Path $Path

  $line = "| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} | {11} | {12} | {13} | {14} |" -f `
    (Escape-MarkdownCell $Row.UtcTime),
    (Escape-MarkdownCell $Row.Repo),
    (Escape-MarkdownCell $Row.Branch),
    (Escape-MarkdownCell $Row.RunId),
    (Escape-MarkdownCell $Row.HeadSha),
    (Escape-MarkdownCell $Row.Preflight),
    (Escape-MarkdownCell $Row.RunGate),
    (Escape-MarkdownCell $Row.DeployGate),
    (Escape-MarkdownCell $Row.VerifyGate),
    (Escape-MarkdownCell $Row.AgeMin),
    (Escape-MarkdownCell $Row.Decision),
    (Escape-MarkdownCell $Row.ManualSmoke),
    (Escape-MarkdownCell $Row.Evidence),
    (Escape-MarkdownCell $Row.Owner),
    (Escape-MarkdownCell $Row.Notes)

  Add-Content -Path $Path -Value $line -Encoding utf8
}

function Invoke-PowerShellFile {
  param(
    [Parameter(Mandatory = $true)]
    [string]$ScriptPath,
    [string[]]$Arguments = @()
  )

  $output = & powershell.exe -NoProfile -File $ScriptPath @Arguments 2>&1
  return [PSCustomObject]@{
    ExitCode = $LASTEXITCODE
    Output = ($output -join "`n")
  }
}

if ($MaxAgeMinutes -lt 0) {
  throw "MaxAgeMinutes must be >= 0"
}

$preflightState = "SKIPPED"
$notes = @()

if (-not $SkipPreflight) {
  $preflightScript = Join-Path $PSScriptRoot "staging-preflight.ps1"
  if (-not (Test-Path $preflightScript)) {
    throw "staging-preflight.ps1 not found: $preflightScript"
  }

  $preflightArgs = @("-Repo", $Repo)
  if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) {
    $preflightArgs += @("-AwsProfile", $AwsProfile)
  }
  if ($AutoLogin) {
    $preflightArgs += "-AutoLogin"
  }
  if ($SkipTerraformPlan) {
    $preflightArgs += "-SkipTerraformPlan"
  }

  $preflightResult = Invoke-PowerShellFile -ScriptPath $preflightScript -Arguments $preflightArgs
  if ($preflightResult.ExitCode -eq 0) {
    $preflightState = "PASS"
  } else {
    $preflightState = "FAIL"
    if ($preflightResult.Output -match "session expired") {
      $notes += "preflight session expired"
    } elseif ($preflightResult.Output -match "credentials missing") {
      $notes += "preflight credentials missing"
    } elseif ($preflightResult.Output -match "aws profile not found") {
      $notes += "preflight profile not found"
    } else {
      $notes += "preflight failed"
    }
  }
}

$statusScript = Join-Path $PSScriptRoot "staging-latest-status.ps1"
if (-not (Test-Path $statusScript)) {
  throw "staging-latest-status.ps1 not found: $statusScript"
}

$statusArgs = @(
  "-Repo", $Repo,
  "-Branch", $Branch,
  "-Limit", "10",
  "-AsJson"
)
if ($WaitForCompletion) {
  $statusArgs += @("-Wait", "-WatchIntervalSeconds", "10")
} else {
  $statusArgs += "-PreferCompleted"
}

$statusResult = Invoke-PowerShellFile -ScriptPath $statusScript -Arguments $statusArgs
if ($statusResult.ExitCode -ne 0) {
  $tail = if ([string]::IsNullOrWhiteSpace($statusResult.Output)) { "status command failed" } else { $statusResult.Output.Split("`n")[-1].Trim() }
  Append-LogRow -Path $LogFile -Row @{
    UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Repo = $Repo
    Branch = $Branch
    RunId = "-"
    HeadSha = "-"
    Preflight = $preflightState
    RunGate = "FAIL"
    DeployGate = "FAIL"
    VerifyGate = "FAIL"
    AgeMin = "-"
    Decision = "HOLD"
    ManualSmoke = "NOT_STARTED"
    Evidence = "-"
    Owner = $Owner
    Notes = "status error: $tail"
  }
  throw "staging status check failed: $tail"
}

try {
  $statusPayload = $statusResult.Output | ConvertFrom-Json
} catch {
  Append-LogRow -Path $LogFile -Row @{
    UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Repo = $Repo
    Branch = $Branch
    RunId = "-"
    HeadSha = "-"
    Preflight = $preflightState
    RunGate = "FAIL"
    DeployGate = "FAIL"
    VerifyGate = "FAIL"
    AgeMin = "-"
    Decision = "HOLD"
    ManualSmoke = "NOT_STARTED"
    Evidence = "-"
    Owner = $Owner
    Notes = "status output is not valid json"
  }
  throw "failed to parse status json output"
}

$summary = $statusPayload.summary
$runGate = ($summary.Conclusion -eq "success")
$deployGate = ($summary.DeployJobConclusion -eq "success")
$verifyGate = ($summary.VerifyStepConclusion -eq "success")
$ageGate = ($MaxAgeMinutes -eq 0 -or [double]$summary.RunAgeMinutes -le $MaxAgeMinutes)
$preflightGate = ($preflightState -ne "FAIL")

$decision = if ($runGate -and $deployGate -and $verifyGate -and $ageGate -and $preflightGate) {
  "CONDITIONAL_GO"
} else {
  "HOLD"
}

$manualSmoke = if ($decision -eq "CONDITIONAL_GO") { "PENDING" } else { "BLOCKED" }

if (-not $ageGate) {
  $notes += "run too old(age=$($summary.RunAgeMinutes), max=$MaxAgeMinutes)"
}
if (-not $runGate) {
  $notes += "run conclusion=$($summary.Conclusion)"
}
if (-not $deployGate) {
  $notes += "deploy=$($summary.DeployJobConclusion)"
}
if (-not $verifyGate) {
  $notes += "verify=$($summary.VerifyStepConclusion)"
}
if (-not $preflightGate) {
  $notes += "preflight failed"
}

Append-LogRow -Path $LogFile -Row @{
  UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  Repo = $Repo
  Branch = $Branch
  RunId = "$($summary.RunId)"
  HeadSha = "$($summary.HeadSha)"
  Preflight = $preflightState
  RunGate = $(if ($runGate) { "PASS" } else { "FAIL" })
  DeployGate = $(if ($deployGate) { "PASS" } else { "FAIL" })
  VerifyGate = $(if ($verifyGate) { "PASS" } else { "FAIL" })
  AgeMin = "$($summary.RunAgeMinutes)"
  Decision = $decision
  ManualSmoke = $manualSmoke
  Evidence = "$($summary.Url)"
  Owner = $Owner
  Notes = ($notes -join "; ")
}

Write-Host "== Staging Ops Cycle =="
Write-Host ("Repo: {0}" -f $Repo)
Write-Host ("Branch: {0}" -f $Branch)
Write-Host ("RunId: {0}" -f $summary.RunId)
Write-Host ("HeadSha: {0}" -f $summary.HeadSha)
Write-Host ("Preflight: {0}" -f $preflightState)
Write-Host ("Gate(run/deploy/verify/age): {0}/{1}/{2}/{3}" -f $runGate, $deployGate, $verifyGate, $ageGate)
Write-Host ("Decision: {0}" -f $decision)
Write-Host ("Log: {0}" -f $LogFile)
Write-Host ""
Write-Host "Update manual smoke results in:"
Write-Host "- docs/staging-smoke-checklist.md"
Write-Host "- docs/staging-feedback-checklist.md"

if ($decision -eq "CONDITIONAL_GO") {
  exit 0
}

exit 1
