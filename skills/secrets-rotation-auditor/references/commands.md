# Secrets Rotation Commands

## Core audit

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

## Documentation targets

- `docs/SECRETS_MANAGEMENT.md`
- `docs/runbook.md`
- `docs/changelog-dev.md`
