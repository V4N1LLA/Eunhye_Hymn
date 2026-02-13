param(
    [string]$Distro = "Ubuntu",
    [string]$DbName = "eunhye_hymn",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgres",
    [string]$JwtSecret = "dev-secret-key-change-in-production-min-32-chars!!",
    [string]$InviteCode = "dev-invite-code"
)

$ErrorActionPreference = "Stop"

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

& (Join-Path $scriptDir "start-wsl-postgres.ps1") -Distro $Distro -DbName $DbName -DbUser $DbUser -DbPassword $DbPassword

$env:DB_URL = "jdbc:postgresql://localhost:5432/$DbName"
$env:DB_USER = $DbUser
$env:DB_PASS = $DbPassword
$env:JWT_SECRET = $JwtSecret
$env:JWT_ACCESS_TTL_SECONDS = "3600"
$env:JWT_REFRESH_TTL_SECONDS = "604800"
$env:INVITE_CODE = $InviteCode

Set-Location (Join-Path $repoRoot "apps/api")
& .\gradlew.bat bootRun --no-daemon
