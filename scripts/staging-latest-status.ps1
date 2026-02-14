param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [string]$Workflow = "deploy-staging.yml",
  [string]$Branch = "develop",
  [string]$Event = "",
  [int]$Limit = 1,
  [switch]$Wait,
  [int]$WatchIntervalSeconds = 10,
  [switch]$RequireSuccess,
  [switch]$AsMarkdown,
  [switch]$AsJson
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-GhJson {
  param(
    [Parameter(Mandatory = $true)]
    [string[]]$Args
  )

  $output = & gh @Args
  if ($LASTEXITCODE -ne 0) {
    throw "gh command failed: gh $($Args -join ' ')"
  }

  if ([string]::IsNullOrWhiteSpace($output)) {
    return $null
  }

  return ($output | ConvertFrom-Json)
}

function Get-DurationSeconds {
  param(
    [string]$StartedAt,
    [string]$CompletedAt
  )

  if ([string]::IsNullOrWhiteSpace($StartedAt) -or [string]::IsNullOrWhiteSpace($CompletedAt)) {
    return $null
  }

  $start = ([DateTime]$StartedAt).ToUniversalTime()
  $end = ([DateTime]$CompletedAt).ToUniversalTime()
  return [math]::Round(($end - $start).TotalSeconds, 1)
}

function Get-StepConclusion {
  param(
    [object]$Job,
    [string]$StepName
  )

  if ($null -eq $Job -or $null -eq $Job.steps) {
    return "-"
  }

  $step = $Job.steps | Where-Object { $_.name -eq $StepName } | Select-Object -First 1
  if ($null -eq $step) {
    return "-"
  }

  if ([string]::IsNullOrWhiteSpace($step.conclusion)) {
    return $step.status
  }

  return $step.conclusion
}

if (-not (Test-CommandExists "gh")) {
  throw "gh CLI is required."
}

if ($Limit -lt 1) {
  throw "Limit must be >= 1"
}

$outputModeCount = 0
if ($AsJson) { $outputModeCount++ }
if ($AsMarkdown) { $outputModeCount++ }
if ($outputModeCount -gt 1) {
  throw "Use only one output mode: -AsJson or -AsMarkdown"
}

$runListArgs = @(
  "run", "list",
  "--repo", $Repo,
  "--workflow", $Workflow,
  "--limit", "$Limit",
  "--json", "databaseId,workflowName,event,status,conclusion,createdAt,updatedAt,headBranch,headSha,url"
)

if (-not [string]::IsNullOrWhiteSpace($Branch)) {
  $runListArgs += @("--branch", $Branch)
}

if (-not [string]::IsNullOrWhiteSpace($Event)) {
  $runListArgs += @("--event", $Event)
}

$runs = Invoke-GhJson -Args $runListArgs

if ($null -eq $runs -or $runs.Count -eq 0) {
  throw "No workflow runs found for $Repo / $Workflow (branch='$Branch', event='$Event')"
}

$selectedRun = $runs[0]
$runId = "$($selectedRun.databaseId)"

if ($Wait -and $selectedRun.status -ne "completed") {
  & gh run watch $runId --repo $Repo --interval $WatchIntervalSeconds --exit-status
}

$runView = Invoke-GhJson -Args @(
  "run", "view", $runId,
  "--repo", $Repo,
  "--json", "databaseId,workflowName,event,status,conclusion,createdAt,updatedAt,headBranch,headSha,url,jobs"
)

$jobs = @($runView.jobs)
$deployJob = $jobs | Where-Object { $_.name -eq "deploy" } | Select-Object -First 1
$verifyStepConclusion = Get-StepConclusion -Job $deployJob -StepName "Verify deployment"
$runDeployConclusion = if ($null -eq $deployJob) { "-" } else { $deployJob.conclusion }

$jobTable = $jobs | ForEach-Object {
  [PSCustomObject]@{
    Job = $_.name
    Status = $_.status
    Conclusion = $_.conclusion
    DurationSec = Get-DurationSeconds -StartedAt $_.startedAt -CompletedAt $_.completedAt
  }
}

$summary = [PSCustomObject]@{
  Repo = $Repo
  Workflow = $runView.workflowName
  RunId = $runView.databaseId
  Event = $runView.event
  Branch = $runView.headBranch
  HeadSha = $runView.headSha
  Status = $runView.status
  Conclusion = $runView.conclusion
  DeployJobConclusion = $runDeployConclusion
  VerifyStepConclusion = $verifyStepConclusion
  CreatedAtUtc = ([DateTime]$runView.createdAt).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  UpdatedAtUtc = ([DateTime]$runView.updatedAt).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
  Url = $runView.url
}

if ($AsJson) {
  [PSCustomObject]@{
    summary = $summary
    jobs = $jobTable
  } | ConvertTo-Json -Depth 6
} elseif ($AsMarkdown) {
  Write-Output "| RunId | Event | Branch | HeadSha | Conclusion | Deploy | Verify | URL |"
  Write-Output "|---|---|---|---|---|---|---|---|"
  Write-Output "| $($summary.RunId) | $($summary.Event) | $($summary.Branch) | $($summary.HeadSha) | $($summary.Conclusion) | $($summary.DeployJobConclusion) | $($summary.VerifyStepConclusion) | $($summary.Url) |"
  Write-Output ""
  Write-Output "| Job | Status | Conclusion | DurationSec |"
  Write-Output "|---|---|---|---|"
  foreach ($job in $jobTable) {
    Write-Output "| $($job.Job) | $($job.Status) | $($job.Conclusion) | $($job.DurationSec) |"
  }
} else {
  Write-Host "== Latest Staging Deploy Run =="
  $summary | Format-List
  Write-Host ""
  Write-Host "== Jobs =="
  $jobTable | Format-Table -AutoSize
}

if ($RequireSuccess -and $runView.conclusion -ne "success") {
  throw "Latest run is not successful (run_id=$runId, conclusion=$($runView.conclusion))"
}

exit 0
