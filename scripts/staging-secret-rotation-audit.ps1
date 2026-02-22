param(
  [string]$Repo = "V4N1LLA/Eunhye_Hymn",
  [int]$MaxAgeDays = 90,
  [switch]$IncludeMobileReleaseSecrets,
  [switch]$AsJson
)

$ErrorActionPreference = "Stop"

if ($MaxAgeDays -lt 1) {
  throw "MaxAgeDays must be >= 1"
}

$requiredStagingSecrets = @(
  "AWS_ACCESS_KEY_ID",
  "AWS_SECRET_ACCESS_KEY",
  "AWS_REGION",
  "ECR_REGISTRY",
  "EC2_HOST",
  "EC2_SSH_KEY",
  "DEPLOY_ENV_FILE"
)

$mobileReleaseSecrets = @(
  "MOBILE_ANDROID_KEYSTORE_BASE64",
  "MOBILE_ANDROID_KEY_ALIAS",
  "MOBILE_ANDROID_KEY_PASSWORD",
  "MOBILE_ANDROID_STORE_PASSWORD",
  "GOOGLE_PLAY_PACKAGE_NAME",
  "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON",
  "MOBILE_IOS_BUNDLE_ID",
  "MOBILE_IOS_TEAM_ID",
  "MOBILE_IOS_P12_BASE64",
  "MOBILE_IOS_P12_PASSWORD",
  "MOBILE_IOS_APPSTORE_ISSUER_ID",
  "MOBILE_IOS_APPSTORE_API_KEY_ID",
  "MOBILE_IOS_APPSTORE_API_PRIVATE_KEY"
)

function Test-CommandExists {
  param([string]$Name)
  $command = Get-Command $Name -ErrorAction SilentlyContinue
  return $null -ne $command
}

if (-not (Test-CommandExists "gh")) {
  throw "gh cli is required."
}

gh auth status 1>$null 2>$null
if ($LASTEXITCODE -ne 0) {
  throw "gh auth required. Run: gh auth login"
}

$secretJson = gh secret list --repo $Repo --json name,updatedAt
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($secretJson)) {
  throw "Failed to load repo secrets: $Repo"
}

try {
  $repoSecrets = $secretJson | ConvertFrom-Json
} catch {
  throw "Failed to parse gh secret list response."
}

$secretByName = @{}
foreach ($secret in $repoSecrets) {
  if ($null -eq $secret.name) {
    continue
  }
  $secretByName[$secret.name] = $secret
}

$targets = @()
foreach ($name in $requiredStagingSecrets) {
  $targets += [PSCustomObject]@{
    Secret = $name
    Scope = "staging"
  }
}

if ($IncludeMobileReleaseSecrets) {
  foreach ($name in $mobileReleaseSecrets) {
    $targets += [PSCustomObject]@{
      Secret = $name
      Scope = "mobile_release"
    }
  }
}

$targets = $targets | Sort-Object Secret -Unique

$nowUtc = (Get-Date).ToUniversalTime()
$rows = @()

foreach ($target in $targets) {
  $name = $target.Secret
  $scope = $target.Scope

  if (-not $secretByName.ContainsKey($name)) {
    $rows += [PSCustomObject]@{
      Scope = $scope
      Secret = $name
      UpdatedAt = "-"
      AgeDays = "-"
      MaxAgeDays = $MaxAgeDays
      Status = "MISSING"
      Detail = "secret not found"
    }
    continue
  }

  $updatedAtRaw = $secretByName[$name].updatedAt
  [datetime]$updatedAtUtc = [datetime]::MinValue
  if (-not [datetime]::TryParse($updatedAtRaw, [ref]$updatedAtUtc)) {
    $rows += [PSCustomObject]@{
      Scope = $scope
      Secret = $name
      UpdatedAt = "$updatedAtRaw"
      AgeDays = "-"
      MaxAgeDays = $MaxAgeDays
      Status = "UNKNOWN"
      Detail = "invalid updatedAt timestamp"
    }
    continue
  }

  $updatedAtUtc = $updatedAtUtc.ToUniversalTime()
  $ageDays = [int][Math]::Floor(($nowUtc - $updatedAtUtc).TotalDays)
  if ($ageDays -lt 0) {
    $ageDays = 0
  }

  $isStale = $ageDays -gt $MaxAgeDays
  $rows += [PSCustomObject]@{
    Scope = $scope
    Secret = $name
    UpdatedAt = $updatedAtUtc.ToString("yyyy-MM-ddTHH:mm:ssZ")
    AgeDays = $ageDays
    MaxAgeDays = $MaxAgeDays
    Status = $(if ($isStale) { "STALE" } else { "OK" })
    Detail = $(if ($isStale) { "age exceeds threshold; rotate secret" } else { "within threshold" })
  }
}

$missing = @($rows | Where-Object { $_.Status -eq "MISSING" })
$stale = @($rows | Where-Object { $_.Status -eq "STALE" })
$unknown = @($rows | Where-Object { $_.Status -eq "UNKNOWN" })

if ($AsJson) {
  $rows | ConvertTo-Json -Depth 4
} else {
  Write-Host ("== Secret Rotation Audit ==")
  Write-Host ("Repo: {0}" -f $Repo)
  Write-Host ("MaxAgeDays: {0}" -f $MaxAgeDays)
  Write-Host ("IncludeMobileReleaseSecrets: {0}" -f $IncludeMobileReleaseSecrets)
  Write-Host ""
  $rows | Format-Table Scope, Secret, UpdatedAt, AgeDays, MaxAgeDays, Status, Detail -AutoSize
  Write-Host ""
  Write-Host ("Summary: OK={0}, STALE={1}, MISSING={2}, UNKNOWN={3}" -f ($rows.Count - $stale.Count - $missing.Count - $unknown.Count), $stale.Count, $missing.Count, $unknown.Count)
}

if ($missing.Count -gt 0 -or $stale.Count -gt 0 -or $unknown.Count -gt 0) {
  exit 1
}

exit 0
