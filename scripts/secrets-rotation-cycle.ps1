param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [int]$MaxAgeDays = 90,
  [switch]$IncludeMobileReleaseSecrets,
  [string]$Owner = "codex",
  [string]$LogFile = "docs/secrets-rotation-log.md"
)

$ErrorActionPreference = "Stop"

if ($MaxAgeDays -lt 1) {
  throw "MaxAgeDays must be >= 1"
}

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
# Secrets Rotation Audit Log

Periodic secret-rotation audit history (staging core + optional mobile release scope).

| UTC Time | Repo | Scope | MaxAgeDays | OK | STALE | MISSING | UNKNOWN | Decision | Owner | Notes |
|----------|------|-------|------------|----|-------|---------|---------|----------|-------|-------|
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
    (Escape-MarkdownCell $Row.Scope),
    (Escape-MarkdownCell $Row.MaxAgeDays),
    (Escape-MarkdownCell $Row.OkCount),
    (Escape-MarkdownCell $Row.StaleCount),
    (Escape-MarkdownCell $Row.MissingCount),
    (Escape-MarkdownCell $Row.UnknownCount),
    (Escape-MarkdownCell $Row.Decision),
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

$auditScript = Join-Path $PSScriptRoot "staging-secret-rotation-audit.ps1"
if (-not (Test-Path $auditScript)) {
  throw "staging-secret-rotation-audit.ps1 not found: $auditScript"
}

$auditArgs = @(
  "-Repo", $Repo,
  "-MaxAgeDays", "$MaxAgeDays",
  "-AsJson"
)
if ($IncludeMobileReleaseSecrets) {
  $auditArgs += "-IncludeMobileReleaseSecrets"
}

$auditResult = Invoke-PowerShellFile -ScriptPath $auditScript -Arguments $auditArgs

try {
  $rows = $auditResult.Output | ConvertFrom-Json
} catch {
  throw "Failed to parse audit output as JSON."
}

if ($null -eq $rows) {
  throw "Audit returned no rows."
}
if ($rows -isnot [System.Array]) {
  $rows = @($rows)
}

$okRows = @($rows | Where-Object { $_.Status -eq "OK" })
$staleRows = @($rows | Where-Object { $_.Status -eq "STALE" })
$missingRows = @($rows | Where-Object { $_.Status -eq "MISSING" })
$unknownRows = @($rows | Where-Object { $_.Status -eq "UNKNOWN" })

$decision = if ($staleRows.Count -eq 0 -and $missingRows.Count -eq 0 -and $unknownRows.Count -eq 0) {
  "PASS"
} else {
  "HOLD"
}

$scope = if ($IncludeMobileReleaseSecrets) { "STAGING+MOBILE_RELEASE" } else { "STAGING" }
$notes = @()
if ($staleRows.Count -gt 0) {
  $notes += ("stale: " + (($staleRows | Select-Object -ExpandProperty Secret) -join ", "))
}
if ($missingRows.Count -gt 0) {
  $notes += ("missing: " + (($missingRows | Select-Object -ExpandProperty Secret) -join ", "))
}
if ($unknownRows.Count -gt 0) {
  $notes += ("unknown: " + (($unknownRows | Select-Object -ExpandProperty Secret) -join ", "))
}

Append-LogRow -Path $LogFile -Row @{
  UtcTime = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  Repo = $Repo
  Scope = $scope
  MaxAgeDays = "$MaxAgeDays"
  OkCount = "$($okRows.Count)"
  StaleCount = "$($staleRows.Count)"
  MissingCount = "$($missingRows.Count)"
  UnknownCount = "$($unknownRows.Count)"
  Decision = $decision
  Owner = $Owner
  Notes = ($notes -join "; ")
}

Write-Host "== Secrets Rotation Cycle =="
Write-Host ("Repo: {0}" -f $Repo)
Write-Host ("Scope: {0}" -f $scope)
Write-Host ("MaxAgeDays: {0}" -f $MaxAgeDays)
Write-Host ("Summary: OK={0}, STALE={1}, MISSING={2}, UNKNOWN={3}" -f $okRows.Count, $staleRows.Count, $missingRows.Count, $unknownRows.Count)
Write-Host ("Decision: {0}" -f $decision)
Write-Host ("Log: {0}" -f $LogFile)

if ($decision -eq "PASS") {
  exit 0
}

exit 1
