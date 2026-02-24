param(
  [Parameter(Mandatory = $true)]
  [string]$TaskName,
  [string]$BaseBranch = "develop",
  [string]$BranchPrefix = "feat",
  [string]$Remote = "origin",
  [string]$WorktreesDir = "..\\worktrees",
  [string]$Owner = "codex",
  [string[]]$ClaimedPaths = @(),
  [string]$TaskBoardPath = "docs/parallel-task-board.md",
  [switch]$SkipTaskBoardUpdate,
  [switch]$AllowClaimedPathConflict
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function New-Slug {
  param([string]$Value)
  $slug = $Value.ToLowerInvariant()
  $slug = $slug -replace "[^a-z0-9]+", "-"
  $slug = $slug.Trim("-")
  if ([string]::IsNullOrWhiteSpace($slug)) {
    throw "TaskName produced an empty slug. Use letters/numbers in TaskName."
  }
  return $slug
}

function Escape-MarkdownCell {
  param([string]$Value)
  if ($null -eq $Value) {
    return ""
  }
  return $Value.Replace("|", "\|").Trim()
}

function Normalize-PathToken {
  param([string]$Value)

  if ([string]::IsNullOrWhiteSpace($Value)) {
    return ""
  }

  $normalized = $Value.Trim()
  $normalized = $normalized -replace "\\", "/"
  $normalized = $normalized.Trim("/")
  return $normalized.ToLowerInvariant()
}

function Test-PathOverlap {
  param(
    [string]$Left,
    [string]$Right
  )

  if ([string]::IsNullOrWhiteSpace($Left) -or [string]::IsNullOrWhiteSpace($Right)) {
    return $false
  }
  if ($Left -eq $Right) {
    return $true
  }
  if ($Left.StartsWith($Right + "/")) {
    return $true
  }
  if ($Right.StartsWith($Left + "/")) {
    return $true
  }
  return $false
}

function Ensure-TaskBoardFile {
  param([string]$Path)

  $directory = Split-Path -Path $Path -Parent
  if (-not [string]::IsNullOrWhiteSpace($directory) -and -not (Test-Path $directory)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
  }

  if (Test-Path $Path) {
    return
  }

  @"
# Parallel Task Board

Record active parallel work ownership.

| Owner | Task | Branch | Worktree Path | Claimed Paths | Status | Updated (UTC) |
|---|---|---|---|---|---|---|
"@ | Set-Content -Path $Path -Encoding utf8
}

function Get-TaskBoardRows {
  param([string]$Path)

  Ensure-TaskBoardFile -Path $Path

  $rows = @()
  $lines = Get-Content -Path $Path -Encoding utf8
  foreach ($line in $lines) {
    $trimmed = $line.Trim()
    if (-not $trimmed.StartsWith("|")) {
      continue
    }
    if ($trimmed -match "^\|\s*-+\s*\|") {
      continue
    }
    if ($trimmed -match "^\|\s*Owner\s*\|") {
      continue
    }

    $parts = $line.Split("|")
    if ($parts.Count -lt 9) {
      continue
    }

    $rows += [PSCustomObject]@{
      Owner = $parts[1].Trim()
      Task = $parts[2].Trim()
      Branch = $parts[3].Trim()
      WorktreePath = $parts[4].Trim()
      ClaimedPaths = $parts[5].Trim()
      Status = $parts[6].Trim()
      UpdatedUtc = $parts[7].Trim()
    }
  }

  return $rows
}

function Get-ClaimedPathTokens {
  param([string]$Value)

  if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Trim() -eq "-") {
    return @()
  }

  $tokens = @()
  foreach ($piece in ($Value -split "[,;]")) {
    $normalized = Normalize-PathToken -Value $piece
    if (-not [string]::IsNullOrWhiteSpace($normalized)) {
      $tokens += $normalized
    }
  }
  return @($tokens | Select-Object -Unique)
}

function Test-ActiveStatus {
  param([string]$Status)

  $token = if ([string]::IsNullOrWhiteSpace($Status)) { "" } else { $Status.Trim().ToLowerInvariant() }
  if ($token -in @("done", "merged", "closed", "cancelled")) {
    return $false
  }
  return $true
}

function Assert-NoOwnershipCollision {
  param(
    [object[]]$TaskBoardRows,
    [string]$Owner,
    [string]$BranchName,
    [string]$WorktreePath,
    [string[]]$ClaimedPathTokens,
    [switch]$AllowClaimedPathConflict
  )

  $normalizedWorktree = Normalize-PathToken -Value $WorktreePath
  foreach ($row in $TaskBoardRows) {
    if (-not (Test-ActiveStatus -Status "$($row.Status)")) {
      continue
    }

    $rowOwner = "$($row.Owner)"
    $rowBranch = "$($row.Branch)"
    $rowWorktree = Normalize-PathToken -Value "$($row.WorktreePath)"

    if ($rowBranch -eq $BranchName -and $rowOwner -ne $Owner) {
      throw "Branch collision: $BranchName is already owned by '$rowOwner' in task board."
    }
    if (-not [string]::IsNullOrWhiteSpace($rowWorktree) -and $rowWorktree -eq $normalizedWorktree -and $rowOwner -ne $Owner) {
      throw "Worktree collision: $WorktreePath is already owned by '$rowOwner' in task board."
    }

    $rowClaims = Get-ClaimedPathTokens -Value "$($row.ClaimedPaths)"
    foreach ($claim in $ClaimedPathTokens) {
      foreach ($rowClaim in $rowClaims) {
        if (Test-PathOverlap -Left $claim -Right $rowClaim) {
          if ($rowOwner -ne $Owner) {
            if ($AllowClaimedPathConflict) {
              Write-Warning ("Claimed path overlap allowed: '{0}' overlaps '{1}' owned by '{2}'." -f $claim, $rowClaim, $rowOwner)
            } else {
              throw ("Claimed path collision: '{0}' overlaps '{1}' owned by '{2}'. Use different scope or pass -AllowClaimedPathConflict." -f $claim, $rowClaim, $rowOwner)
            }
          }
        }
      }
    }
  }
}

function Update-TaskBoardRow {
  param(
    [string]$Path,
    [string]$Owner,
    [string]$TaskName,
    [string]$BranchName,
    [string]$WorktreePath,
    [string[]]$ClaimedPathTokens
  )

  Ensure-TaskBoardFile -Path $Path

  $updatedUtc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  $claimedText = if ($ClaimedPathTokens.Count -eq 0) { "-" } else { ($ClaimedPathTokens -join ", ") }
  $newRow = "| {0} | {1} | {2} | {3} | {4} | in_progress | {5} |" -f `
    (Escape-MarkdownCell $Owner),
    (Escape-MarkdownCell $TaskName),
    (Escape-MarkdownCell $BranchName),
    (Escape-MarkdownCell $WorktreePath),
    (Escape-MarkdownCell $claimedText),
    (Escape-MarkdownCell $updatedUtc)

  $lines = Get-Content -Path $Path -Encoding utf8
  $replaced = $false
  for ($index = 0; $index -lt $lines.Count; $index++) {
    $line = $lines[$index]
    $trimmed = $line.Trim()
    if (-not $trimmed.StartsWith("|")) {
      continue
    }
    if ($trimmed -match "^\|\s*-+\s*\|" -or $trimmed -match "^\|\s*Owner\s*\|") {
      continue
    }

    $parts = $line.Split("|")
    if ($parts.Count -lt 9) {
      continue
    }

    if ($parts[3].Trim() -eq $BranchName) {
      $lines[$index] = $newRow
      $replaced = $true
      break
    }
  }

  if (-not $replaced) {
    $lines += $newRow
  }

  $lines | Set-Content -Path $Path -Encoding utf8
}

if (-not (Test-CommandExists "git")) {
  throw "git CLI is required."
}

$slug = New-Slug -Value $TaskName
$branchName = "$BranchPrefix/$slug"
$startPoint = "$Remote/$BaseBranch"

git fetch $Remote $BaseBranch
if ($LASTEXITCODE -ne 0) {
  throw "git fetch failed: $Remote $BaseBranch"
}

$localBranch = git branch --list $branchName
if ($LASTEXITCODE -ne 0) {
  throw "git branch check failed."
}
if (-not [string]::IsNullOrWhiteSpace($localBranch)) {
  throw "Local branch already exists: $branchName"
}

$remoteBranch = git ls-remote --heads $Remote $branchName
if ($LASTEXITCODE -ne 0) {
  throw "remote branch check failed."
}
if (-not [string]::IsNullOrWhiteSpace($remoteBranch)) {
  throw "Remote branch already exists: $Remote/$branchName"
}

if (-not (Test-Path $WorktreesDir)) {
  New-Item -ItemType Directory -Path $WorktreesDir | Out-Null
}

$worktreePath = Join-Path $WorktreesDir $branchName.Replace("/", "-")
if (Test-Path $worktreePath) {
  throw "Worktree path already exists: $worktreePath"
}

$resolvedTaskBoardPath = $TaskBoardPath
$taskBoardRows = Get-TaskBoardRows -Path $resolvedTaskBoardPath
$normalizedClaims = @()
foreach ($item in $ClaimedPaths) {
  $normalized = Normalize-PathToken -Value $item
  if (-not [string]::IsNullOrWhiteSpace($normalized)) {
    $normalizedClaims += $normalized
  }
}
$normalizedClaims = @($normalizedClaims | Select-Object -Unique)

Assert-NoOwnershipCollision `
  -TaskBoardRows $taskBoardRows `
  -Owner $Owner `
  -BranchName $branchName `
  -WorktreePath $worktreePath `
  -ClaimedPathTokens $normalizedClaims `
  -AllowClaimedPathConflict:$AllowClaimedPathConflict

git worktree add $worktreePath -b $branchName $startPoint
if ($LASTEXITCODE -ne 0) {
  throw "git worktree add failed."
}

if (-not $SkipTaskBoardUpdate) {
  Update-TaskBoardRow `
    -Path $resolvedTaskBoardPath `
    -Owner $Owner `
    -TaskName $TaskName `
    -BranchName $branchName `
    -WorktreePath $worktreePath `
    -ClaimedPathTokens $normalizedClaims
}

Write-Host "Created worktree."
Write-Host ("Owner:    {0}" -f $Owner)
Write-Host ("Branch:   {0}" -f $branchName)
Write-Host ("Path:     {0}" -f (Resolve-Path $worktreePath))
Write-Host ("Base:     {0}" -f $startPoint)
if ($normalizedClaims.Count -gt 0) {
  Write-Host ("Claims:   {0}" -f ($normalizedClaims -join ", "))
} else {
  Write-Host "Claims:   -"
}
if ($SkipTaskBoardUpdate) {
  Write-Host ("TaskBoard:{0} (skip update)" -f $resolvedTaskBoardPath)
} else {
  Write-Host ("TaskBoard:{0} (updated)" -f $resolvedTaskBoardPath)
}
Write-Host ""
Write-Host "Next:"
Write-Host ("1) cd `"{0}`"" -f (Resolve-Path $worktreePath))
Write-Host "2) implement changes"
Write-Host "3) git add -A && git commit"
Write-Host ("4) git push -u {0} {1}" -f $Remote, $branchName)
