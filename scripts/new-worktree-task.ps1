param(
  [Parameter(Mandatory = $true)]
  [string]$TaskName,
  [string]$BaseBranch = "develop",
  [string]$BranchPrefix = "feat",
  [string]$Remote = "origin",
  [string]$WorktreesDir = "..\\worktrees"
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

if (-not (Test-Path $WorktreesDir)) {
  New-Item -ItemType Directory -Path $WorktreesDir | Out-Null
}

$worktreePath = Join-Path $WorktreesDir $branchName.Replace("/", "-")
if (Test-Path $worktreePath) {
  throw "Worktree path already exists: $worktreePath"
}

git worktree add $worktreePath -b $branchName $startPoint
if ($LASTEXITCODE -ne 0) {
  throw "git worktree add failed."
}

Write-Host "Created worktree."
Write-Host ("Branch:   {0}" -f $branchName)
Write-Host ("Path:     {0}" -f (Resolve-Path $worktreePath))
Write-Host ("Base:     {0}" -f $startPoint)
Write-Host ""
Write-Host "Next:"
Write-Host ("1) cd `"{0}`"" -f (Resolve-Path $worktreePath))
Write-Host "2) implement changes"
Write-Host "3) git add -A && git commit"
Write-Host ("4) git push -u {0} {1}" -f $Remote, $branchName)
