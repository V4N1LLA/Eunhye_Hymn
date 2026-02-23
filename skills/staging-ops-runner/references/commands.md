# Staging Ops Commands

## Preflight

```powershell
.\scripts\staging-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -AutoLogin
```

Use `-AwsProfile <profile>` when needed.

## Latest deploy gate

```powershell
.\scripts\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 120 -AsJson
```

Use `-AsMarkdown` for log-ready table output.

## Full ops cycle (recommended)

```powershell
.\scripts\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner <owner> -AutoLogin -WaitForCompletion
```

## Rehearsal dispatch

```powershell
.\scripts\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -AutoLogin
```

Dry-run:

```powershell
.\scripts\staging-rehearsal.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -SkipPreflight -DryRun
```

## Log files

- `docs/staging-smoke-log.md`
- `docs/staging-rehearsal-log.md`
