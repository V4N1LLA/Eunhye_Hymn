param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Branch = "develop",
  [ValidateSet("PASS", "HOLD", "ERROR")]
  [string]$Overall = "PASS",
  [string]$FailureCategory = "-",
  [string]$Evidence = "-",
  [string]$Notes = "",
  [string]$Operator = "codex",
  [string]$SourceLog = "docs/ops-health-log.md",
  [int]$DedupWindowMinutes = 30,
  [int]$HoldReAlertWindowMinutes = 480,
  [int]$ErrorReAlertWindowMinutes = 120,
  [switch]$ForceAlert,
  [switch]$DryRun,
  [switch]$AsJson
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Normalize-Token {
  param([string]$Value, [string]$Fallback = "unknown")

  if ([string]::IsNullOrWhiteSpace($Value) -or $Value -eq "-") {
    return $Fallback
  }

  $normalized = $Value.Trim().ToLowerInvariant()
  $normalized = $normalized -replace "[^a-z0-9,_-]", "-"
  $normalized = $normalized -replace "-+", "-"
  $normalized = $normalized.Trim("-")
  if ([string]::IsNullOrWhiteSpace($normalized)) {
    return $Fallback
  }
  return $normalized
}

function Get-ReAlertWindowMinutes {
  param(
    [string]$Overall,
    [int]$HoldWindowMinutes,
    [int]$ErrorWindowMinutes
  )

  if ($Overall -eq "ERROR") {
    return $ErrorWindowMinutes
  }

  return $HoldWindowMinutes
}

function Get-IssueAlertState {
  param(
    [string]$Repo,
    [string]$IssueNumber
  )

  $issueViewJson = gh issue view $IssueNumber --repo $Repo --json number,url,createdAt,comments
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($issueViewJson)) {
    throw "failed to query issue activity."
  }

  $issueView = $issueViewJson | ConvertFrom-Json

  [datetime]$lastAlertUtc = [datetime]::MinValue
  [datetime]$createdUtc = [datetime]::MinValue
  if ([datetime]::TryParse("$($issueView.createdAt)", [ref]$createdUtc)) {
    $lastAlertUtc = $createdUtc.ToUniversalTime()
  }

  foreach ($comment in @($issueView.comments)) {
    [datetime]$commentUtc = [datetime]::MinValue
    if ([datetime]::TryParse("$($comment.createdAt)", [ref]$commentUtc)) {
      $candidateUtc = $commentUtc.ToUniversalTime()
      if ($candidateUtc -gt $lastAlertUtc) {
        $lastAlertUtc = $candidateUtc
      }
    }
  }

  return [PSCustomObject]@{
    IssueNumber = "$($issueView.number)"
    IssueUrl = "$($issueView.url)"
    HasLastAlert = ($lastAlertUtc -ne [datetime]::MinValue)
    LastAlertUtc = $lastAlertUtc
  }
}

$utcNow = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

if ($DedupWindowMinutes -lt 0) {
  throw "DedupWindowMinutes must be >= 0"
}
if ($HoldReAlertWindowMinutes -lt 0) {
  throw "HoldReAlertWindowMinutes must be >= 0"
}
if ($ErrorReAlertWindowMinutes -lt 0) {
  throw "ErrorReAlertWindowMinutes must be >= 0"
}

$result = [PSCustomObject]@{
  UtcTime = $utcNow
  Repo = $Repo
  Branch = $Branch
  Overall = $Overall
  FailureCategory = $FailureCategory
  Evidence = $Evidence
  Notes = $Notes
  Action = "skipped"
  IssueNumber = ""
  IssueUrl = ""
  Title = ""
  SkipReason = ""
  LastAlertUtc = ""
  MinutesSinceLastAlert = ""
  DedupWindowMinutes = $DedupWindowMinutes
  ReAlertWindowMinutes = ""
  ForceAlert = [bool]$ForceAlert
}

if ($Overall -eq "PASS") {
  if ($AsJson) {
    $result | ConvertTo-Json -Depth 6
  } else {
    Write-Host "ops-health alert skipped: overall=PASS"
  }
  exit 0
}

if (-not (Test-CommandExists "gh")) {
  throw "gh CLI is required."
}

gh auth status 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
  throw "gh auth required. Run: gh auth login"
}

$categoryToken = Normalize-Token -Value $FailureCategory -Fallback "unknown"
$branchToken = Normalize-Token -Value $Branch -Fallback "branch"
$title = "[ops-health][$branchToken][$categoryToken][$Overall]-action-required"
$result.Title = $title
$reAlertWindowMinutes = Get-ReAlertWindowMinutes -Overall $Overall -HoldWindowMinutes $HoldReAlertWindowMinutes -ErrorWindowMinutes $ErrorReAlertWindowMinutes
$result.ReAlertWindowMinutes = $reAlertWindowMinutes

$body = @"
### Ops health alert

- UTC: $utcNow
- Repo: $Repo
- Branch: $Branch
- Overall: $Overall
- FailureCategory: $FailureCategory
- Evidence: $Evidence
- SourceLog: $SourceLog
- Operator: $Operator

#### Notes
$Notes
"@

$search = "$title in:title"
$existingJson = gh issue list --repo $Repo --state open --search $search --json number,title,url --limit 50
if ($LASTEXITCODE -ne 0) {
  throw "failed to query existing issues."
}

$existingCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($existingJson)) {
  $existingCandidates = $existingJson | ConvertFrom-Json
}
$existing = @($existingCandidates | Where-Object { "$($_.title)" -eq $title } | Select-Object -First 1)

$skipAlert = $false
$skipReason = ""
if ($existing.Count -gt 0) {
  $issueState = Get-IssueAlertState -Repo $Repo -IssueNumber "$($existing[0].number)"
  $result.IssueNumber = "$($issueState.IssueNumber)"
  $result.IssueUrl = "$($issueState.IssueUrl)"

  if ($issueState.HasLastAlert) {
    $lastAlertUtc = $issueState.LastAlertUtc.ToString("yyyy-MM-ddTHH:mm:ssZ")
    $minutesSinceLast = [math]::Round(((Get-Date).ToUniversalTime() - $issueState.LastAlertUtc).TotalMinutes, 1)
    $result.LastAlertUtc = $lastAlertUtc
    $result.MinutesSinceLastAlert = "$minutesSinceLast"

    if (-not $ForceAlert) {
      if ($DedupWindowMinutes -gt 0 -and $minutesSinceLast -lt $DedupWindowMinutes) {
        $skipAlert = $true
        $skipReason = "dedup_window"
      } elseif ($reAlertWindowMinutes -gt 0 -and $minutesSinceLast -lt $reAlertWindowMinutes) {
        $skipAlert = $true
        $skipReason = "realert_cooldown"
      }
    }
  }
}

if ($DryRun) {
  if ($existing.Count -gt 0) {
    if ($skipAlert) {
      $result.Action = "dryrun_skipped"
      $result.SkipReason = $skipReason
    } else {
      $result.Action = "dryrun_comment"
    }
  } else {
    $result.Action = "dryrun_create"
  }

  if ($AsJson) {
    $result | ConvertTo-Json -Depth 6
  } else {
    Write-Host ($result | ConvertTo-Json -Depth 6)
  }
  exit 0
}

if ($existing.Count -gt 0) {
  if ($skipAlert) {
    $result.Action = "skipped"
    $result.SkipReason = $skipReason

    if ($AsJson) {
      $result | ConvertTo-Json -Depth 6
    } else {
      Write-Host ("ops-health alert skipped ({0}): {1}" -f $skipReason, $result.IssueUrl)
    }
    exit 0
  }

  $issueNumber = "$($result.IssueNumber)"
  gh issue comment $issueNumber --repo $Repo --body $body 1>$null
  if ($LASTEXITCODE -ne 0) {
    throw "failed to append issue comment."
  }

  $result.Action = "commented"
  $result.IssueNumber = $issueNumber
} else {
  $createdUrl = gh issue create --repo $Repo --title $title --body $body
  if ($LASTEXITCODE -ne 0) {
    throw "failed to create issue."
  }

  $result.Action = "created"
  $result.IssueUrl = "$createdUrl".Trim()
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 6
} else {
  Write-Host ("ops-health alert {0}: {1}" -f $result.Action, $result.IssueUrl)
}

exit 0
