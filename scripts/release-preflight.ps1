param(
  [Parameter(Mandatory = $true)]
  [string]$Version,
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Branch = "staging",
  [int]$MaxStagingAgeMinutes = 1440,
  [switch]$AsJson
)

$ErrorActionPreference = "Stop"

$checks = @()

function Add-Check {
  param(
    [string]$Name,
    [bool]$Passed,
    [string]$Detail
  )

  $script:checks += [PSCustomObject]@{
    Name   = $Name
    Passed = $Passed
    Detail = $Detail
  }
}

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-GhJson {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Args
  )

  $raw = & gh @Args 2>&1
  if ($LASTEXITCODE -ne 0) {
    $detail = ($raw -join "`n").Trim()
    if ([string]::IsNullOrWhiteSpace($detail)) {
      $detail = "gh command failed"
    }
    throw "$detail"
  }

  if ([string]::IsNullOrWhiteSpace($raw)) {
    return $null
  }

  return (($raw -join "`n") | ConvertFrom-Json)
}

function Invoke-PowerShellFile {
  param(
    [Parameter(Mandatory = $true)]
    [string]$PowerShellCommand,
    [Parameter(Mandatory = $true)]
    [string]$ScriptPath,
    [string[]]$Arguments = @()
  )

  $previousErrorAction = $ErrorActionPreference
  $ErrorActionPreference = "Continue"
  try {
    $output = & $PowerShellCommand -NoProfile -File $ScriptPath @Arguments 2>&1
    $exitCode = $LASTEXITCODE
  } finally {
    $ErrorActionPreference = $previousErrorAction
  }

  return [PSCustomObject]@{
    ExitCode = $exitCode
    Output = (($output -join "`n") -replace "`e\[[\d;]*[A-Za-z]", "").Trim()
  }
}

function Get-FirstUsefulLine {
  param([string]$Text)

  if ([string]::IsNullOrWhiteSpace($Text)) {
    return ""
  }

  $lines = $Text -split "`r?`n" | ForEach-Object { $_.Trim() } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
  if ($null -eq $lines -or $lines.Count -eq 0) {
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

function Get-LatestRunForWorkflowSha {
  param(
    [string]$Repository,
    [string]$WorkflowName,
    [string]$TargetBranch,
    [string]$HeadSha
  )

  $runs = Invoke-GhJson -Args @(
    "run", "list",
    "--repo", $Repository,
    "--workflow", $WorkflowName,
    "--branch", $TargetBranch,
    "--limit", "50",
    "--json", "databaseId,workflowName,headSha,status,conclusion,url,createdAt"
  )

  if ($null -eq $runs -or $runs.Count -eq 0) {
    return $null
  }

  return $runs |
    Where-Object { $_.headSha -eq $HeadSha -and $_.status -eq "completed" -and $_.conclusion -eq "success" } |
    Sort-Object { [DateTime]$_.createdAt } -Descending |
    Select-Object -First 1
}

$tag = "v$Version"
$semverPattern = '^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$'

Add-Check "gh cli" (Test-CommandExists "gh") "required"
if (-not (Test-CommandExists "gh")) {
  $checks | Format-Table -AutoSize
  exit 1
}

gh auth status 1>$null 2>$null
$ghAuthPassed = ($LASTEXITCODE -eq 0)
if (-not $ghAuthPassed -and -not [string]::IsNullOrWhiteSpace($env:GH_TOKEN)) {
  # CI can authenticate gh via GH_TOKEN env without a persisted login session.
  $ghAuthPassed = $true
}
Add-Check "gh auth status" $ghAuthPassed "required"

$isSemVer = ($Version -match $semverPattern)
Add-Check "semver format" $isSemVer ($(if ($isSemVer) { "ok ($Version)" } else { "invalid: use MAJOR.MINOR.PATCH" }))

if ($isSemVer) {
  try {
    $tagMatchCount = Invoke-GhJson -Args @("api", "repos/$Repo/git/matching-refs/tags/$tag", "--jq", "length")
    $tagExists = [int]$tagMatchCount -gt 0
    Add-Check "tag availability" (-not $tagExists) ($(if ($tagExists) { "$tag already exists" } else { "available ($tag)" }))
  } catch {
    Add-Check "tag availability" $false ("failed: " + $_.Exception.Message)
  }
} else {
  Add-Check "tag availability" $false "skipped (invalid semver)"
}

$stagingSummary = $null
$deployHeadSha = ""
$statusScript = Join-Path $PSScriptRoot "staging-latest-status.ps1"
$powershellCmd = if (Test-CommandExists "powershell.exe") {
  "powershell.exe"
} elseif (Test-CommandExists "pwsh") {
  "pwsh"
} else {
  ""
}

if (-not (Test-Path $statusScript)) {
  Add-Check "staging deploy gate" $false "missing script: $statusScript"
} elseif ([string]::IsNullOrWhiteSpace($powershellCmd)) {
  Add-Check "staging deploy gate" $false "missing powershell command (powershell.exe/pwsh)"
} else {
  $statusResult = Invoke-PowerShellFile `
    -PowerShellCommand $powershellCmd `
    -ScriptPath $statusScript `
    -Arguments @(
      "-Repo", $Repo,
      "-Branch", $Branch,
      "-Limit", "20",
      "-PreferCompleted",
      "-RequireSuccess",
      "-RequireDeploySuccess",
      "-RequireVerifySuccess",
      "-MaxAgeMinutes", "$MaxStagingAgeMinutes",
      "-AsJson"
    )

  if ($statusResult.ExitCode -ne 0) {
    $tail = Get-FirstUsefulLine -Text $statusResult.Output
    if ([string]::IsNullOrWhiteSpace($tail)) {
      $tail = "staging status command failed"
    }
    Add-Check "staging deploy gate" $false $tail
  } else {
    try {
      $statusJson = ($statusResult.Output | ConvertFrom-Json)
      $stagingSummary = $statusJson.summary
      $deployHeadSha = "$($stagingSummary.HeadSha)"
      Add-Check "staging deploy gate" $true ("run_id={0}, age_min={1}, sha={2}" -f $stagingSummary.RunId, $stagingSummary.RunAgeMinutes, $deployHeadSha)
    } catch {
      Add-Check "staging deploy gate" $false "failed: invalid JSON output"
    }
  }
}

$requiredWorkflows = @(
  "API CI",
  "Admin CI",
  "Mobile CI",
  "Mobile Release Check"
)

if ([string]::IsNullOrWhiteSpace($deployHeadSha)) {
  foreach ($workflow in $requiredWorkflows) {
    Add-Check ("workflow " + $workflow) $false "skipped (staging sha unavailable)"
  }
} else {
  foreach ($workflow in $requiredWorkflows) {
    try {
      $run = Get-LatestRunForWorkflowSha -Repository $Repo -WorkflowName $workflow -TargetBranch $Branch -HeadSha $deployHeadSha
      if ($null -eq $run) {
        Add-Check ("workflow " + $workflow) $false ("no success run for sha " + $deployHeadSha)
      } else {
        Add-Check ("workflow " + $workflow) $true ("run_id={0}" -f $run.databaseId)
      }
    } catch {
      Add-Check ("workflow " + $workflow) $false ("failed: " + $_.Exception.Message)
    }
  }
}

$failed = @($checks | Where-Object { -not $_.Passed })
$ready = ($failed.Count -eq 0)

$result = [PSCustomObject]@{
  Version = $Version
  Tag = $tag
  Repo = $Repo
  Branch = $Branch
  Ready = $ready
  StagingRunId = $(if ($null -eq $stagingSummary) { "" } else { "$($stagingSummary.RunId)" })
  StagingHeadSha = $deployHeadSha
  StagingUrl = $(if ($null -eq $stagingSummary) { "" } else { "$($stagingSummary.Url)" })
  Checks = $checks
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 6
} else {
  Write-Host "== Release Preflight =="
  Write-Host ("Version: {0}" -f $Version)
  Write-Host ("Tag:     {0}" -f $tag)
  Write-Host ("Repo:    {0}" -f $Repo)
  Write-Host ("Branch:  {0}" -f $Branch)
  Write-Host ""
  $checks | Format-Table -AutoSize
  Write-Host ""
  if ($ready) {
    Write-Host "Decision: READY"
    Write-Host ("Next: git tag -a {0} -m ""Release {0}"" && git push origin {0}" -f $tag)
  } else {
    Write-Host "Decision: HOLD"
  }
}

if ($ready) {
  exit 0
}

exit 1
