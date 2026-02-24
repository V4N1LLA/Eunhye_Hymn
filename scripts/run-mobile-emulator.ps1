param(
    [ValidateSet("local", "staging", "release")]
    [string] $Environment = "local",
    [string] $DeviceId = "emulator-5554",
    [string] $ApiBaseUrl,
    [string] $KakaoNativeAppKey
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$mobileDir = Join-Path $repoRoot "apps/mobile"
$mobileEnv = Join-Path $mobileDir ".env"
$repoEnv = Join-Path $repoRoot ".env"
$profileFile = Join-Path $mobileDir ("env/{0}.json" -f $Environment)

$flutterWrapper = Join-Path $PSScriptRoot "flutterw.ps1"
if (-not (Test-Path -LiteralPath $flutterWrapper)) {
    throw "Flutter wrapper not found: $flutterWrapper"
}
if (-not (Test-Path -LiteralPath $profileFile)) {
    throw "Environment profile not found: $profileFile"
}

function Read-EnvValue {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,
        [Parameter(Mandatory = $true)]
        [string] $Name
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith('#')) {
            continue
        }
        if ($trimmed -match '^\s*' + [regex]::Escape($Name) + '\s*=\s*(.*)\s*$') {
            return $matches[1]
        }
    }
    return $null
}

function Read-JsonValue {
  param(
    [Parameter(Mandatory = $true)]
    [string] $Path,
    [Parameter(Mandatory = $true)]
    [string] $Name
  )

  $raw = Get-Content -LiteralPath $Path -Raw -Encoding UTF8
  if ([string]::IsNullOrWhiteSpace($raw)) {
    return $null
  }
  $json = $raw | ConvertFrom-Json
  $value = $json.$Name
  if ($null -eq $value) {
    return $null
  }
  return $value.ToString()
}

function Test-CommandExists {
  param([string] $Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Get-PowerShellCommand {
  if (Test-CommandExists "powershell.exe") {
    return "powershell.exe"
  }
  if (Test-CommandExists "pwsh") {
    return "pwsh"
  }
  return ""
}

$profileApiBaseUrl = Read-JsonValue -Path $profileFile -Name "API_BASE_URL"

$localEnvApiBaseUrl = $null
if ($Environment -eq "local") {
    $localEnvApiBaseUrl = Read-EnvValue -Path $mobileEnv -Name "API_BASE_URL"
}

$resolvedApiBaseUrl = if (-not [string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $ApiBaseUrl
} elseif (-not [string]::IsNullOrWhiteSpace($localEnvApiBaseUrl)) {
    $localEnvApiBaseUrl
} else {
    $profileApiBaseUrl
}

if ([string]::IsNullOrWhiteSpace($resolvedApiBaseUrl)) {
    throw "API_BASE_URL is empty. Set it in $profileFile or pass -ApiBaseUrl."
}

$resolvedKakaoKey = if ($KakaoNativeAppKey) {
    $KakaoNativeAppKey
} else {
    $mobileKakao = Read-EnvValue -Path $mobileEnv -Name "KAKAO_NATIVE_APP_KEY"
    if (-not [string]::IsNullOrWhiteSpace($mobileKakao)) {
        $mobileKakao
    } else {
        Read-EnvValue -Path $repoEnv -Name "KAKAO_NATIVE_APP_KEY"
    }
}

if ([string]::IsNullOrWhiteSpace($resolvedKakaoKey)) {
    throw "KAKAO_NATIVE_APP_KEY is required. Set it in apps/mobile/.env, .env, or pass -KakaoNativeAppKey."
}

$powerShellCommand = Get-PowerShellCommand
if ([string]::IsNullOrWhiteSpace($powerShellCommand)) {
    throw "missing powershell command (powershell.exe/pwsh)"
}

$dartDefines = @(
    "--dart-define-from-file=" + $profileFile
    "--dart-define=KAKAO_NATIVE_APP_KEY=" + $resolvedKakaoKey.Trim()
)

if ($resolvedApiBaseUrl.Trim() -ne $profileApiBaseUrl) {
    $dartDefines += "--dart-define=API_BASE_URL=" + $resolvedApiBaseUrl.Trim()
}

Push-Location $mobileDir
try {
    Write-Host ("[run-mobile-emulator] environment={0} api={1}" -f $Environment, $resolvedApiBaseUrl.Trim())
    & $powerShellCommand -NoProfile -ExecutionPolicy Bypass -File $flutterWrapper run -d $DeviceId @dartDefines
} finally {
    Pop-Location
}
