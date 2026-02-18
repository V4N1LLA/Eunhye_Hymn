param(
    [string] $DeviceId = "emulator-5554",
    [string] $ApiBaseUrl,
    [string] $KakaoNativeAppKey
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$mobileDir = Join-Path $repoRoot "apps/mobile"
$mobileEnv = Join-Path $mobileDir ".env"
$repoEnv = Join-Path $repoRoot ".env"
$preferredFlutter = Join-Path $repoRoot "tools/flutter/bin/flutter.bat"
$flutter = Get-Command "flutter" -ErrorAction SilentlyContinue
if ($flutter) {
    $flutter = $flutter.Source
}
if (-not $flutter) {
    $flutter = $preferredFlutter
}

if (-not (Test-Path -LiteralPath $flutter)) {
    throw "Flutter executable not found: $flutter"
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

$resolvedApiBaseUrl = if ($ApiBaseUrl) {
    $ApiBaseUrl
} else {
    $valueFromEnv = Read-EnvValue -Path $mobileEnv -Name "API_BASE_URL"
    if ([string]::IsNullOrWhiteSpace($valueFromEnv)) {
        "http://10.0.2.2:8080/api/v1"
    } else {
        $valueFromEnv
    }
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

$dartDefines = @(
    "--dart-define=API_BASE_URL=" + ($resolvedApiBaseUrl.Trim())
    "--dart-define=KAKAO_NATIVE_APP_KEY=" + $resolvedKakaoKey.Trim()
)

Push-Location $mobileDir
try {
    & $flutter run -d $DeviceId @dartDefines
} finally {
    Pop-Location
}
