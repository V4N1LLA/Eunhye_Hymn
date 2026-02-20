param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Ref = "develop",
  [string]$Workflow = "mobile-store-release.yml",
  [ValidateSet("android", "ios", "both")][string]$Target = "android",
  [ValidateSet("build_only", "play_upload")][string]$AndroidDistributionMode = "build_only",
  [string]$AndroidPackageName = "",
  [ValidateSet("internal", "alpha", "beta", "production")][string]$AndroidTrack = "internal",
  [ValidateSet("draft", "completed", "inProgress", "halted")][string]$AndroidReleaseStatus = "draft",
  [bool]$AndroidChangesNotSentForReview = $true,
  [ValidateSet("build_only", "testflight")][string]$IosDistributionMode = "build_only",
  [string]$IosBundleId = "",
  [string]$ReleaseNotes = "",
  [string]$ApiBaseUrl = "https://example.com/api/v1",
  [string]$LogFile = "docs/mobile-store-release-log.md",
  [switch]$SkipPreflight,
  [switch]$DryRun,
  [int]$RunDetectRetries = 36,
  [int]$RunDetectIntervalSeconds = 5,
  [int]$WatchIntervalSeconds = 20
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
# Mobile Store Release Log

Operational log for mobile store release workflow cycles.

| UTC Time | Repo | Ref | Target | AndroidMode | iOSMode | Preflight | RunId | Result | URL | Notes |
|----------|------|-----|--------|-------------|---------|-----------|-------|--------|-----|-------|
"@ | Set-Content -Path $Path -Encoding utf8
}

function Append-LogRow {
  param(
    [string]$Path,
    [hashtable]$Row
  )

  Ensure-LogFile -Path $Path
  $line = "| {0} | {1} | {2} | {3} | {4} | {5} | {6} | {7} | {8} | {9} | {10} |" -f `
    (Escape-MarkdownCell $Row.UtcTime),
    (Escape-MarkdownCell $Row.Repo),
    (Escape-MarkdownCell $Row.Ref),
    (Escape-MarkdownCell $Row.Target),
    (Escape-MarkdownCell $Row.AndroidMode),
    (Escape-MarkdownCell $Row.IosMode),
    (Escape-MarkdownCell $Row.Preflight),
    (Escape-MarkdownCell $Row.RunId),
    (Escape-MarkdownCell $Row.Result),
    (Escape-MarkdownCell $Row.Url),
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

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

if (-not (Test-CommandExists "gh")) {
  throw "gh CLI is required."
}

$preflightState = "SKIPPED"

if (-not $SkipPreflight) {
  $preflightScript = Join-Path $PSScriptRoot "mobile-store-preflight.ps1"
  if (-not (Test-Path $preflightScript)) {
    throw "mobile-store-preflight.ps1 not found: $preflightScript"
  }

  $preflightArgs = @(
    "-Repo", $Repo,
    "-Target", $Target,
    "-AndroidDistributionMode", $AndroidDistributionMode,
    "-IosDistributionMode", $IosDistributionMode
  )
  if (-not [string]::IsNullOrWhiteSpace($AndroidPackageName)) {
    $preflightArgs += @("-AndroidPackageName", $AndroidPackageName)
  }
  if (-not [string]::IsNullOrWhiteSpace($IosBundleId)) {
    $preflightArgs += @("-IosBundleId", $IosBundleId)
  }

  $preflightResult = Invoke-PowerShellFile -ScriptPath $preflightScript -Arguments $preflightArgs
  if ($preflightResult.ExitCode -eq 0) {
    $preflightState = "PASS"
  } else {
    $preflightState = "FAIL"
    Append-LogRow -Path $LogFile -Row @{
      UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
      Repo = $Repo
      Ref = $Ref
      Target = $Target
      AndroidMode = $AndroidDistributionMode
      IosMode = $IosDistributionMode
      Preflight = $preflightState
      RunId = "-"
      Result = "ABORTED"
      Url = "-"
      Notes = "preflight failed"
    }
    throw "mobile store preflight failed; cycle aborted."
  }
}

$androidChanges = if ($AndroidChangesNotSentForReview) { "true" } else { "false" }

if ($DryRun) {
  Write-Host "[DryRun] Repo: $Repo"
  Write-Host "[DryRun] Ref: $Ref"
  Write-Host "[DryRun] Workflow: $Workflow"
  Write-Host "[DryRun] Target: $Target"
  Write-Host "[DryRun] Android mode: $AndroidDistributionMode"
  Write-Host "[DryRun] iOS mode: $IosDistributionMode"
  Write-Host "[DryRun] Preflight: $preflightState"
  Write-Host "[DryRun] LogFile: $LogFile"
  exit 0
}

$triggeredAfter = (Get-Date).ToUniversalTime()
$baselineRuns = gh run list --repo $Repo --workflow $Workflow --branch $Ref --event workflow_dispatch --limit 30 --json databaseId 2>$null
$knownRunIds = @{}
if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($baselineRuns)) {
  try {
    $known = $baselineRuns | ConvertFrom-Json
    foreach ($run in $known) {
      $id = "$($run.databaseId)"
      if (-not [string]::IsNullOrWhiteSpace($id)) {
        $knownRunIds[$id] = $true
      }
    }
  } catch {
    # Best-effort baseline only.
  }
}

$workflowArgs = @(
  "workflow", "run", $Workflow,
  "--repo", $Repo,
  "--ref", $Ref,
  "-f", "target=$Target",
  "-f", "android_distribution_mode=$AndroidDistributionMode",
  "-f", "android_track=$AndroidTrack",
  "-f", "android_release_status=$AndroidReleaseStatus",
  "-f", "android_changes_not_sent_for_review=$androidChanges",
  "-f", "ios_distribution_mode=$IosDistributionMode",
  "-f", "release_notes=$ReleaseNotes",
  "-f", "api_base_url=$ApiBaseUrl"
)

if (-not [string]::IsNullOrWhiteSpace($AndroidPackageName)) {
  $workflowArgs += @("-f", "android_package_name=$AndroidPackageName")
}
if (-not [string]::IsNullOrWhiteSpace($IosBundleId)) {
  $workflowArgs += @("-f", "ios_bundle_id=$IosBundleId")
}

& gh @workflowArgs
if ($LASTEXITCODE -ne 0) {
  Append-LogRow -Path $LogFile -Row @{
    UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Repo = $Repo
    Ref = $Ref
    Target = $Target
    AndroidMode = $AndroidDistributionMode
    IosMode = $IosDistributionMode
    Preflight = $preflightState
    RunId = "-"
    Result = "FAILED"
    Url = "-"
    Notes = "workflow dispatch failed"
  }
  throw "failed to dispatch workflow."
}

$runId = ""
$runUrl = ""
for ($i = 0; $i -lt $RunDetectRetries; $i++) {
  $runJson = gh run list --repo $Repo --workflow $Workflow --branch $Ref --event workflow_dispatch --limit 10 --json databaseId,createdAt,url,status
  if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($runJson)) {
    $runs = $runJson | ConvertFrom-Json
    $candidate = $runs |
      Where-Object { -not $knownRunIds.ContainsKey("$($_.databaseId)") } |
      Sort-Object { [DateTime]$_.createdAt } -Descending |
      Select-Object -First 1

    if ($null -ne $candidate) {
      $runId = "$($candidate.databaseId)"
      $runUrl = "$($candidate.url)"
      break
    }

    # Fallback: if no baseline match was captured, use latest run after dispatch time window.
    if ($knownRunIds.Count -eq 0) {
      $fallback = $runs |
        Where-Object { ([DateTime]$_.createdAt).ToUniversalTime() -ge $triggeredAfter.AddSeconds(-30) } |
        Sort-Object { [DateTime]$_.createdAt } -Descending |
        Select-Object -First 1
      if ($null -ne $fallback) {
        $runId = "$($fallback.databaseId)"
        $runUrl = "$($fallback.url)"
        break
      }
    }
  }

  Start-Sleep -Seconds $RunDetectIntervalSeconds
}

if ([string]::IsNullOrWhiteSpace($runId)) {
  Append-LogRow -Path $LogFile -Row @{
    UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Repo = $Repo
    Ref = $Ref
    Target = $Target
    AndroidMode = $AndroidDistributionMode
    IosMode = $IosDistributionMode
    Preflight = $preflightState
    RunId = "-"
    Result = "FAILED"
    Url = "-"
    Notes = "could not detect workflow run id"
  }
  throw "failed to detect workflow run id."
}

& gh run watch $runId --repo $Repo --interval $WatchIntervalSeconds --exit-status
$watchExitCode = $LASTEXITCODE

$runView = gh run view $runId --repo $Repo --json conclusion,status,url,headSha
if ($LASTEXITCODE -ne 0) {
  $result = if ($watchExitCode -eq 0) { "SUCCESS" } else { "FAILED" }
  Append-LogRow -Path $LogFile -Row @{
    UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    Repo = $Repo
    Ref = $Ref
    Target = $Target
    AndroidMode = $AndroidDistributionMode
    IosMode = $IosDistributionMode
    Preflight = $preflightState
    RunId = $runId
    Result = $result
    Url = $runUrl
    Notes = "run watch completed, run view failed"
  }
  if ($watchExitCode -ne 0) {
    throw "workflow run failed (run id: $runId)"
  }
  exit 0
}

$runInfo = $runView | ConvertFrom-Json
$conclusion = if ([string]::IsNullOrWhiteSpace($runInfo.conclusion)) { "unknown" } else { $runInfo.conclusion }
$result = if ($watchExitCode -eq 0 -and $conclusion -eq "success") { "SUCCESS" } else { "FAILED" }
$notes = "conclusion=$conclusion, head_sha=$($runInfo.headSha)"

Append-LogRow -Path $LogFile -Row @{
  UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  Repo = $Repo
  Ref = $Ref
  Target = $Target
  AndroidMode = $AndroidDistributionMode
  IosMode = $IosDistributionMode
  Preflight = $preflightState
  RunId = $runId
  Result = $result
  Url = $runInfo.url
  Notes = $notes
}

Write-Host "== Mobile Store Cycle =="
Write-Host ("Repo: {0}" -f $Repo)
Write-Host ("Ref: {0}" -f $Ref)
Write-Host ("Target: {0}" -f $Target)
Write-Host ("RunId: {0}" -f $runId)
Write-Host ("Preflight: {0}" -f $preflightState)
Write-Host ("Conclusion: {0}" -f $conclusion)
Write-Host ("Log: {0}" -f $LogFile)

if ($result -ne "SUCCESS") {
  exit 1
}

exit 0
