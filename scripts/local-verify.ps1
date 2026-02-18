param(
    [string] $ComposeFile = "infra/docker/docker-compose.yml",
    [string] $ApiBaseUrl = "http://localhost:8080/api/v1",
    [string] $AdminLoginId = "local_admin",
    [string] $AdminLoginPassword = "local_admin_2026!",
    [string] $InviteCode = "",
    [int] $InviteMaxUses = 20,
    [int] $ApiTimeoutSeconds = 90,
    [string] $AwsAccessKeyId = "test",
    [string] $AwsSecretAccessKey = "test",
    [switch] $SkipDockerUp,
    [switch] $KeepGeneratedFiles,
    [string] $SummaryFile
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot ".env"
$composeFilePath = Join-Path $repoRoot $ComposeFile
$script:ApiBaseUrl = $ApiBaseUrl.TrimEnd("/")

if (-not (Test-Path -LiteralPath $composeFilePath)) {
  throw "Compose file not found: $composeFilePath"
}

function Ensure-Command {
  param([string] $Name)
  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Required command not found: $Name"
  }
}

function Write-Section {
  param([string]$Text)
  Write-Host ""
  Write-Host "===== $Text =====" -ForegroundColor Cyan
}

function Write-Check {
  param([bool] $Condition, [string] $Message, [bool] $WarnOnly = $false)
  if ($Condition) {
    Write-Host "[PASS] $Message" -ForegroundColor Green
  } elseif ($WarnOnly) {
    Write-Host "[WARN] $Message" -ForegroundColor Yellow
  } else {
    throw "[FAIL] $Message"
  }
}

function Set-EnvValue {
  param(
    [Parameter(Mandatory = $true)] [string] $Path,
    [Parameter(Mandatory = $true)] [string] $Key,
    [Parameter(Mandatory = $true)] [string] $Value
  )

  if (-not (Test-Path -LiteralPath $Path)) {
    return $false
  }

  $lines = [System.Collections.Generic.List[string]]::new()
  $updated = $false
  $existed = $false
  $alreadyReplaced = $false
  foreach ($line in Get-Content -LiteralPath $Path) {
    if ($line -match "^\s*$([regex]::Escape($Key))\s*=.*$") {
      if (-not $existed) {
        $existed = $true
      }
      if (-not $alreadyReplaced) {
        $lines.Add("$Key=$Value")
        $alreadyReplaced = $true
        $updated = $true
      }
    } else {
      $lines.Add($line)
    }
  }

  if (-not $existed) {
    $lines.Add("$Key=$Value")
    $updated = $true
  }

  Set-Content -LiteralPath $Path -Value $lines -Encoding utf8
  return $updated
}

function Try-AssetUpload {
  param(
    [Parameter(Mandatory = $true)] [string] $PrimaryUrl,
    [string] $FallbackPublicUrl,
    [Parameter(Mandatory = $true)] [string] $FilePath,
    [Parameter(Mandatory = $true)] [string] $ContentType
  )

  try {
    $response = Invoke-WebRequest -Method Put -Uri $PrimaryUrl -InFile $FilePath -ContentType $ContentType
    return $response
  } catch {
    $errorMessage = $_.Exception.Message
    if ([string]::IsNullOrWhiteSpace($FallbackPublicUrl)) {
      throw "Asset upload failed: $errorMessage"
    }

    try {
      $primaryUri = [Uri]$PrimaryUrl
      $publicUri = [Uri]$FallbackPublicUrl
      if ($primaryUri.Host -ieq $publicUri.Host) {
        throw $errorMessage
      }

      $fallbackBuilder = [System.UriBuilder]::new()
      $fallbackBuilder.Scheme = $publicUri.Scheme
      $fallbackBuilder.Host = $publicUri.Host
      if ($publicUri.Port -ne -1) {
        $fallbackBuilder.Port = $publicUri.Port
      }
      $fallbackBuilder.Path = $primaryUri.AbsolutePath
      $fallbackBuilder.Query = $primaryUri.Query.TrimStart("?")

      $fallbackUrl = $fallbackBuilder.Uri.ToString()
      Write-Host " - Presigned upload host fallback: $($publicUri.Host):$($publicUri.Port)"
      $response = Invoke-WebRequest -Method Put -Uri $fallbackUrl -InFile $FilePath -ContentType $ContentType
      return $response
    } catch {
      throw "Asset upload failed: $errorMessage"
    }
  }
}

function Ensure-ValueInEnvFile {
  param(
    [string] $Path,
    [string] $Key,
    [string] $Value
  )

  if (Set-EnvValue -Path $Path -Key $Key -Value $Value) {
    Write-Host "  - $Key updated in .env"
  } else {
    Write-Host "  - $Key already exists in .env"
  }
}

function Invoke-EhApi {
  param(
    [string] $Method,
    [string] $Path,
    [object] $Body = $null,
    [string] $Token = ""
  )

  $uri = "$script:ApiBaseUrl$Path"
  $headers = @{}
  if (-not [string]::IsNullOrWhiteSpace($Token)) {
    $headers.Authorization = "Bearer $Token"
  }

  $request = @{
    Uri        = $uri
    Method     = $Method
    Headers    = $headers
    ErrorAction = "Stop"
  }

  if ($null -ne $Body) {
    $request.ContentType = "application/json"
    $request.Body = $Body | ConvertTo-Json -Depth 20
  }

  try {
    $response = Invoke-RestMethod @request
  } catch {
    $exception = $_.Exception
    $statusText = "unknown"
    $payloadText = $null
    $innerResponse = $null

    if ($exception.Response) {
      try {
        if ($exception.Response.StatusCode -as [int]) {
          $statusText = [int]$exception.Response.StatusCode
        }
      } catch {
      }
      try {
        $stream = $exception.Response.GetResponseStream()
        if ($stream) {
          $reader = [System.IO.StreamReader]::new($stream)
          $payloadText = $reader.ReadToEnd()
          $reader.Dispose()
          $stream.Dispose()
        }
      } catch {
      }
    }

    if ([string]::IsNullOrWhiteSpace($payloadText)) {
      $payloadText = $exception.Message
    }

    $errorMessage = "API $Method $Path failed (HTTP $statusText): $payloadText"
    throw $errorMessage
  }

  if ($null -eq $response.success) {
    throw "Invalid API response for $Method ${Path}: missing success flag."
  }
  if (-not $response.success) {
    $reason = $response.error.message
    if ([string]::IsNullOrWhiteSpace($reason)) {
      $reason = "unknown reason"
    }
    throw "API $Method ${Path} returned success=false: $reason"
  }

  return $response.data
}

function Wait-ApiReady {
  param([int] $TimeoutSeconds = 90)
  Write-Section "API readiness check"
  $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

  while ((Get-Date) -lt $deadline) {
    try {
      $ping = Invoke-EhApi -Method "GET" -Path "/ping"
      if ($ping.ok -eq $true) {
        Write-Host "[PASS] API ready: /ping"
        return
      }
    } catch {
      Start-Sleep -Seconds 1
    }
  }
  throw "API is not ready within $TimeoutSeconds seconds. Check docker logs."
}

Write-Section "Initial checks"
Ensure-Command "docker"
Ensure-Command "Invoke-RestMethod"

if (-not (Test-Path $envPath)) {
  $example = Join-Path $repoRoot ".env.example"
  if (-not (Test-Path -LiteralPath $example)) {
    throw ".env is missing and .env.example is not found."
  }
  Copy-Item -Path $example -Destination $envPath
  Write-Host "Created .env from .env.example"
}

Ensure-ValueInEnvFile -Path $envPath -Key "ADMIN_LOGIN_ID" -Value $AdminLoginId
Ensure-ValueInEnvFile -Path $envPath -Key "ADMIN_LOGIN_PASSWORD" -Value $AdminLoginPassword
Ensure-ValueInEnvFile -Path $envPath -Key "AWS_ACCESS_KEY_ID" -Value $AwsAccessKeyId
Ensure-ValueInEnvFile -Path $envPath -Key "AWS_SECRET_ACCESS_KEY" -Value $AwsSecretAccessKey

if (-not $SkipDockerUp) {
  Write-Section "Start docker-compose"
  docker compose --env-file $envPath -f $composeFilePath up -d --force-recreate
}

Wait-ApiReady -TimeoutSeconds $ApiTimeoutSeconds

Write-Section "Admin auth"
$adminLogin = Invoke-EhApi -Method "POST" -Path "/auth/admin/login" -Body @{
  loginId = $AdminLoginId
  password = $AdminLoginPassword
}
$adminToken = $adminLogin.accessToken
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($adminToken)) "Admin login succeeded"

$normalizedInvite = if ([string]::IsNullOrWhiteSpace($InviteCode)) {
  "LOCAL-" + (Get-Date -Format "yyyyMMddHHmmss")
} else {
  $InviteCode.Trim().ToUpperInvariant()
}

Write-Section "Invite code"
$inviteDesc = "Local smoke test " + (Get-Date -Format "yyyy-MM-dd HH:mm:ss")
$createdInvite = Invoke-EhApi -Method "POST" -Path "/admin/invite-codes" -Token $adminToken -Body @{
  code = $normalizedInvite
  description = $inviteDesc
  maxUses = $InviteMaxUses
  expiresAt = $null
}
Write-Check -Condition ($createdInvite.code -eq $normalizedInvite) "Invite code created: $($createdInvite.code)"

$validatedInvite = Invoke-EhApi -Method "POST" -Path "/auth/invite/validate" -Body @{
  code = "  $($normalizedInvite.ToLowerInvariant())  "
}
Write-Check -Condition ($validatedInvite.valid -eq $true) "Invite code validation works with spaces/case-insensitive input"

Write-Section "Create hymn + asset"
$hymn = Invoke-EhApi -Method "POST" -Path "/admin/hymns" -Token $adminToken -Body @{
  title = "Local Smoke Hymn"
  number = "TST-001"
  tags = "local,smoke"
  enabled = $true
}
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($hymn.id)) "Hymn created: $($hymn.id)"

$presign = Invoke-EhApi -Method "POST" -Path "/admin/assets/presign" -Token $adminToken -Body @{
  hymnId = $hymn.id
  type = "PNG"
  part = "ALL"
  filename = "local-smoke-png.png"
  contentType = "image/png"
}
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($presign.uploadUrl)) "Asset presign issued"

$tmpRoot = Join-Path $repoRoot ".tmp"
if (-not (Test-Path $tmpRoot)) {
  New-Item -ItemType Directory -Path $tmpRoot | Out-Null
}
$assetFile = Join-Path $tmpRoot "local-smoke-png.png"
[IO.File]::WriteAllBytes(
  $assetFile,
  [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
)

try {
  $upload = Try-AssetUpload -PrimaryUrl $presign.uploadUrl -FallbackPublicUrl $presign.publicUrl -FilePath $assetFile -ContentType "image/png"
  Write-Check -Condition ($upload.StatusCode -ge 200 -and $upload.StatusCode -lt 300) "Demo asset uploaded"
} catch {
  throw "Asset upload failed: $($_.Exception.Message)"
}

$confirmedAsset = Invoke-EhApi -Method "POST" -Path "/admin/assets/confirm" -Token $adminToken -Body @{
  hymnId = $hymn.id
  type = "PNG"
  part = "ALL"
  publicUrl = $presign.publicUrl
  objectKey = $presign.objectKey
  checksum = "local-checksum"
  version = "1"
}
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($confirmedAsset.assetId)) "Asset confirmed in DB"

$detail = Invoke-EhApi -Method "GET" -Path "/hymns/$($hymn.id)" -Token $adminToken
Write-Check -Condition ($null -ne $detail.assets -and $detail.assets.Count -gt 0) "Hymn detail includes assets"
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($detail.assets[0].url)) "Asset URL exists in detail response"

Write-Section "Host-side asset URL verification"
$assetUrl = $detail.assets[0].url
if ([string]::IsNullOrWhiteSpace($assetUrl)) {
  Write-Check -Condition $false "Asset URL is missing"
} else {
  try {
    $head = Invoke-WebRequest -Method Head -Uri $assetUrl -TimeoutSec 10 -ErrorAction Stop
    Write-Check -Condition ($head.StatusCode -ge 200 -and $head.StatusCode -lt 400) "Asset URL is reachable from host"
  } catch {
    Write-Check -Condition $false -WarnOnly $true "Host could not access asset URL. If using Android emulator, set S3_PUBLIC_BASE_URL to http://10.0.2.2:4566/local-bucket in .env and restart API."
  }
}

Write-Section "DB account login check"
$createdUser = Invoke-EhApi -Method "POST" -Path "/admin/users" -Token $adminToken -Body @{
  displayName = "local-smoke-user"
  role = "USER"
  status = "ACTIVE"
}
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($createdUser.id)) "User created in DB: $($createdUser.id)"

$devLogin = Invoke-EhApi -Method "POST" -Path "/auth/dev/login" -Body @{
  userId = $createdUser.id
  role = "USER"
  displayName = $createdUser.displayName
}
Write-Check -Condition (-not [string]::IsNullOrWhiteSpace($devLogin.accessToken)) "Dev login on created DB user returned token"

$profile = Invoke-EhApi -Method "GET" -Path "/me/profile" -Token $devLogin.accessToken
Write-Check -Condition ($profile.userId -eq $createdUser.id) "Profile matches DB user id"

Write-Section "Kakao login verification"
Write-Host "Manual check only: launch emulator app and test:"
Write-Host "1) Enter invite code: $normalizedInvite"
Write-Host "2) Tap 'Kakao Login' and complete consent"
Write-Host "3) Confirm /me/profile and hymns list load without errors"
Write-Host "4) Open detail for '$($hymn.title)' and verify image/audio is loaded"

Write-Section "Summary"
Write-Host "Invite Code: $($createdInvite.code)"
Write-Host "Hymn ID   : $($hymn.id)"
Write-Host "Asset URL : $($detail.assets[0].url)"
Write-Host "DB User   : $($createdUser.id)"
$hasSummary = -not [string]::IsNullOrWhiteSpace($SummaryFile)
if ($hasSummary) {
    if ([System.IO.Path]::IsPathRooted($SummaryFile)) {
        $summaryPath = $SummaryFile
    } else {
        $summaryPath = Join-Path $repoRoot $SummaryFile
    }
    $payload = @{
        inviteCode = $createdInvite.code
        hymnId = $hymn.id
        assetUrl = $detail.assets[0].url
        dbUserId = $createdUser.id
        generatedAt = (Get-Date).ToString("s")
    } | ConvertTo-Json
    $summaryDir = Split-Path -Path $summaryPath -Parent
    if (-not [string]::IsNullOrWhiteSpace($summaryDir) -and -not (Test-Path -LiteralPath $summaryDir)) {
        New-Item -ItemType Directory -Path $summaryDir -Force | Out-Null
    }
    Set-Content -LiteralPath $summaryPath -Value $payload -Encoding utf8
    Write-Host "Summary written: $summaryPath"
}
if (-not $KeepGeneratedFiles) {
  Remove-Item -LiteralPath $assetFile -Force -ErrorAction SilentlyContinue
  Write-Host "Temporary asset file removed: $assetFile"
} else {
  Write-Host "Temporary asset file kept: $assetFile"
}
