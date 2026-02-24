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

$utcNow = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")

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
$existingJson = gh issue list --repo $Repo --state open --search $search --json number,title,url --limit 1
if ($LASTEXITCODE -ne 0) {
  throw "failed to query existing issues."
}

$existing = @()
if (-not [string]::IsNullOrWhiteSpace($existingJson)) {
  $existing = $existingJson | ConvertFrom-Json
}

if ($DryRun) {
  if ($existing.Count -gt 0) {
    $result.Action = "dryrun_comment"
    $result.IssueNumber = "$($existing[0].number)"
    $result.IssueUrl = "$($existing[0].url)"
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
  $issueNumber = "$($existing[0].number)"
  gh issue comment $issueNumber --repo $Repo --body $body 1>$null
  if ($LASTEXITCODE -ne 0) {
    throw "failed to append issue comment."
  }

  $result.Action = "commented"
  $result.IssueNumber = $issueNumber
  $result.IssueUrl = "$($existing[0].url)"
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
