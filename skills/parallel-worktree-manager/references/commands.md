# Parallel Worktree Commands

## Create task worktree

```powershell
.\scripts\new-worktree-task.ps1 -TaskName "admin-user-filter" -BaseBranch develop
```

## Check active worktrees

```powershell
git worktree list
```

## Check branch list

```powershell
git branch --all
```

## Task board file

Register or update:

- `docs/parallel-task-board.md`

Include:

- owner
- task
- branch
- worktree path
- claimed paths
- status
- updated UTC timestamp
