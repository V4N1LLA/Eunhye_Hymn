param(
    [string]$Distro = "Ubuntu",
    [string]$DbName = "eunhye_hymn",
    [string]$DbUser = "postgres",
    [string]$DbPassword = "postgres"
)

$ErrorActionPreference = "Stop"

function Invoke-Wsl {
    param([string[]]$CmdArgs)
    & wsl -d $Distro -- @CmdArgs
    if ($LASTEXITCODE -ne 0) {
        throw "WSL command failed: $($CmdArgs -join ' ')"
    }
}

Invoke-Wsl @("service", "postgresql", "start")
Invoke-Wsl @("sudo", "-u", "postgres", "psql", "-c", "ALTER USER $DbUser WITH PASSWORD '$DbPassword';")

$dbExists = (& wsl -d $Distro -- sudo -u postgres psql -tAc "SELECT 1 FROM pg_database WHERE datname='$DbName'").Trim()
if ($LASTEXITCODE -ne 0) {
    throw "Failed to query database list in WSL."
}

if ($dbExists -ne "1") {
    Invoke-Wsl @("sudo", "-u", "postgres", "createdb", "-O", $DbUser, $DbName)
}

Write-Host "[wsl-postgres] ready: jdbc:postgresql://localhost:5432/$DbName"
