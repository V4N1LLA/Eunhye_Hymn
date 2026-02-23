param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Branch = "staging",
  [string]$Owner = "codex",
  [string]$OpsHealthLogFile = "docs/ops-health-log.md",
  [switch]$SkipStagingOps,
  [switch]$SkipSecretsRotation,
  [int]$StagingMaxAgeMinutes = 120,
  [int]$ManualSmokeMaxAgeDays = 7,
  [switch]$SkipManualSmokeRecencyGate,
  [switch]$WaitForCompletion,
  [switch]$SkipPreflight,
  [switch]$SkipTerraformPlan,
  [string]$AwsProfile = "",
  [switch]$AutoLogin,
  [ValidateSet("", "PASS", "FAIL")]
  [string]$ManualSmokeResult = "",
  [string]$ManualSmokeEvidence = "",
  [string]$ManualSmokeNotes = "",
  [string]$StagingLogFile = "docs/staging-smoke-log.md",
  [int]$SecretsMaxAgeDays = 90,
  [switch]$IncludeMobileReleaseSecrets,
  [string]$SecretsLogFile = "docs/secrets-rotation-log.md",
  [switch]$AsJson
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
# Ops Health Cycle Log

Consolidated operations health log (staging gates + secret rotation audit).

| UTC Time | Repo | Branch | Staging | Secrets | Overall | FailureCategory | Evidence | Owner | Notes |
|----------|------|--------|---------|---------|---------|-----------------|----------|-------|-------|
"@ | Set-Content -Path $Path -Encoding utf8
}

function Append-LogRow {
  param(
    [string]$Path,
    [hashtable]$Row
  )

  Ensure-LogFile -Path $Path

  $line = "| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} |" -f `
    (Escape-MarkdownCell $Row.UtcTime),
    (Escape-MarkdownCell $Row.Repo),
    (Escape-MarkdownCell $Row.Branch),
    (Escape-MarkdownCell $Row.Staging),
    (Escape-MarkdownCell $Row.Secrets),
    (Escape-MarkdownCell $Row.Overall),
    (Escape-MarkdownCell $Row.FailureCategory),
    (Escape-MarkdownCell $Row.Evidence),
    (Escape-MarkdownCell $Row.Owner),
    (Escape-MarkdownCell $Row.Notes)

  Add-Content -Path $Path -Value $line -Encoding utf8
}

function Get-FirstUsefulLine {
  param([string]$Output)

  if ([string]::IsNullOrWhiteSpace($Output)) {
    return ""
  }

  $lines = $Output -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
  if ($lines.Count -eq 0) {
    return ""
  }

  foreach ($line in $lines) {
    if ($line -eq "System.Management.Automation.RemoteException") { continue }
    if ($line -like "At line:*") { continue }
    if ($line -like "+ CategoryInfo:*") { continue }
    if ($line -like "+ FullyQualifiedErrorId:*") { continue }
    return $line
  }

  return $lines[0]
}

function Invoke-PowerShellFile {
  param(
    [Parameter(Mandatory = $true)]
    [string]$ScriptPath,
    [string[]]$Arguments = @()
  )

  $output = & powershell.exe -NoProfile -File $ScriptPath @Arguments 2>&1
  $merged = ($output -join "`n")
  $json = $null
  if (-not [string]::IsNullOrWhiteSpace($merged)) {
    try {
      $json = $merged | ConvertFrom-Json
    } catch {
      $json = $null
    }
  }

  return [PSCustomObject]@{
    ExitCode = $LASTEXITCODE
    Output = $merged
    Json = $json
  }
}

function Get-FailureCategory {
  param(
    [string]$Step,
    [object]$Json,
    [string]$Output
  )

  if ($Step -eq "staging") {
    if ($null -ne $Json) {
      if ($Json.ManualSmoke -eq "OVERDUE" -or "$($Json.Notes)" -match "manual smoke") {
        return "manual_smoke_recency"
      }
      if ("$($Json.Notes)" -match "run too old") {
        return "deploy_freshness"
      }
      if ("$($Json.Notes)" -match "session expired|credentials missing|profile not found|preflight") {
        return "aws_preflight"
      }
      if ("$($Json.ErrorType)" -match "status_") {
        return "status_query_error"
      }
      if ($Json.Decision -eq "HOLD") {
        return "staging_hold"
      }
    }

    if ($Output -match "manual smoke") { return "manual_smoke_recency" }
    if ($Output -match "run too old") { return "deploy_freshness" }
    if ($Output -match "session expired|credentials missing|profile not found|preflight") { return "aws_preflight" }
    if ($Output -match "status error|status output is not valid json") { return "status_query_error" }
    return "staging_error"
  }

  if ($Step -eq "secrets") {
    if ($null -ne $Json) {
      if ([int]$Json.MissingCount -gt 0) { return "secret_missing" }
      if ([int]$Json.StaleCount -gt 0) { return "secret_stale" }
      if ([int]$Json.UnknownCount -gt 0) { return "secret_unknown" }
      if ($Json.Decision -eq "HOLD") { return "secrets_hold" }
    }

    if ($Output -match "missing:") { return "secret_missing" }
    if ($Output -match "stale:") { return "secret_stale" }
    if ($Output -match "unknown:") { return "secret_unknown" }
    return "secrets_error"
  }

  return "unknown_error"
}

if ($StagingMaxAgeMinutes -lt 0) {
  throw "StagingMaxAgeMinutes must be >= 0"
}
if ($ManualSmokeMaxAgeDays -lt 1) {
  throw "ManualSmokeMaxAgeDays must be >= 1"
}
if ($SecretsMaxAgeDays -lt 1) {
  throw "SecretsMaxAgeDays must be >= 1"
}
if ([string]::IsNullOrWhiteSpace($ManualSmokeResult) -and (
    -not [string]::IsNullOrWhiteSpace($ManualSmokeEvidence) -or
    -not [string]::IsNullOrWhiteSpace($ManualSmokeNotes)
  )) {
  throw "ManualSmokeEvidence/ManualSmokeNotes requires ManualSmokeResult."
}
if ($SkipStagingOps -and $SkipSecretsRotation) {
  throw "At least one step must run. Remove -SkipStagingOps or -SkipSecretsRotation."
}

$stagingScript = Join-Path $PSScriptRoot "staging-ops-cycle.ps1"
$secretsScript = Join-Path $PSScriptRoot "secrets-rotation-cycle.ps1"
if (-not $SkipStagingOps -and -not (Test-Path $stagingScript)) {
  throw "staging-ops-cycle.ps1 not found: $stagingScript"
}
if (-not $SkipSecretsRotation -and -not (Test-Path $secretsScript)) {
  throw "secrets-rotation-cycle.ps1 not found: $secretsScript"
}

$stagingResult = [PSCustomObject]@{
  Step = "staging"
  Result = "SKIPPED"
  Decision = "SKIPPED"
  Category = ""
  Evidence = "-"
  Notes = ""
  RawError = ""
}

$secretsResult = [PSCustomObject]@{
  Step = "secrets"
  Result = "SKIPPED"
  Decision = "SKIPPED"
  Category = ""
  Evidence = "-"
  Notes = ""
  RawError = ""
}

if (-not $SkipStagingOps) {
  $stagingArgs = @(
    "-Repo", $Repo,
    "-Branch", $Branch,
    "-Owner", $Owner,
    "-MaxAgeMinutes", "$StagingMaxAgeMinutes",
    "-ManualSmokeMaxAgeDays", "$ManualSmokeMaxAgeDays",
    "-LogFile", $StagingLogFile,
    "-AsJson"
  )
  if ($SkipPreflight) { $stagingArgs += "-SkipPreflight" }
  if ($SkipTerraformPlan) { $stagingArgs += "-SkipTerraformPlan" }
  if ($WaitForCompletion) { $stagingArgs += "-WaitForCompletion" }
  if ($AutoLogin) { $stagingArgs += "-AutoLogin" }
  if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) { $stagingArgs += @("-AwsProfile", $AwsProfile) }
  if ($SkipManualSmokeRecencyGate) { $stagingArgs += "-SkipManualSmokeRecencyGate" }
  if (-not [string]::IsNullOrWhiteSpace($ManualSmokeResult)) { $stagingArgs += @("-ManualSmokeResult", $ManualSmokeResult) }
  if (-not [string]::IsNullOrWhiteSpace($ManualSmokeEvidence)) { $stagingArgs += @("-ManualSmokeEvidence", $ManualSmokeEvidence) }
  if (-not [string]::IsNullOrWhiteSpace($ManualSmokeNotes)) { $stagingArgs += @("-ManualSmokeNotes", $ManualSmokeNotes) }

  $stagingInvocation = Invoke-PowerShellFile -ScriptPath $stagingScript -Arguments $stagingArgs
  if ($null -ne $stagingInvocation.Json) {
    $stagingResult.Decision = "$($stagingInvocation.Json.Decision)"
    $stagingResult.Evidence = if ([string]::IsNullOrWhiteSpace("$($stagingInvocation.Json.Evidence)")) { "-" } else { "$($stagingInvocation.Json.Evidence)" }
    $stagingResult.Notes = "$($stagingInvocation.Json.Notes)"
  } else {
    $stagingResult.Notes = "json parse failed"
    $stagingResult.RawError = Get-FirstUsefulLine -Output $stagingInvocation.Output
  }

  if ($stagingInvocation.ExitCode -eq 0) {
    $stagingResult.Result = "PASS"
    $stagingResult.Category = ""
  } else {
    $stagingResult.Result = if ($null -ne $stagingInvocation.Json) { "HOLD" } else { "ERROR" }
    $stagingResult.Category = Get-FailureCategory -Step "staging" -Json $stagingInvocation.Json -Output $stagingInvocation.Output
    if ($stagingResult.Result -eq "ERROR" -and [string]::IsNullOrWhiteSpace($stagingResult.RawError)) {
      $stagingResult.RawError = Get-FirstUsefulLine -Output $stagingInvocation.Output
    }
  }
}

if (-not $SkipSecretsRotation) {
  $secretsArgs = @(
    "-Repo", $Repo,
    "-MaxAgeDays", "$SecretsMaxAgeDays",
    "-Owner", $Owner,
    "-LogFile", $SecretsLogFile,
    "-AsJson"
  )
  if ($IncludeMobileReleaseSecrets) {
    $secretsArgs += "-IncludeMobileReleaseSecrets"
  }

  $secretsInvocation = Invoke-PowerShellFile -ScriptPath $secretsScript -Arguments $secretsArgs
  if ($null -ne $secretsInvocation.Json) {
    $secretsResult.Decision = "$($secretsInvocation.Json.Decision)"
    $secretsResult.Evidence = $SecretsLogFile
    $secretsResult.Notes = "$($secretsInvocation.Json.Notes)"
  } else {
    $secretsResult.Notes = "json parse failed"
    $secretsResult.RawError = Get-FirstUsefulLine -Output $secretsInvocation.Output
  }

  if ($secretsInvocation.ExitCode -eq 0) {
    $secretsResult.Result = "PASS"
    $secretsResult.Category = ""
  } else {
    $secretsResult.Result = if ($null -ne $secretsInvocation.Json) { "HOLD" } else { "ERROR" }
    $secretsResult.Category = Get-FailureCategory -Step "secrets" -Json $secretsInvocation.Json -Output $secretsInvocation.Output
    if ($secretsResult.Result -eq "ERROR" -and [string]::IsNullOrWhiteSpace($secretsResult.RawError)) {
      $secretsResult.RawError = Get-FirstUsefulLine -Output $secretsInvocation.Output
    }
  }
}

$overall = if ($stagingResult.Result -eq "ERROR" -or $secretsResult.Result -eq "ERROR") {
  "ERROR"
} elseif ($stagingResult.Result -eq "HOLD" -or $secretsResult.Result -eq "HOLD") {
  "HOLD"
} else {
  "PASS"
}

$failureCategories = @()
if (-not [string]::IsNullOrWhiteSpace($stagingResult.Category)) { $failureCategories += $stagingResult.Category }
if (-not [string]::IsNullOrWhiteSpace($secretsResult.Category)) { $failureCategories += $secretsResult.Category }
$failureCategory = if ($failureCategories.Count -eq 0) { "-" } else { ($failureCategories -join ",") }

$notes = @()
if ($stagingResult.Result -ne "SKIPPED") {
  $notes += ("staging={0}/{1}" -f $stagingResult.Result, $stagingResult.Decision)
}
if ($secretsResult.Result -ne "SKIPPED") {
  $notes += ("secrets={0}/{1}" -f $secretsResult.Result, $secretsResult.Decision)
}
if ($stagingResult.Result -eq "ERROR" -and -not [string]::IsNullOrWhiteSpace($stagingResult.RawError)) {
  $notes += ("stagingError=" + $stagingResult.RawError)
}
if ($secretsResult.Result -eq "ERROR" -and -not [string]::IsNullOrWhiteSpace($secretsResult.RawError)) {
  $notes += ("secretsError=" + $secretsResult.RawError)
}

$evidenceItems = @()
if (-not [string]::IsNullOrWhiteSpace($stagingResult.Evidence) -and $stagingResult.Evidence -ne "-") {
  $evidenceItems += $stagingResult.Evidence
}
if (-not [string]::IsNullOrWhiteSpace($secretsResult.Evidence) -and $secretsResult.Evidence -ne "-") {
  $evidenceItems += $secretsResult.Evidence
}
$evidence = if ($evidenceItems.Count -eq 0) { "-" } else { ($evidenceItems -join " ; ") }

$utcNow = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
Append-LogRow -Path $OpsHealthLogFile -Row @{
  UtcTime = $utcNow
  Repo = $Repo
  Branch = $Branch
  Staging = $stagingResult.Result
  Secrets = $secretsResult.Result
  Overall = $overall
  FailureCategory = $failureCategory
  Evidence = $evidence
  Owner = $Owner
  Notes = ($notes -join "; ")
}

$resultPayload = [PSCustomObject]@{
  UtcTime = $utcNow
  Repo = $Repo
  Branch = $Branch
  Owner = $Owner
  Overall = $overall
  FailureCategory = $failureCategory
  Evidence = $evidence
  Notes = ($notes -join "; ")
  LogFile = $OpsHealthLogFile
  Steps = [PSCustomObject]@{
    Staging = $stagingResult
    Secrets = $secretsResult
  }
}

if ($AsJson) {
  $resultPayload | ConvertTo-Json -Depth 8
} else {
  Write-Host "== Ops Health Cycle =="
  Write-Host ("Repo: {0}" -f $Repo)
  Write-Host ("Branch: {0}" -f $Branch)
  Write-Host ("Staging: {0} ({1})" -f $stagingResult.Result, $stagingResult.Decision)
  Write-Host ("Secrets: {0} ({1})" -f $secretsResult.Result, $secretsResult.Decision)
  Write-Host ("Overall: {0}" -f $overall)
  Write-Host ("FailureCategory: {0}" -f $failureCategory)
  Write-Host ("Evidence: {0}" -f $evidence)
  Write-Host ("Log: {0}" -f $OpsHealthLogFile)
}

if ($overall -eq "PASS") {
  exit 0
}

exit 1
