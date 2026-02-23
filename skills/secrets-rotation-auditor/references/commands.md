# Secrets Rotation Commands

## Core audit (raw)

```powershell
.\scripts\staging-secret-rotation-audit.ps1 -Repo V4N1LLA/Eunhye_Hymn -MaxAgeDays 90
```

## Include mobile release secrets

```powershell
.\scripts\staging-secret-rotation-audit.ps1 -Repo V4N1LLA/Eunhye_Hymn -MaxAgeDays 90 -IncludeMobileReleaseSecrets
```

## Optional secret sync helper

```powershell
.\scripts\staging-sync-secrets.ps1 -Repo V4N1LLA/Eunhye_Hymn
```

Use only when explicit sync/update is requested.

## Rotation cycle (recommended)

```powershell
.\scripts\secrets-rotation-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Owner <operator> -MaxAgeDays 90
```

Include mobile release scope:

```powershell
.\scripts\secrets-rotation-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Owner <operator> -MaxAgeDays 90 -IncludeMobileReleaseSecrets
```

JSON output for automation/debugging:

```powershell
.\scripts\secrets-rotation-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Owner <operator> -MaxAgeDays 90 -AsJson
```

## Documentation targets

- `docs/SECRETS_MANAGEMENT.md`
- `docs/runbook.md`
- `docs/secrets-rotation-log.md`
- `docs/changelog-dev.md`
