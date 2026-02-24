param(
    [ValidateSet("local", "staging", "release")]
    [string] $Environment = "local",
    [ValidateSet("debug", "release")]
    [string] $BuildMode = "debug",
    [string] $ApiBaseUrl,
    [string] $KakaoNativeAppKey,
    [switch] $Install,
    [string] $DeviceId = "emulator-5554"
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$mobileDir = Join-Path $repoRoot "apps/mobile"
$mobileEnv = Join-Path $mobileDir ".env"
$repoEnv = Join-Path $repoRoot ".env"
$profileFile = Join-Path $mobileDir ("env/{0}.json" -f $Environment)

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

function Resolve-AdbPath {
    $fromCommand = Get-Command "adb" -ErrorAction SilentlyContinue
    if ($fromCommand) {
        return $fromCommand.Source
    }

    $candidates = @(
        [Environment]::GetEnvironmentVariable("LOCALAPPDATA", "User"),
        $env:LOCALAPPDATA,
        (Join-Path $env:USERPROFILE "AppData/Local")
    ) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique

    foreach ($base in $candidates) {
        $candidate = Join-Path $base "Android/Sdk/platform-tools/adb.exe"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    return $null
}

$profileApiBaseUrl = Read-JsonValue -Path $profileFile -Name "API_BASE_URL"
$resolvedApiBaseUrl = if (-not [string]::IsNullOrWhiteSpace($ApiBaseUrl)) {
    $ApiBaseUrl
} else {
    $profileApiBaseUrl
}

if ([string]::IsNullOrWhiteSpace($resolvedApiBaseUrl)) {
    throw "API_BASE_URL is empty. Set it in $profileFile or pass -ApiBaseUrl."
}

if ($Environment -eq "release" -and
    -not $ApiBaseUrl -and
    $resolvedApiBaseUrl.Trim().ToLowerInvariant() -eq "https://example.com/api/v1") {
    throw "Release API URL is placeholder. Pass -ApiBaseUrl for release build."
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
    "--dart-define-from-file=" + $profileFile
    "--dart-define=KAKAO_NATIVE_APP_KEY=" + $resolvedKakaoKey.Trim()
)

if ($resolvedApiBaseUrl.Trim() -ne $profileApiBaseUrl) {
    $dartDefines += "--dart-define=API_BASE_URL=" + $resolvedApiBaseUrl.Trim()
}

$buildArgs = @("build", "apk")
if ($BuildMode -eq "release") {
    $buildArgs += "--release"
} else {
    $buildArgs += "--debug"
}
$buildArgs += $dartDefines

Push-Location $mobileDir
try {
    Write-Host ("[build-mobile-apk] environment={0} mode={1} api={2}" -f $Environment, $BuildMode, $resolvedApiBaseUrl.Trim())
    & $flutter @buildArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Flutter build failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$apkPath = if ($BuildMode -eq "release") {
    Join-Path $mobileDir "build/app/outputs/flutter-apk/app-release.apk"
} else {
    Join-Path $mobileDir "build/app/outputs/flutter-apk/app-debug.apk"
}

if (-not (Test-Path -LiteralPath $apkPath)) {
    throw "APK not found: $apkPath"
}

if ($Install) {
    $adb = Resolve-AdbPath
    if (-not $adb) {
        throw "adb not found. Install Android SDK platform-tools or add adb to PATH."
    }
    & $adb -s $DeviceId install -r $apkPath
    if ($LASTEXITCODE -ne 0) {
        throw "adb install failed with exit code $LASTEXITCODE"
    }
}

Write-Host ("APK_READY={0}" -f $apkPath)
