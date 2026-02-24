---
name: mobile-store-release-runner
description: Execute and verify mobile store release readiness cycles for Eunhye Hymn. Use when Codex runs mobile store preflight, workflow dispatch, run watch, and release log updates for Android/iOS release paths.
---

# Mobile Store Release Runner

Use this skill to run mobile release readiness cycles with repeatable evidence.

## Execute workflow

1. Collect target (`android`, `ios`, `both`) and distribution modes.
2. Run preflight with `scripts/mobile-store-preflight.ps1`.
3. Run full cycle with `scripts/mobile-store-cycle.ps1`.
4. Capture workflow run IDs and conclusions.
5. Ensure `docs/mobile-store-release-log.md` reflects each run.

Read `references/commands.md` for command templates.

## Enforce release-readiness rules

- Block on preflight failure.
- Treat missing required secrets as blocking.
- Keep `build_only` as default safe path unless upload is explicitly requested.
- Record `ABORTED` entries when cycle stops before dispatch.

## Sync documents when changed

- `docs/mobile/README.md`
- `apps/mobile/README.md`
- `docs/mobile-store-release-log.md`
- `docs/changelog-dev.md`

## Return output

Always return:

- target/mode used
- preflight status
- run ID and URL
- final cycle result
- updated log/doc files
