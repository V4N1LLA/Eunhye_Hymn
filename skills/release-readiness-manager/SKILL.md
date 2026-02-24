---
name: release-readiness-manager
description: Manage SemVer release readiness and release execution for Eunhye Hymn. Use when Codex validates version tags, runs release preflight, checks staging deploy health, and prepares tag-based GitHub releases.
---

# Release Readiness Manager

Use this skill to prepare and execute release steps with consistent preflight gates.

## Execute workflow

1. Parse release target version (`MAJOR.MINOR.PATCH`).
2. Run release preflight and inspect failed checks first.
3. Confirm latest staging deploy and verify gates are healthy.
4. Confirm CI evidence for the staging head SHA.
5. Create/push annotated release tag only when all gates pass.
6. Record release decision and evidence in docs when required.

Read `references/commands.md` for command templates.

## Enforce release rules

- Reject invalid SemVer strings.
- Reject duplicate tags.
- Block release when staging gate is stale or failing.
- Use PR-driven branch flow (`develop -> staging -> production`).

## Sync documents when changed

- `docs/release-management.md`
- `docs/changelog-dev.md`
- `docs/current-usable-scope.md`
- `docs/deployment-readiness-audit.md`

## Return output

Always return:

- version checked
- gate status per check
- release decision (`READY`, `BLOCKED`)
- tag action taken or skipped
