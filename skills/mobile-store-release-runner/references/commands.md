# Mobile Store Release Commands

## Android preflight (`build_only`)

```powershell
.\scripts\mobile-store-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -Target android -AndroidDistributionMode build_only
```

## iOS preflight (`testflight`)

```powershell
.\scripts\mobile-store-preflight.ps1 -Repo V4N1LLA/Eunhye_Hymn -Target ios -IosDistributionMode testflight
```

## Full cycle (recommended)

```powershell
.\scripts\mobile-store-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -Target android -AndroidDistributionMode build_only -IosDistributionMode build_only
```

## Dry run

```powershell
.\scripts\mobile-store-cycle.ps1 -Repo V4N1LLA/Eunhye_Hymn -Ref develop -Target both -DryRun
```

## Verify workflow runs

```powershell
gh run list --repo V4N1LLA/Eunhye_Hymn --workflow mobile-store-release.yml --limit 10 --json databaseId,status,conclusion,url,headSha
```

## Log file

- `docs/mobile-store-release-log.md`
