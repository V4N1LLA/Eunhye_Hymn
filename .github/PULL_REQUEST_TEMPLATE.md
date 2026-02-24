## Summary
- What changed?
- Why now?

## Scope
- [ ] API (`apps/api`)
- [ ] Admin (`apps/admin`)
- [ ] Mobile (`apps/mobile`)
- [ ] Infra/DevOps (`infra`, `.github/workflows`, `scripts`)
- [ ] Docs only

## Branch Flow
- Base branch:
  - [ ] `develop` (feature integration)
  - [ ] `staging` (release candidate validation)
  - [ ] `production` (release)

## Verification
- [ ] Local tests/build completed
- [ ] CI checks passed (`PR Gate` 포함)
- [ ] No secrets included in diff

## Release Impact
- SemVer impact:
  - [ ] PATCH
  - [ ] MINOR
  - [ ] MAJOR
  - [ ] N/A
- [ ] `CHANGELOG.md` update needed

## Parallel Work Notes
- Related terminal/worktree tasks:
- Potentially conflicting files discussed:
