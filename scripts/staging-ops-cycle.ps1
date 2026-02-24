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
  [switch]$WaitForCompletion,
  [ValidateSet("", "PASS", "FAIL")]
  [string]$ManualSmokeResult = "",
  [string]$ManualSmokeEvidence = "",
  [string]$ManualSmokeNotes = "",
  [int]$ManualSmokeMaxAgeDays = 7,
  [switch]$SkipManualSmokeRecencyGate,
  [int]$LogDedupWindowMinutes = 30,
  [switch]$AsJson
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-PowerShellCommand {
  if (Test-CommandExists "powershell.exe") {
    return "powershell.exe"
  }
  if (Test-CommandExists "pwsh") {
    return "pwsh"
  }
  return ""
}

function Escape-MarkdownCell {
  param([string]$Value)

  if ($null -eq $Value) {
    return ""
  }

  return $Value.Replace("|", "\|").Trim()
}

function Ensure-ParentDirectory {
  param([string]$Path)

  $directory = Split-Path -Path $Path -Parent
  if (-not [string]::IsNullOrWhiteSpace($directory) -and -not (Test-Path $directory)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
  }
}

function Ensure-LogFile {
  param([string]$Path)

  Ensure-ParentDirectory -Path $Path

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

function Get-LatestLogRow {
  param([string]$Path)

  if (-not (Test-Path $Path)) {
    return $null
  }

  $lines = Get-Content -Path $Path -Encoding utf8
  for ($index = $lines.Count - 1; $index -ge 0; $index--) {
    $line = $lines[$index]
    $trimmed = $line.Trim()
    if (-not $trimmed.StartsWith("|")) {
      continue
    }
    if ($trimmed -match "^\|\s*-+\s*\|") {
      continue
    }

    $parts = $line.Split("|")
    if ($parts.Count -lt 16) {
      continue
    }

    [datetime]$rowUtc = [datetime]::MinValue
    if (-not [datetime]::TryParse($parts[1].Trim(), [ref]$rowUtc)) {
      continue
    }

    return [PSCustomObject]@{
      UtcTime = $rowUtc.ToUniversalTime()
      Repo = $parts[2].Trim()
      Branch = $parts[3].Trim()
      RunId = $parts[4].Trim()
      HeadSha = $parts[5].Trim()
      Preflight = $parts[6].Trim()
      RunGate = $parts[7].Trim()
      DeployGate = $parts[8].Trim()
      VerifyGate = $parts[9].Trim()
      AgeMin = $parts[10].Trim()
      Decision = $parts[11].Trim()
      ManualSmoke = $parts[12].Trim()
      Evidence = $parts[13].Trim()
      Owner = $parts[14].Trim()
      Notes = $parts[15].Trim()
    }
  }

  return $null
}

function Should-SkipLogAppend {
  param(
    [string]$Path,
    [hashtable]$Row,
    [int]$WindowMinutes = 30
  )

  if ($WindowMinutes -le 0) {
    return $false
  }

  $latest = Get-LatestLogRow -Path $Path
  if ($null -eq $latest) {
    return $false
  }

  if ((([DateTime]::UtcNow - $latest.UtcTime).TotalMinutes) -gt $WindowMinutes) {
    return $false
  }

  if ("$($latest.Repo)" -ne "$($Row.Repo)") { return $false }
  if ("$($latest.Branch)" -ne "$($Row.Branch)") { return $false }
  if ("$($latest.RunId)" -ne "$($Row.RunId)") { return $false }
  if ("$($latest.HeadSha)" -ne "$($Row.HeadSha)") { return $false }
  if ("$($latest.Preflight)" -ne "$($Row.Preflight)") { return $false }
  if ("$($latest.RunGate)" -ne "$($Row.RunGate)") { return $false }
  if ("$($latest.DeployGate)" -ne "$($Row.DeployGate)") { return $false }
  if ("$($latest.VerifyGate)" -ne "$($Row.VerifyGate)") { return $false }
  if ("$($latest.Decision)" -ne "$($Row.Decision)") { return $false }
  if ("$($latest.ManualSmoke)" -ne "$($Row.ManualSmoke)") { return $false }

  return $true
}

function Invoke-PowerShellFile {
  param(
    [Parameter(Mandatory = $true)]
    [string]$ScriptPath,
    [string[]]$Arguments = @()
  )

  if ([string]::IsNullOrWhiteSpace($script:PowerShellCommand)) {
    throw "missing powershell command (powershell.exe/pwsh)"
  }

  $exitCode = 0
  $outputText = ""
  try {
    $output = & $script:PowerShellCommand -NoProfile -File $ScriptPath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
    $outputText = ($output -join "`n")
  } catch {
    $exitCode = if ($LASTEXITCODE -ne 0) { $LASTEXITCODE } else { 1 }
    $outputText = "$($_.Exception.Message)"
  }

  return [PSCustomObject]@{
    ExitCode = $exitCode
    Output = $outputText
  }
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

function Get-LatestManualSmokeRecord {
  param(
    [string]$Path,
    [string]$Repo,
    [string]$Branch
  )

  if (-not (Test-Path $Path)) {
    return $null
  }

  $lines = Get-Content -Path $Path -Encoding utf8
  $latest = $null

  foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if (-not $trimmed.StartsWith("|")) {
      continue
    }
    if ($trimmed -match "^\|\s*-+\s*\|") {
      continue
    }

    $parts = $line.Split("|")
    if ($parts.Count -lt 16) {
      continue
    }

    $rowUtc = $parts[1].Trim()
    $rowRepo = $parts[2].Trim()
    $rowBranch = $parts[3].Trim()
    $rowManualSmoke = $parts[12].Trim().ToUpperInvariant()
    if (-not ($rowManualSmoke -eq "PASS" -or $rowManualSmoke -eq "FAIL")) {
      continue
    }
    if (-not [string]::IsNullOrWhiteSpace($Repo) -and $rowRepo -ne $Repo) {
      continue
    }
    if (-not [string]::IsNullOrWhiteSpace($Branch) -and $rowBranch -ne $Branch) {
      continue
    }

    [datetime]$rowUtcParsed = [datetime]::MinValue
    if (-not [datetime]::TryParse($rowUtc, [ref]$rowUtcParsed)) {
      continue
    }
    $rowUtcParsed = $rowUtcParsed.ToUniversalTime()

    if ($null -eq $latest -or $rowUtcParsed -gt $latest.UtcTime) {
      $latest = [PSCustomObject]@{
        UtcTime = $rowUtcParsed
        Repo = $rowRepo
        Branch = $rowBranch
        ManualSmoke = $rowManualSmoke
        Evidence = $parts[13].Trim()
        Owner = $parts[14].Trim()
        Notes = $parts[15].Trim()
      }
    }
  }

  return $latest
}

function Get-ManualSmokeEvidencePortion {
  param([string]$EvidenceText)

  if ([string]::IsNullOrWhiteSpace($EvidenceText)) {
    return ""
  }

  $parts = @($EvidenceText -split ";" | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
  if ($parts.Count -le 1) {
    return ""
  }

  return ($parts[1..($parts.Count - 1)] -join " ; ")
}

function Test-EvidenceReference {
  param([string]$Value)

  if ([string]::IsNullOrWhiteSpace($Value)) {
    return $false
  }

  $trimmed = $Value.Trim()
  if ($trimmed -eq "-") {
    return $false
  }
  if ($trimmed -match "https?://") {
    return $true
  }
  if ($trimmed -match "\[[^\]]+\]\([^)]+\)") {
    return $true
  }
  if ($trimmed -match "(^|[ ;])(docs|apps|infra|scripts|\.github)/[A-Za-z0-9._/\-]+") {
    return $true
  }
  if ($trimmed -match "(^|[ ;])[A-Za-z0-9._/\-]+#L\d+") {
    return $true
  }

  return $false
}

function Get-FailureCategoryCode {
  param(
    [bool]$RunGate,
    [bool]$DeployGate,
    [bool]$VerifyGate,
    [bool]$AgeGate,
    [bool]$PreflightGate,
    [bool]$ManualSmokeRecencyGate,
    [bool]$ManualSmokeOutcomeGate,
    [bool]$HasEvidenceWarning
  )

  if (-not $RunGate) { return "run_conclusion" }
  if (-not $DeployGate) { return "deploy_job" }
  if (-not $VerifyGate) { return "verify_step" }
  if (-not $AgeGate) { return "deploy_freshness" }
  if (-not $PreflightGate) { return "aws_preflight" }
  if (-not $ManualSmokeRecencyGate) { return "manual_smoke_recency" }
  if (-not $ManualSmokeOutcomeGate) { return "manual_smoke_failed" }
  if ($HasEvidenceWarning) { return "manual_smoke_evidence_warn" }

  return "-"
}

function Get-FailureDetail {
  param(
    [string]$FailureCategory,
    [string]$RunConclusion,
    [string]$DeployConclusion,
    [string]$VerifyConclusion,
    [string]$RunAgeMinutes,
    [int]$MaxAgeMinutes,
    [string[]]$Notes = @(),
    [string[]]$EvidenceWarnings = @()
  )

  switch ($FailureCategory) {
    "run_conclusion" { return ("run conclusion=" + $RunConclusion) }
    "deploy_job" { return ("deploy=" + $DeployConclusion) }
    "verify_step" { return ("verify=" + $VerifyConclusion) }
    "deploy_freshness" { return ("run too old(age=" + $RunAgeMinutes + ", max=" + $MaxAgeMinutes + ")") }
    "aws_preflight" {
      $preflightDetail = @($Notes | Where-Object { $_ -like "preflight*" } | Select-Object -First 1)
      if ($preflightDetail.Count -gt 0) { return "$($preflightDetail[0])" }
      return "preflight failed"
    }
    "manual_smoke_recency" {
      $recencyDetail = @($Notes | Where-Object { $_ -match "manual smoke (record missing|stale|recency gate failed)" } | Select-Object -First 1)
      if ($recencyDetail.Count -gt 0) { return "$($recencyDetail[0])" }
      return "manual smoke recency gate failed"
    }
    "manual_smoke_failed" { return "manual smoke outcome gate failed" }
    "manual_smoke_evidence_warn" {
      if ($EvidenceWarnings.Count -gt 0) {
        return ("evidence warning: " + $EvidenceWarnings[0])
      }
      return "manual smoke evidence warning"
    }
    default { return "-" }
  }
}

$script:PowerShellCommand = Get-PowerShellCommand
if ([string]::IsNullOrWhiteSpace($script:PowerShellCommand)) {
  throw "missing powershell command (powershell.exe/pwsh)"
}

if ($MaxAgeMinutes -lt 0) {
  throw "MaxAgeMinutes must be >= 0"
}
if ($ManualSmokeMaxAgeDays -lt 1) {
  throw "ManualSmokeMaxAgeDays must be >= 1"
}
if ($LogDedupWindowMinutes -lt 0) {
  throw "LogDedupWindowMinutes must be >= 0"
}
if ([string]::IsNullOrWhiteSpace($ManualSmokeResult) -and (
    -not [string]::IsNullOrWhiteSpace($ManualSmokeEvidence) -or
    -not [string]::IsNullOrWhiteSpace($ManualSmokeNotes)
  )) {
  throw "ManualSmokeEvidence/ManualSmokeNotes requires ManualSmokeResult."
}

$preflightState = "SKIPPED"
$notes = @()
$evidenceWarnings = @()

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
  $tail = Get-FirstUsefulLine -Output $statusResult.Output
  if ([string]::IsNullOrWhiteSpace($tail)) {
    $tail = "status command failed"
  }
  $errorUtc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  $errorRow = @{
    UtcTime = $errorUtc
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
  if (-not (Should-SkipLogAppend -Path $LogFile -Row $errorRow -WindowMinutes $LogDedupWindowMinutes)) {
    Append-LogRow -Path $LogFile -Row $errorRow
  }
  if ($AsJson) {
    [PSCustomObject]@{
      UtcTime = $errorUtc
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
      ManualSmokeAgeDays = "-"
      FailureCategory = "status_command"
      FailureDetail = $tail
      Evidence = "-"
      Owner = $Owner
      Notes = "status error: $tail"
      ErrorType = "status_command_failed"
    } | ConvertTo-Json -Depth 6
    exit 1
  }
  throw "staging status check failed: $tail"
}

try {
  $statusPayload = $statusResult.Output | ConvertFrom-Json
} catch {
  $errorUtc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  $errorRow = @{
    UtcTime = $errorUtc
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
  if (-not (Should-SkipLogAppend -Path $LogFile -Row $errorRow -WindowMinutes $LogDedupWindowMinutes)) {
    Append-LogRow -Path $LogFile -Row $errorRow
  }
  if ($AsJson) {
    [PSCustomObject]@{
      UtcTime = $errorUtc
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
      ManualSmokeAgeDays = "-"
      FailureCategory = "status_json_parse"
      FailureDetail = "status output is not valid json"
      Evidence = "-"
      Owner = $Owner
      Notes = "status output is not valid json"
      ErrorType = "status_json_parse_failed"
    } | ConvertTo-Json -Depth 6
    exit 1
  }
  throw "failed to parse status json output"
}

$summary = $statusPayload.summary
$runGate = ($summary.Conclusion -eq "success")
$deployGate = ($summary.DeployJobConclusion -eq "success")
$verifyGate = ($summary.VerifyStepConclusion -eq "success")
$ageGate = ($MaxAgeMinutes -eq 0 -or [double]$summary.RunAgeMinutes -le $MaxAgeMinutes)
$preflightGate = ($preflightState -ne "FAIL")
$manualSmokeRecencyGate = $true
$manualSmokeOutcomeGate = $true
$manualSmokeAgeDaysDisplay = "-"
$latestManualSmoke = $null

if (-not [string]::IsNullOrWhiteSpace($ManualSmokeResult)) {
  $manualSmokeAgeDaysDisplay = "0"
  if ($ManualSmokeResult -eq "FAIL") {
    $manualSmokeOutcomeGate = $false
    $notes += "manual smoke result=FAIL"
  }
  if ([string]::IsNullOrWhiteSpace($ManualSmokeEvidence)) {
    $evidenceWarnings += "manual smoke evidence missing"
  } elseif (-not (Test-EvidenceReference -Value $ManualSmokeEvidence)) {
    $evidenceWarnings += "manual smoke evidence has no link/reference"
  }
} elseif (-not $SkipManualSmokeRecencyGate) {
  $latestManualSmoke = Get-LatestManualSmokeRecord -Path $LogFile -Repo $Repo -Branch $Branch
  if ($null -eq $latestManualSmoke) {
    $manualSmokeRecencyGate = $false
    $notes += "manual smoke record missing"
  } else {
    $manualSmokeAgeDays = ([DateTime]::UtcNow - $latestManualSmoke.UtcTime).TotalDays
    if ($manualSmokeAgeDays -lt 0) {
      $manualSmokeAgeDays = 0
    }
    $manualSmokeAgeDaysDisplay = [Math]::Round($manualSmokeAgeDays, 2)
    if ($manualSmokeAgeDays -gt [double]$ManualSmokeMaxAgeDays) {
      $manualSmokeRecencyGate = $false
      $notes += "manual smoke stale(ageDays=$manualSmokeAgeDaysDisplay, maxDays=$ManualSmokeMaxAgeDays)"
    }

    $latestManualEvidence = Get-ManualSmokeEvidencePortion -EvidenceText "$($latestManualSmoke.Evidence)"
    if ([string]::IsNullOrWhiteSpace($latestManualEvidence)) {
      $evidenceWarnings += "latest manual smoke evidence missing"
    } elseif (-not (Test-EvidenceReference -Value $latestManualEvidence)) {
      $evidenceWarnings += "latest manual smoke evidence has no link/reference"
    }
  }
} else {
  $notes += "manual smoke recency gate skipped"
}

$decision = if ($runGate -and $deployGate -and $verifyGate -and $ageGate -and $preflightGate -and $manualSmokeRecencyGate -and $manualSmokeOutcomeGate) {
  "CONDITIONAL_GO"
} else {
  "HOLD"
}

$manualSmoke = if (-not [string]::IsNullOrWhiteSpace($ManualSmokeResult)) {
  $ManualSmokeResult
} elseif (-not $manualSmokeRecencyGate) {
  "OVERDUE"
} elseif ($decision -eq "CONDITIONAL_GO") {
  "PENDING"
} else {
  "BLOCKED"
}
$evidence = "$($summary.Url)"
if (-not [string]::IsNullOrWhiteSpace($ManualSmokeEvidence)) {
  $evidence = "$($summary.Url) ; $ManualSmokeEvidence"
}
if (-not [string]::IsNullOrWhiteSpace($ManualSmokeNotes)) {
  $notes += "manual smoke note: $ManualSmokeNotes"
}
if ($manualSmokeRecencyGate -and $null -ne $latestManualSmoke) {
  $notes += ("latest manual smoke={0} at {1} ageDays={2}" -f $latestManualSmoke.ManualSmoke, $latestManualSmoke.UtcTime.ToString("yyyy-MM-ddTHH:mm:ssZ"), $manualSmokeAgeDaysDisplay)
}

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
if (-not $manualSmokeRecencyGate) {
  $notes += "manual smoke recency gate failed"
}
if (-not $manualSmokeOutcomeGate) {
  $notes += "manual smoke outcome gate failed"
}

$evidenceWarnings = @($evidenceWarnings | Select-Object -Unique)
foreach ($warning in $evidenceWarnings) {
  $notes += ("evidence warning: " + $warning)
}

$failureCategory = Get-FailureCategoryCode `
  -RunGate $runGate `
  -DeployGate $deployGate `
  -VerifyGate $verifyGate `
  -AgeGate $ageGate `
  -PreflightGate $preflightGate `
  -ManualSmokeRecencyGate $manualSmokeRecencyGate `
  -ManualSmokeOutcomeGate $manualSmokeOutcomeGate `
  -HasEvidenceWarning ($evidenceWarnings.Count -gt 0)

$failureDetail = Get-FailureDetail `
  -FailureCategory $failureCategory `
  -RunConclusion "$($summary.Conclusion)" `
  -DeployConclusion "$($summary.DeployJobConclusion)" `
  -VerifyConclusion "$($summary.VerifyStepConclusion)" `
  -RunAgeMinutes "$($summary.RunAgeMinutes)" `
  -MaxAgeMinutes $MaxAgeMinutes `
  -Notes $notes `
  -EvidenceWarnings $evidenceWarnings

$logRow = @{
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
  Evidence = $evidence
  Owner = $Owner
  Notes = ($notes -join "; ")
}
if (-not (Should-SkipLogAppend -Path $LogFile -Row $logRow -WindowMinutes $LogDedupWindowMinutes)) {
  Append-LogRow -Path $LogFile -Row $logRow
} elseif (-not $AsJson) {
  Write-Host ("Skip duplicate staging log row (window={0}m)." -f $LogDedupWindowMinutes)
}

$resultPayload = [PSCustomObject]@{
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
  ManualSmokeAgeDays = "$manualSmokeAgeDaysDisplay"
  FailureCategory = $failureCategory
  FailureDetail = $failureDetail
  EvidenceStatus = $(if ($evidenceWarnings.Count -gt 0) { "WARN" } else { "OK" })
  EvidenceWarnings = $evidenceWarnings
  Evidence = $evidence
  Owner = $Owner
  Notes = ($notes -join "; ")
  Gates = [PSCustomObject]@{
    Run = $runGate
    Deploy = $deployGate
    Verify = $verifyGate
    Age = $ageGate
    ManualRecency = $manualSmokeRecencyGate
    ManualOutcome = $manualSmokeOutcomeGate
  }
  LatestManualSmoke = if ($null -eq $latestManualSmoke) {
    $null
  } else {
    [PSCustomObject]@{
      UtcTime = $latestManualSmoke.UtcTime.ToString("yyyy-MM-ddTHH:mm:ssZ")
      ManualSmoke = $latestManualSmoke.ManualSmoke
      Evidence = $latestManualSmoke.Evidence
      Owner = $latestManualSmoke.Owner
      Notes = $latestManualSmoke.Notes
    }
  }
}

if ($AsJson) {
  $resultPayload | ConvertTo-Json -Depth 8
} else {
  Write-Host "== Staging Ops Cycle =="
  Write-Host ("Repo: {0}" -f $Repo)
  Write-Host ("Branch: {0}" -f $Branch)
  Write-Host ("RunId: {0}" -f $summary.RunId)
  Write-Host ("HeadSha: {0}" -f $summary.HeadSha)
  Write-Host ("Preflight: {0}" -f $preflightState)
  Write-Host ("Gate(run/deploy/verify/age/manualRecency/manualOutcome): {0}/{1}/{2}/{3}/{4}/{5}" -f $runGate, $deployGate, $verifyGate, $ageGate, $manualSmokeRecencyGate, $manualSmokeOutcomeGate)
  Write-Host ("Decision: {0}" -f $decision)
  Write-Host ("ManualSmoke: {0}" -f $manualSmoke)
  Write-Host ("ManualSmokeAgeDays: {0}" -f $manualSmokeAgeDaysDisplay)
  Write-Host ("FailureCategory: {0}" -f $failureCategory)
  if ($failureDetail -ne "-") {
    Write-Host ("FailureDetail: {0}" -f $failureDetail)
  }
  Write-Host ("Log: {0}" -f $LogFile)
  Write-Host ""
  Write-Host "Update manual smoke results in:"
  Write-Host "- docs/staging-smoke-checklist.md"
  Write-Host "- docs/staging-feedback-checklist.md"
  Write-Host "- or run staging-ops-cycle with -ManualSmokeResult PASS/FAIL"
}

if ($decision -eq "CONDITIONAL_GO") {
  exit 0
}

exit 1
