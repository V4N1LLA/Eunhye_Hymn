---
name: docs-sync-enforcer
description: Enforce document synchronization for Eunhye Hymn changes. Use when code, scripts, workflows, or operations are modified and Codex must update matching docs (README, contracts, runbook, changelog, CLAUDE, WORK_CYCLE) in the same cycle.
---

# Docs Sync Enforcer

Use this skill to prevent code-doc drift during implementation cycles.

## Execute workflow

1. Classify changed files by area (`api`, `admin`, `mobile`, `ops`, `ci`, `docs`).
2. Load required target docs from `references/sync-matrix.md`.
3. Update docs in the same change when behavior, commands, or contracts changed.
4. Run conflict and stale-marker checks from `references/checks.md`.
5. Summarize synced docs and any intentionally deferred updates.

## Enforce hard rules

- Keep `docs/api-contract.md` as API source of truth.
- Keep `docs/data-model.md` as schema source of truth.
- Update `docs/changelog-dev.md` and `CLAUDE.md` for meaningful cycle outcomes.
- Scan for merge markers before finishing.

## Return output

Always return:

- changed code paths
- docs updated
- verification commands and results
- deferred docs with reason
