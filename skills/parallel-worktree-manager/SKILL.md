---
name: parallel-worktree-manager
description: Manage parallel terminal task setup for Eunhye Hymn. Use when Codex should create isolated worktrees/branches, register task ownership, prevent branch or path collisions, and prepare PR-safe parallel execution.
---

# Parallel Worktree Manager

Use this skill to start and control parallel feature cycles safely.

## Execute workflow

1. Collect task name, base branch, and owner.
2. Create worktree/branch with `scripts/new-worktree-task.ps1`.
3. Register ownership row in `docs/parallel-task-board.md`.
4. Confirm no active collision on claimed paths.
5. Share worktree path, branch name, and next commands.

Read `references/commands.md` for templates.

## Enforce parallel rules

- Use one branch per terminal.
- Avoid multi-terminal edits on the same branch.
- Highlight shared-file risk for `README.md`, `docs/*`, and workflow files.
- Keep PR scope limited to one task unit.

## Return output

Always return:

- created branch/worktree path
- task board row added or updated
- conflict warnings (if any)
- recommended next validation commands
