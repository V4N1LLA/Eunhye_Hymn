$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$flutterDir = Join-Path $repoRoot "tools/flutter"
$flutterBat = Join-Path $flutterDir "bin/flutter.bat"
$flutterHome = Join-Path $repoRoot ".flutter-home"
$gitConfigPath = Join-Path $repoRoot ".flutter-gitconfig"

function Ensure-Directory {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
    }
}

function Ensure-FlutterSdk {
    if (Test-Path -LiteralPath $flutterBat) {
        return
    }

    if (-not (Get-Command git -ErrorAction SilentlyContinue)) {
        throw "git is required to install Flutter SDK automatically."
    }

    Ensure-Directory (Split-Path -Parent $flutterDir)
    Write-Host "[flutterw] Installing Flutter stable SDK to $flutterDir ..."
    git clone --depth 1 -b stable https://github.com/flutter/flutter.git $flutterDir | Out-Null
}

function Ensure-GitSafeDirectory {
    $safeDir = ($flutterDir -replace "\\", "/")
    $block = @"
[safe]
    directory = $safeDir
"@

    if (-not (Test-Path -LiteralPath $gitConfigPath)) {
        $block | Set-Content -LiteralPath $gitConfigPath -Encoding UTF8
        return
    }

    $content = Get-Content -LiteralPath $gitConfigPath -Raw -Encoding UTF8
    if ($content -notmatch [Regex]::Escape("directory = $safeDir")) {
        Add-Content -LiteralPath $gitConfigPath -Value "`n$block" -Encoding UTF8
    }
}

Ensure-FlutterSdk

Ensure-Directory $flutterHome
Ensure-Directory (Join-Path $flutterHome "AppData")
Ensure-Directory (Join-Path $flutterHome "AppData/Roaming")
Ensure-Directory (Join-Path $flutterHome "AppData/Local")
Ensure-Directory (Join-Path $flutterHome ".pub-cache")
Ensure-Directory (Join-Path $flutterHome ".android")

Ensure-GitSafeDirectory

$env:HOME = $flutterHome
$env:USERPROFILE = $flutterHome
$env:APPDATA = Join-Path $flutterHome "AppData/Roaming"
$env:LOCALAPPDATA = Join-Path $flutterHome "AppData/Local"
$env:PUB_CACHE = Join-Path $flutterHome ".pub-cache"
$env:GIT_CONFIG_GLOBAL = $gitConfigPath

$flutterArgs = @($args)
if (-not $flutterArgs -or $flutterArgs.Count -eq 0) {
    $flutterArgs = @("--version")
}

& $flutterBat @flutterArgs
exit $LASTEXITCODE
