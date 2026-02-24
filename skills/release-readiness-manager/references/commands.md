# Release Readiness Commands

## Release preflight

```powershell
.\scripts\release-preflight.ps1 -Version 1.2.3 -Repo V4N1LLA/Eunhye_Hymn -Branch staging
```

## Staging gate check

```powershell
.\scripts\staging-latest-status.ps1 -Repo V4N1LLA/Eunhye_Hymn -Branch staging -RequireSuccess -RequireDeploySuccess -RequireVerifySuccess -MaxAgeMinutes 1440 -AsJson
```

## Optional workflow run inspection

```powershell
gh run list --repo V4N1LLA/Eunhye_Hymn --workflow release-readiness.yml --limit 10 --json databaseId,status,conclusion,url,headSha
```

## Tag and push

```powershell
git checkout production
git pull origin production
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
```

## Required docs

- `docs/release-management.md`
- `docs/changelog-dev.md`
