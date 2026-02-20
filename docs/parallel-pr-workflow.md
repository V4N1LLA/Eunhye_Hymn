# Parallel PR Workflow

Use this flow when multiple tasks must progress at the same time.

## 1. Branch split

- Start each task from `develop`.
- Create one branch per feature/fix (`feat/*`, `fix/*`, `docs/*`).
- Keep changes scoped to one PR topic.

## 2. Local execution

- Option A: single workspace + frequent branch switching.
- Option B: multiple worktrees for true parallel editing/testing.

Example:

```bash
git worktree add ../repo-pr-a -b feat/pr-a origin/develop
git worktree add ../repo-pr-b -b feat/pr-b origin/develop
```

## 3. Validation per PR

- Run only relevant tests/build checks per branch.
- Update impacted docs together with code.
- Ensure no conflict markers remain.

## 4. Review handling

- Apply review comments on the same PR branch.
- Re-run validations after each significant fix.
- Reply to each review thread with what changed.

## 5. Merge order

- Merge low-risk/base changes first.
- Rebase or merge `develop` into remaining branches when needed.
- Confirm CI is green before each merge.

## Related docs

- Work cycle standard: `docs/WORK_CYCLE.md`
- Dev guide: `docs/dev-guide.md`
