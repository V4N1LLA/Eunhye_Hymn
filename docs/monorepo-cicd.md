# Monorepo CI/CD Overview

This repository uses path-aware CI and staged deployment workflows.

## PR validation

- Workflow: `.github/workflows/pr-gate.yml`
- Purpose: run only required checks for changed areas (`apps/api`, `apps/admin`, `apps/mobile`, workflows/scripts).
- Result: central gate status for merge decisions.

## App-specific CI

- API CI: `.github/workflows/api-ci.yml`
- Admin CI: `.github/workflows/admin-ci.yml`
- Mobile CI: `.github/workflows/mobile-ci.yml`
- Mobile release build check: `.github/workflows/mobile-release-check.yml`

## Staging deploy

- Workflow: `.github/workflows/deploy-staging.yml`
- Trigger: push to `develop` or manual `workflow_dispatch`.
- Verification: deployment check step and staging operation scripts.

## Release readiness

- Workflow: `.github/workflows/release-readiness.yml`
- Script: `scripts/release-preflight.ps1`
- Artifact: `release-preflight.json`

## Related docs

- Runbook: `docs/runbook.md`
- Current usable scope: `docs/current-usable-scope.md`
- Work cycle standard: `docs/WORK_CYCLE.md`
