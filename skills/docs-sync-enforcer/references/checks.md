# Docs Consistency Checks

## Merge marker check

```powershell
rg -n "^(<<<<<<<|=======|>>>>>>>)+$" README.md docs CLAUDE.md
```

## Stale keyword scans (example)

```powershell
rg -n "TODO|TBD|placeholder|FIXME" docs README.md CLAUDE.md
```

## Required files touched check (example)

```powershell
git diff --name-only
```

Review whether changed areas from `references/sync-matrix.md` are reflected in doc updates.

## Reporting format

- list checks run
- list pass/fail per check
- list unresolved doc sync items
