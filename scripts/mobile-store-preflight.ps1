param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [ValidateSet("android", "ios", "both")][string]$Target = "both",
  [ValidateSet("build_only", "play_upload")][string]$AndroidDistributionMode = "build_only",
  [string]$AndroidPackageName = "",
  [ValidateSet("build_only", "testflight")][string]$IosDistributionMode = "build_only",
  [string]$IosBundleId = ""
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
    Name = $Name
    Passed = $Passed
    Detail = $Detail
  }
}

function Test-CommandExists {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Add-SecretCheck {
  param(
    [string]$SecretName,
    [string]$Reason
  )

  $exists = $script:existingSecrets.ContainsKey($SecretName)
  if ($exists) {
    Add-Check ("secret " + $SecretName) $true "set"
    return
  }

  Add-Check ("secret " + $SecretName) $false ("missing: " + $Reason)
}

Add-Check "gh cli" (Test-CommandExists "gh") "required"
if (-not (Test-CommandExists "gh")) {
  $checks | Format-Table -AutoSize
  exit 1
}

gh auth status 1>$null 2>$null
Add-Check "gh auth status" ($LASTEXITCODE -eq 0) "github auth"
if ($LASTEXITCODE -ne 0) {
  $checks | Format-Table -AutoSize
  exit 1
}

$secretNames = gh secret list --repo $Repo --json name --jq '.[].name' 2>$null
if ($LASTEXITCODE -ne 0) {
  Add-Check "gh secret list" $false "failed"
  $checks | Format-Table -AutoSize
  exit 1
}

$script:existingSecrets = @{}
foreach ($name in $secretNames) {
  $trimmed = "$name".Trim()
  if (-not [string]::IsNullOrWhiteSpace($trimmed)) {
    $script:existingSecrets[$trimmed] = $true
  }
}
Add-Check "gh secret list" $true "ok"

$targetAndroid = ($Target -eq "android" -or $Target -eq "both")
$targetIos = ($Target -eq "ios" -or $Target -eq "both")

if ($targetAndroid) {
  Add-SecretCheck "MOBILE_ANDROID_KEYSTORE_BASE64" "android signed AAB build"
  Add-SecretCheck "MOBILE_ANDROID_KEY_ALIAS" "android signed AAB build"
  Add-SecretCheck "MOBILE_ANDROID_KEY_PASSWORD" "android signed AAB build"
  Add-SecretCheck "MOBILE_ANDROID_STORE_PASSWORD" "android signed AAB build"

  if ($AndroidDistributionMode -eq "play_upload") {
    Add-SecretCheck "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON" "google play upload mode"

    if (-not [string]::IsNullOrWhiteSpace($AndroidPackageName)) {
      Add-Check "android package name" $true "input override"
    } elseif ($script:existingSecrets.ContainsKey("GOOGLE_PLAY_PACKAGE_NAME")) {
      Add-Check "android package name" $true "secret GOOGLE_PLAY_PACKAGE_NAME"
    } else {
      Add-Check "android package name" $false "missing input or GOOGLE_PLAY_PACKAGE_NAME secret"
    }
  } else {
    Add-Check "android play upload secrets" $true "not required for build_only"
  }
}

if ($targetIos) {
  if ($IosDistributionMode -eq "testflight") {
    if (-not [string]::IsNullOrWhiteSpace($IosBundleId)) {
      Add-Check "ios bundle id" $true "input override"
    } else {
      Add-SecretCheck "MOBILE_IOS_BUNDLE_ID" "ios testflight upload mode"
    }

    Add-SecretCheck "MOBILE_IOS_TEAM_ID" "ios testflight upload mode"
    Add-SecretCheck "MOBILE_IOS_P12_BASE64" "ios testflight upload mode"
    Add-SecretCheck "MOBILE_IOS_P12_PASSWORD" "ios testflight upload mode"
    Add-SecretCheck "MOBILE_IOS_APPSTORE_ISSUER_ID" "ios testflight upload mode"
    Add-SecretCheck "MOBILE_IOS_APPSTORE_API_KEY_ID" "ios testflight upload mode"
    Add-SecretCheck "MOBILE_IOS_APPSTORE_API_PRIVATE_KEY" "ios testflight upload mode"
  } else {
    Add-Check "ios testflight secrets" $true "not required for build_only"
  }
}

$checks | Format-Table -AutoSize

$failed = @($checks | Where-Object { -not $_.Passed })
if ($failed.Count -gt 0) {
  exit 1
}

exit 0
