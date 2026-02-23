---
name: secrets-rotation-auditor
description: Audit and drive secret rotation hygiene for Eunhye Hymn staging/release operations. Use when Codex checks stale or missing GitHub Actions secrets, runs secret audit scripts, and updates remediation logs or security docs.
---

# Secrets Rotation Auditor

Use this skill to perform periodic secret audit cycles and track follow-up actions.

## Execute workflow

1. Run secret rotation audit script for core staging secrets.
2. Include mobile release secrets when requested.
3. Classify findings into `PASS`, `STALE`, `MISSING`.
4. Run `secrets-rotation-cycle.ps1` for repeatable logging and decision tracking.
5. Propose or execute secret sync steps when policy allows.
6. Record remediation outcomes in docs.

Read `references/commands.md` for command templates.

## Enforce security policy

- Never print or store raw secret values.
- Record only status and age evidence.
- Treat `MISSING` as blocking for related workflows.
- Treat `STALE` as rotate-now when above policy threshold.

## Sync documents when changed

- `docs/SECRETS_MANAGEMENT.md`
- `docs/runbook.md`
- `infra/aws/README.md`
- `docs/secrets-rotation-log.md`
- `docs/changelog-dev.md`

## Return output

Always return:

- audit scope
- stale/missing summary
- remediation actions performed or pending
- docs updated
