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

Record manual smoke result:

```powershell
.\scripts\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner <owner> -SkipPreflight -ManualSmokeResult PASS -ManualSmokeEvidence <evidence-url>
```

JSON output for automation/debugging:

```powershell
.\scripts\staging-ops-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner <owner> -SkipPreflight -AsJson
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
- `docs/ops-health-log.md`

## Consolidated health cycle

```powershell
.\scripts\ops-health-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch develop -Owner <owner> -AutoLogin -WaitForCompletion
```

This combines staging gates + secrets rotation checks and classifies failures.
