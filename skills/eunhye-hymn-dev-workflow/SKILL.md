---
name: eunhye-hymn-dev-workflow
description: Workstream guide for the Eunhye Hymn monorepo. Use when Codex is changing or reviewing code/docs in apps/api, apps/admin, apps/mobile, docs, scripts, or infra; especially for local setup, API/admin/mobile feature work, CI/CD or staging operations, and required document synchronization.
---

# Eunhye Hymn Dev Workflow

Use this skill to execute repository work with minimal context loading and consistent validation.

## Execute core flow

1. Classify the request into one primary area: `api`, `admin`, `mobile`, `ops`, or `docs`.
2. Read `AGENTS.md` and `README.md`, then load only the area-specific files listed in `references/doc-routing.md`.
3. Apply the smallest change that satisfies the request while preserving:
   - API base path `/api/v1`
   - Clean Architecture dependency direction in `apps/api`
4. Run focused checks from `references/validation-matrix.md` for touched areas.
5. Sync required docs when behavior/contracts/operations changed.
6. Return a concise report with changed files, commands run, and any skipped checks.

## Keep document updates consistent

Update docs in the same change when applicable:

- API contract or request/response changes: `docs/api-contract.md`
- DB schema/entity changes: `docs/data-model.md`
- Local setup/command changes: `README.md`, `docs/LOCAL_SETUP.md`, `docs/dev-guide.md`
- Staging/release/ops changes: `docs/runbook.md`, `docs/monorepo-cicd.md`, related staging logs/checklists
- Work-cycle policy changes: `docs/WORK_CYCLE.md`, `CLAUDE.md`

## Apply execution defaults

- Prefer wrappers and repo-standard commands:
  - API: `apps/api\\gradlew.bat`
  - Mobile: `scripts\\flutterw.ps1`
- Prefer narrow verification before broad verification.
- Avoid broad refactors unless requested.
- Treat `docs/api-contract.md` and `docs/data-model.md` as source-of-truth for API/data behavior.
