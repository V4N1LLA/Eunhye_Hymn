param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string[]]$Branches = @("develop", "staging", "production"),
  [int]$RequiredApprovals = 1,
  [switch]$RequirePrGateCheck,
  [switch]$DryRun
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Assert-PositiveInt {
  param(
    [string]$Name,
    [int]$Value
  )
  if ($Value -lt 1) {
    throw "$Name must be >= 1"
  }
}

function ConvertTo-JsonFile {
  param(
    [Parameter(Mandatory = $true)]
    [object]$InputObject
  )
  $path = [System.IO.Path]::GetTempFileName()
  $InputObject | ConvertTo-Json -Depth 10 | Set-Content -Path $path -Encoding utf8
  return $path
}

if (-not (Test-CommandExists "gh")) {
  throw "gh CLI is required."
}

Assert-PositiveInt -Name "RequiredApprovals" -Value $RequiredApprovals

gh auth status 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
  throw "gh auth status failed. Run 'gh auth login' first."
}

$contexts = @()
if ($RequirePrGateCheck) {
  $contexts += "PR Gate / gate"
}

foreach ($branch in $Branches) {
  if ([string]::IsNullOrWhiteSpace($branch)) {
    continue
  }

  $trimmedBranch = $branch.Trim()
  if ([string]::IsNullOrWhiteSpace($trimmedBranch)) {
    continue
  }

  $branchInfo = gh api "repos/$Repo/branches/$trimmedBranch" 2>$null
  if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($branchInfo)) {
    throw "Branch not found or inaccessible: $trimmedBranch"
  }

  $payload = @{
    required_status_checks           = $(if ($RequirePrGateCheck) {
      @{
        strict   = $true
        contexts = $contexts
      }
    } else {
      $null
    })
    enforce_admins                   = $true
    required_pull_request_reviews    = @{
      dismiss_stale_reviews           = $true
      require_code_owner_reviews      = $false
      required_approving_review_count = $RequiredApprovals
      require_last_push_approval      = $false
    }
    restrictions                     = $null
    required_linear_history          = $true
    allow_force_pushes               = $false
    allow_deletions                  = $false
    block_creations                  = $false
    required_conversation_resolution = $true
    lock_branch                      = $false
    allow_fork_syncing               = $true
  }

  if ($DryRun) {
    Write-Host "== DryRun: $trimmedBranch =="
    $payload | ConvertTo-Json -Depth 10
    Write-Host ""
    continue
  }

  $jsonPath = ConvertTo-JsonFile -InputObject $payload
  try {
    gh api `
      --method PUT `
      -H "Accept: application/vnd.github+json" `
      "repos/$Repo/branches/$trimmedBranch/protection" `
      --input $jsonPath 1>$null

    if ($LASTEXITCODE -ne 0) {
      throw "Failed to apply branch protection: $trimmedBranch"
    }
  } finally {
    Remove-Item $jsonPath -Force -ErrorAction SilentlyContinue
  }

  Write-Host "Applied branch protection: $trimmedBranch"
}

Write-Host "Done."
