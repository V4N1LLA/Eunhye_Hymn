# Release Management (SemVer)

This document defines the lightweight release flow used in this repository.

## 1. Versioning rule

- Follow `MAJOR.MINOR.PATCH` format.
- Create tags with `v` prefix (example: `v1.2.3`).

## 2. Preflight check

Run the release preflight before creating a tag:

```powershell
./scripts/release-preflight.ps1 -Version 1.2.3 -Repo V4N1LLA/Eunhye_Hymn -Branch staging
```

- `READY`: proceed to tagging.
- `HOLD`: resolve failed checks first.

## 3. Readiness workflow

You can also run the GitHub workflow manually:

- Workflow: `.github/workflows/release-readiness.yml`
- Output artifact: `release-preflight.json`

## 4. Tagging

When preflight is ready:

```bash
git tag -a v1.2.3 -m "Release v1.2.3"
git push origin v1.2.3
```

## 5. Related docs

- Deployment runbook: `docs/runbook.md`
- Deployment readiness audit: `docs/deployment-readiness-audit.md`
- Development changelog: `docs/changelog-dev.md`
