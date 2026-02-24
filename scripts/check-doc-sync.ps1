param(
  [string[]]$ChangedFiles = @(),
  [string]$BaseRef = "",
  [string]$HeadRef = "HEAD",
  [switch]$AsJson
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$requiredDocs = @(
  "docs/changelog-dev.md",
  "CLAUDE.md"
)

function Normalize-Path {
  param([string]$PathValue)
  return ($PathValue -replace "\\", "/").Trim()
}

function Get-ChangedFilesFromGit {
  param(
    [string]$DiffBaseRef,
    [string]$DiffHeadRef
  )

  if ([string]::IsNullOrWhiteSpace($DiffBaseRef)) {
    throw "BaseRef is required when ChangedFiles is not provided."
  }

  $range = "$DiffBaseRef...$DiffHeadRef"
  $raw = & git diff --name-only $range
  if ($LASTEXITCODE -ne 0) {
    throw "Failed to read changed files from git diff range: $range"
  }
  return @($raw | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Is-DocOnlyPath {
  param([string]$PathValue)

  if ($PathValue -match "^docs/") { return $true }
  if ($PathValue -in @("README.md", "CLAUDE.md", "AGENTS.md")) { return $true }
  if ($PathValue -match "^2026-[0-9]{2}-[0-9]{2}\.md$") { return $true }
  return $false
}

$files = @($ChangedFiles | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
if ($files.Count -eq 0) {
  $files = Get-ChangedFilesFromGit -DiffBaseRef $BaseRef -DiffHeadRef $HeadRef
}

$normalizedFiles = @($files | ForEach-Object { Normalize-Path $_ } | Where-Object { $_ -ne "" } | Select-Object -Unique)
$nonDocFiles = @($normalizedFiles | Where-Object { -not (Is-DocOnlyPath $_) })
$requiresDocSync = $nonDocFiles.Count -gt 0

$missingRequiredDocs = @()
if ($requiresDocSync) {
  foreach ($required in $requiredDocs) {
    if (-not ($normalizedFiles -contains $required)) {
      $missingRequiredDocs += $required
    }
  }
}

$result = [ordered]@{
  passed = ($missingRequiredDocs.Count -eq 0)
  requiresDocSync = $requiresDocSync
  changedFiles = $normalizedFiles
  nonDocFiles = $nonDocFiles
  requiredDocs = $requiredDocs
  missingRequiredDocs = $missingRequiredDocs
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 6
}

if ($missingRequiredDocs.Count -gt 0) {
  Write-Host "Doc sync check FAILED."
  Write-Host "Changed non-doc files:"
  foreach ($item in $nonDocFiles) {
    Write-Host ("- " + $item)
  }
  Write-Host "Missing required docs:"
  foreach ($item in $missingRequiredDocs) {
    Write-Host ("- " + $item)
  }
  exit 1
}

Write-Host "Doc sync check PASSED."
Write-Host ("requiresDocSync=" + $requiresDocSync)
