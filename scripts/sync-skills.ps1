param(
  [string]$SourceDir = "skills",
  [string]$DestDir = "",
  [string[]]$Skill = @(),
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Resolve-RepoPath {
  param(
    [string]$RepoRoot,
    [string]$PathValue
  )

  if ([string]::IsNullOrWhiteSpace($PathValue)) {
    return $RepoRoot
  }
  if ([System.IO.Path]::IsPathRooted($PathValue)) {
    return [System.IO.Path]::GetFullPath($PathValue)
  }
  return [System.IO.Path]::GetFullPath((Join-Path $RepoRoot $PathValue))
}

function Normalize-DirectoryPath {
  param([string]$PathValue)

  if ([string]::IsNullOrWhiteSpace($PathValue)) {
    return ""
  }

  $fullPath = [System.IO.Path]::GetFullPath($PathValue)
  return $fullPath.TrimEnd('\', '/')
}

function Test-PathOverlap {
  param(
    [string]$PathA,
    [string]$PathB
  )

  $normalizedA = Normalize-DirectoryPath -PathValue $PathA
  $normalizedB = Normalize-DirectoryPath -PathValue $PathB
  if ([string]::IsNullOrWhiteSpace($normalizedA) -or [string]::IsNullOrWhiteSpace($normalizedB)) {
    return $false
  }

  if ($normalizedA -ieq $normalizedB) {
    return $true
  }

  $prefixA = $normalizedA + [System.IO.Path]::DirectorySeparatorChar
  $prefixB = $normalizedB + [System.IO.Path]::DirectorySeparatorChar
  return $normalizedA.StartsWith($prefixB, [System.StringComparison]::OrdinalIgnoreCase) -or
    $normalizedB.StartsWith($prefixA, [System.StringComparison]::OrdinalIgnoreCase)
}

function Get-CodexHome {
  if (-not [string]::IsNullOrWhiteSpace($env:CODEX_HOME)) {
    return $env:CODEX_HOME
  }
  return (Join-Path ([Environment]::GetFolderPath("UserProfile")) ".codex")
}

function Test-SkillFolder {
  param([string]$Path)
  return (Test-Path (Join-Path $Path "SKILL.md"))
}

function Get-FrontmatterName {
  param([string]$SkillMdPath)

  $lines = Get-Content -Encoding UTF8 $SkillMdPath
  if ($lines.Count -lt 3 -or $lines[0].Trim() -ne "---") {
    return $null
  }

  for ($i = 1; $i -lt $lines.Count; $i++) {
    $line = $lines[$i]
    if ($line.Trim() -eq "---") {
      break
    }
    if ($line -match '^\s*name\s*:\s*(.+?)\s*$') {
      return $Matches[1].Trim()
    }
  }
  return $null
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$sourceRoot = Resolve-RepoPath -RepoRoot $repoRoot -PathValue $SourceDir

if (-not (Test-Path -LiteralPath $sourceRoot)) {
  throw "SourceDir not found: $sourceRoot"
}

$destRoot = if ([string]::IsNullOrWhiteSpace($DestDir)) {
  Join-Path (Get-CodexHome) "skills"
} else {
  Resolve-RepoPath -RepoRoot $repoRoot -PathValue $DestDir
}

if (Test-PathOverlap -PathA $sourceRoot -PathB $destRoot) {
  throw ("SourceDir and DestDir must not be identical or nested. source={0}, dest={1}" -f $sourceRoot, $destRoot)
}

$allSkillDirs = Get-ChildItem -LiteralPath $sourceRoot -Directory |
  Where-Object { $_.Name -ne ".system" -and (Test-SkillFolder -Path $_.FullName) } |
  Sort-Object Name

if (-not $allSkillDirs -or $allSkillDirs.Count -eq 0) {
  throw "No skill folders with SKILL.md found under: $sourceRoot"
}

$selectedSkillDirs = $allSkillDirs

if ($Skill.Count -gt 0) {
  $wanted = @{}
  foreach ($skillName in $Skill) {
    if (-not [string]::IsNullOrWhiteSpace($skillName)) {
      $wanted[$skillName.Trim()] = $true
    }
  }
  if ($wanted.Count -eq 0) {
    throw "Skill parameter is empty after trimming."
  }

  $selectedSkillDirs = $allSkillDirs | Where-Object { $wanted.ContainsKey($_.Name) }

  $missing = @()
  foreach ($name in $wanted.Keys) {
    if (-not ($selectedSkillDirs | Where-Object { $_.Name -eq $name })) {
      $missing += $name
    }
  }
  if ($missing.Count -gt 0) {
    $available = ($allSkillDirs | Select-Object -ExpandProperty Name) -join ", "
    throw ("Unknown skills: {0}. Available: {1}" -f ($missing -join ", "), $available)
  }
}

if (-not (Test-Path -LiteralPath $destRoot)) {
  if (-not $DryRun) {
    New-Item -ItemType Directory -Path $destRoot | Out-Null
  }
}

Write-Host ("Source: {0}" -f $sourceRoot)
Write-Host ("Destination: {0}" -f $destRoot)
Write-Host ("Skills: {0}" -f (($selectedSkillDirs | Select-Object -ExpandProperty Name) -join ", "))
Write-Host ""

$synced = 0
$warnings = @()

foreach ($skillDir in $selectedSkillDirs) {
  $skillName = $skillDir.Name
  $sourcePath = $skillDir.FullName
  $targetPath = Join-Path $destRoot $skillName
  $skillMd = Join-Path $sourcePath "SKILL.md"
  $frontmatterName = Get-FrontmatterName -SkillMdPath $skillMd

  if (-not [string]::IsNullOrWhiteSpace($frontmatterName) -and $frontmatterName -ne $skillName) {
    $warnings += ("name mismatch: folder '{0}' vs frontmatter '{1}'" -f $skillName, $frontmatterName)
  }

  if ($DryRun) {
    Write-Host ("[DRYRUN] sync {0} -> {1}" -f $sourcePath, $targetPath)
    continue
  }

  if (Test-Path -LiteralPath $targetPath) {
    Remove-Item -LiteralPath $targetPath -Recurse -Force
  }
  Copy-Item -LiteralPath $sourcePath -Destination $targetPath -Recurse -Force
  Write-Host ("[OK] synced {0}" -f $skillName)
  $synced++
}

Write-Host ""
if ($DryRun) {
  Write-Host "Dry-run complete."
} else {
  Write-Host ("Sync complete. {0} skill(s) updated." -f $synced)
}

if ($warnings.Count -gt 0) {
  Write-Host ""
  foreach ($w in $warnings) {
    Write-Host ("[WARN] {0}" -f $w)
  }
}

Write-Host "Restart Codex to pick up new skills."
