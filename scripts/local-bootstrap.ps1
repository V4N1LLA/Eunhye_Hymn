param(
    [string] $ComposeFile = "infra/docker/docker-compose.yml",
    [string] $SummaryFile = ".tmp/local-smoke-summary.json",
    [string] $KakaoNativeAppKey = "",
    [string] $DeviceId = "",
    [switch] $SkipDockerUp,
    [switch] $SkipMobileEnv
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$envPath = Join-Path $repoRoot ".env"
$envExample = Join-Path $repoRoot ".env.example"
$mobileEnvPath = Join-Path $repoRoot "apps/mobile/.env"
$mobileEnvExample = Join-Path $repoRoot "apps/mobile/.env.example"
$localVerify = Join-Path $repoRoot "scripts/local-verify.ps1"
$runMobile = Join-Path $repoRoot "scripts/run-mobile-emulator.ps1"
$localSummaryPath = Join-Path $repoRoot $SummaryFile

function Set-EnvValue {
    param(
        [Parameter(Mandatory = $true)] [string] $Path,
        [Parameter(Mandatory = $true)] [string] $Key,
        [Parameter(Mandatory = $true)] [string] $Value
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    $lines = [System.Collections.Generic.List[string]]::new()
    $updated = $false
    $found = $false

    foreach ($line in Get-Content -LiteralPath $Path) {
        if ($line -match "^\s*$([regex]::Escape($Key))\s*=.*$") {
            if (-not $found) {
                $found = $true
                $lines.Add("$Key=$Value")
                $updated = $true
            }
        } else {
            $lines.Add($line)
        }
    }

    if (-not $found) {
        $lines.Add("$Key=$Value")
        $updated = $true
    }

    if ($updated) {
        Set-Content -LiteralPath $Path -Value $lines -Encoding utf8
    }
}

function Get-EnvValue {
    param(
        [Parameter(Mandatory = $true)] [string] $Path,
        [Parameter(Mandatory = $true)] [string] $Key
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }

    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($trimmed) -or $trimmed.StartsWith("#")) {
            continue
        }
        if ($trimmed -match '^\s*' + [regex]::Escape($Key) + '\s*=\s*(.*)\s*$') {
            return $matches[1]
        }
    }
    return $null
}

function Resolve-KakaoKey {
    $mobile = Get-EnvValue -Path $mobileEnvPath -Key "KAKAO_NATIVE_APP_KEY"
    if (-not [string]::IsNullOrWhiteSpace($mobile)) {
        return $mobile
    }

    if (-not [string]::IsNullOrWhiteSpace($KakaoNativeAppKey)) {
        return $KakaoNativeAppKey
    }

    $root = Get-EnvValue -Path $envPath -Key "KAKAO_NATIVE_APP_KEY"
    if (-not [string]::IsNullOrWhiteSpace($root)) {
        return $root
    }

    return $null
}

Write-Host "== Local bootstrap =="

if (-not (Test-Path -LiteralPath $envPath)) {
    if (-not (Test-Path -LiteralPath $envExample)) {
        throw ".env.example not found."
    }
    Copy-Item -Path $envExample -Destination $envPath
    Write-Host "[OK] Created .env from .env.example"
}

if (-not $SkipMobileEnv -and -not (Test-Path -LiteralPath $mobileEnvPath)) {
    if (Test-Path -LiteralPath $mobileEnvExample) {
        Copy-Item -Path $mobileEnvExample -Destination $mobileEnvPath
        Write-Host "[OK] Created apps/mobile/.env from apps/mobile/.env.example"
    } else {
        throw "apps/mobile/.env.example not found."
    }
}

$mobileApiBase = Get-EnvValue -Path $mobileEnvPath -Key "API_BASE_URL"
if ([string]::IsNullOrWhiteSpace($mobileApiBase)) {
    Set-EnvValue -Path $mobileEnvPath -Key "API_BASE_URL" -Value "http://10.0.2.2:8080/api/v1"
}

$resolvedKakaoKey = Resolve-KakaoKey
if (-not [string]::IsNullOrWhiteSpace($resolvedKakaoKey)) {
    Set-EnvValue -Path $mobileEnvPath -Key "KAKAO_NATIVE_APP_KEY" -Value $resolvedKakaoKey
    Set-EnvValue -Path $envPath -Key "KAKAO_NATIVE_APP_KEY" -Value $resolvedKakaoKey
} else {
    Write-Host "[WARN] KAKAO_NATIVE_APP_KEY is not set. Set it in .env, apps/mobile/.env, or with -KakaoNativeAppKey before running mobile."
}

Write-Host "[OK] Environment prepared"

Write-Host "`n== API & DB smoke check =="
& (Resolve-Path $localVerify) -ComposeFile $ComposeFile -ApiBaseUrl "http://localhost:8080/api/v1" -SummaryFile $SummaryFile -SkipDockerUp:$SkipDockerUp -KeepGeneratedFiles

if (Test-Path -LiteralPath $localSummaryPath) {
    try {
        $summary = Get-Content -LiteralPath $localSummaryPath -Raw | ConvertFrom-Json
        Write-Host "`n== Local test data =="
        Write-Host ("Invite   : {0}" -f $summary.inviteCode)
        Write-Host ("DB User  : {0}" -f $summary.dbUserId)
        Write-Host ("Hymn ID : {0}" -f $summary.hymnId)
        Write-Host ("Asset   : {0}" -f $summary.assetUrl)
        Write-Host ("Summary : {0}" -f $localSummaryPath)
        Write-Host ("Generated: {0}" -f $summary.generatedAt)
    } catch {
        Write-Host "[WARN] Could not parse local smoke summary JSON: $localSummaryPath"
    }
}

Write-Host "`n== Next step =="
Write-Host "Local API URL: http://localhost:8080/api/v1"
Write-Host "Admin URL: http://localhost:5173"
Write-Host "Mobile run command:"
Write-Host "  .\scripts\run-mobile-emulator.ps1 -DeviceId emulator-5554"

if (-not [string]::IsNullOrWhiteSpace($DeviceId)) {
    Write-Host "`nIf you want me to launch emulator now, run:"
    Write-Host "  .\scripts\run-mobile-emulator.ps1 -DeviceId $DeviceId"
}
