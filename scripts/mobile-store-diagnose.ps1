param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [Parameter(Mandatory = $true)][string]$RunId,
  [switch]$AsJson
)

$ErrorActionPreference = "Stop"

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Resolve-FailureKey {
  param(
    [string]$JobName,
    [string]$StepName
  )

  if ($StepName -like "*Upload Android AAB to Google Play*") { return "android_play_upload" }
  if ($StepName -like "*Validate Android signing secrets*") { return "android_signing_secret_validation" }
  if ($StepName -like "*Prepare Android keystore*") { return "android_keystore_prepare" }
  if ($StepName -like "*Build Android signed AAB*") { return "android_aab_build" }
  if ($StepName -like "*Import Apple code-sign certificate*") { return "ios_codesign_certificate_import" }
  if ($StepName -like "*Download App Store provisioning profile*") { return "ios_profile_download" }
  if ($StepName -like "*Build iOS signed archive and IPA*") { return "ios_archive_build" }
  if ($StepName -like "*Upload to TestFlight*") { return "ios_testflight_upload" }

  if ($JobName -eq "android-signed-aab") { return "android_job_failure" }
  if ($JobName -eq "ios-testflight-upload") { return "ios_job_failure" }
  return "unknown_failure"
}

function Get-RecoveryActions {
  param([string]$FailureKey)

  switch ($FailureKey) {
    "android_play_upload" {
      return @(
        "Verify GOOGLE_PLAY_SERVICE_ACCOUNT_JSON has Play Console release permissions for the package.",
        "Confirm android_package_name (or GOOGLE_PLAY_PACKAGE_NAME) matches an existing app in Play Console.",
        "Retry with android_distribution_mode=play_upload after validating track/release status inputs."
      )
    }
    "android_signing_secret_validation" {
      return @(
        "Set MOBILE_ANDROID_KEYSTORE_BASE64, MOBILE_ANDROID_KEY_ALIAS, MOBILE_ANDROID_KEY_PASSWORD, and MOBILE_ANDROID_STORE_PASSWORD.",
        "If play upload mode is used, also set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON and package name input/secret.",
        "Run scripts/mobile-store-preflight.ps1 again before rerunning the workflow."
      )
    }
    "android_keystore_prepare" {
      return @(
        "Regenerate base64 from a valid release keystore and update MOBILE_ANDROID_KEYSTORE_BASE64.",
        "Validate key alias/password pair against the keystore locally.",
        "Re-run build_only first to isolate signing before play_upload."
      )
    }
    "android_aab_build" {
      return @(
        "Inspect Flutter/Gradle build logs for signing or dependency errors.",
        "Confirm android/key.properties values map to the imported keystore content.",
        "Run scripts/build-mobile-apk.ps1 locally with release mode to reproduce quickly."
      )
    }
    "ios_codesign_certificate_import" {
      return @(
        "Regenerate MOBILE_IOS_P12_BASE64 from a valid Apple Distribution certificate and verify MOBILE_IOS_P12_PASSWORD.",
        "Ensure certificate is not expired/revoked and matches MOBILE_IOS_TEAM_ID.",
        "Retry testflight mode after replacing certificate secrets."
      )
    }
    "ios_profile_download" {
      return @(
        "Verify MOBILE_IOS_BUNDLE_ID (or ios_bundle_id input) exists in App Store Connect.",
        "Check App Store API key secrets have provisioning profile access.",
        "Confirm Team ID and bundle ID belong to the same Apple Developer team."
      )
    }
    "ios_archive_build" {
      return @(
        "Validate provisioning profile specifier and signing identity for the selected bundle id.",
        "Inspect xcodebuild archive logs for signing mismatch errors.",
        "Run build_only first to isolate Flutter build issues before signed archive."
      )
    }
    "ios_testflight_upload" {
      return @(
        "Verify App Store Connect API credentials and app permissions for upload.",
        "Ensure the IPA bundle id/version is valid for the target app record.",
        "Retry with updated release notes and confirmed issuer/key pair."
      )
    }
    "android_job_failure" {
      return @(
        "Inspect android-signed-aab job logs for first failed step and exception details.",
        "Run scripts/mobile-store-preflight.ps1 with the same target/mode before retry.",
        "Retry in build_only mode first, then switch back to play_upload."
      )
    }
    "ios_job_failure" {
      return @(
        "Inspect ios-testflight-upload job logs for first failed step and exception details.",
        "Re-run scripts/mobile-store-preflight.ps1 with ios testflight settings.",
        "Validate iOS signing secrets and bundle id before retry."
      )
    }
    default {
      return @(
        "Open the failed job log and capture the first failed step/error line.",
        "Validate mode-specific secrets with scripts/mobile-store-preflight.ps1.",
        "Update credentials/inputs and rerun the workflow."
      )
    }
  }
}

if (-not (Test-CommandExists "gh")) {
  throw "gh CLI is required."
}

$runJson = gh run view $RunId --repo $Repo --json conclusion,status,url,displayTitle,workflowName,jobs
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($runJson)) {
  throw "failed to load workflow run details for run id: $RunId"
}

$runInfo = $runJson | ConvertFrom-Json
$failedJobs = @($runInfo.jobs | Where-Object { $_.conclusion -in @("failure", "cancelled", "timed_out", "action_required") })
$failedJobs = @($failedJobs | Sort-Object startedAt)

$failedJob = $null
$failedStep = $null
foreach ($job in $failedJobs) {
  $candidate = @($job.steps | Where-Object { $_.conclusion -eq "failure" } | Sort-Object number | Select-Object -First 1)
  if ($candidate.Count -gt 0) {
    $failedJob = $job
    $failedStep = $candidate[0]
    break
  }
}

if ($null -eq $failedJob -and $failedJobs.Count -gt 0) {
  $failedJob = $failedJobs[0]
}

$failedJobName = if ($null -eq $failedJob) { "" } else { "$($failedJob.name)" }
$failedStepName = if ($null -eq $failedStep) { "" } else { "$($failedStep.name)" }
$failureKey = Resolve-FailureKey -JobName $failedJobName -StepName $failedStepName
$recoveryActions = Get-RecoveryActions -FailureKey $failureKey

$result = [ordered]@{
  runId = "$RunId"
  workflowName = "$($runInfo.workflowName)"
  displayTitle = "$($runInfo.displayTitle)"
  status = "$($runInfo.status)"
  conclusion = "$($runInfo.conclusion)"
  url = "$($runInfo.url)"
  failedJobName = $failedJobName
  failedJobUrl = if ($null -eq $failedJob) { "" } else { "$($failedJob.url)" }
  failedStepName = $failedStepName
  failureKey = $failureKey
  recoveryActions = $recoveryActions
}

if ($AsJson) {
  $result | ConvertTo-Json -Depth 6
  exit 0
}

Write-Host "== Mobile Store Run Diagnosis =="
Write-Host ("RunId: {0}" -f $result.runId)
Write-Host ("Workflow: {0}" -f $result.workflowName)
Write-Host ("Conclusion: {0}" -f $result.conclusion)
if (-not [string]::IsNullOrWhiteSpace($result.failedJobName)) {
  Write-Host ("Failed Job: {0}" -f $result.failedJobName)
}
if (-not [string]::IsNullOrWhiteSpace($result.failedStepName)) {
  Write-Host ("Failed Step: {0}" -f $result.failedStepName)
}
Write-Host ("Failure Key: {0}" -f $result.failureKey)
Write-Host "Recovery actions:"
foreach ($action in $result.recoveryActions) {
  Write-Host ("- " + $action)
}
Write-Host ("Run URL: {0}" -f $result.url)

exit 0
