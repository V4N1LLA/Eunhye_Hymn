---
name: staging-ops-runner
description: Run and evaluate the Eunhye Hymn staging operations cycle. Use when Codex needs to execute or verify staging preflight, deploy status gates, rehearsal runs, smoke log updates, and Go/Hold decisions for staging.
---

# Staging Ops Runner

Use this skill to run staging checks in a consistent order and leave auditable evidence.

## Execute workflow

1. Confirm target repo/branch and operator.
2. Run preflight first unless user explicitly skips it.
3. Run deploy gate check with `staging-latest-status.ps1`.
4. Run full cycle with `staging-ops-cycle.ps1` when Go/Hold evidence is needed.
5. Run rehearsal with `staging-rehearsal.ps1` when dispatch verification is needed.
6. Run `ops-health-cycle.ps1` when consolidated ops health evidence is needed.
7. Record run IDs, conclusions, and decision in staging logs.

Read `references/commands.md` for exact command templates.

## Enforce decision policy

- Mark `HOLD` when preflight fails or deploy/verify gate fails.
- Mark `CONDITIONAL_GO` when automation passes but manual smoke is pending.
- Mark `GO` only after required manual smoke checks pass.

## Sync documents when changed

- `docs/staging-smoke-log.md`
- `docs/staging-rehearsal-log.md`
- `docs/ops-health-log.md`
- `docs/staging-smoke-checklist.md`
- `docs/runbook.md`
- `docs/changelog-dev.md`

## Return output

Always return:

- commands run
- preflight result
- deploy/verify gate result
- final decision (`GO`, `CONDITIONAL_GO`, `HOLD`)
- log/doc files updated
