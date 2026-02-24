# Validation Matrix

Run the smallest meaningful validation set for changed areas, then expand only if needed.

## API changes (`apps/api`)

- Targeted class:
  - `cd apps/api`
  - `.\gradlew.bat test --tests "*ClassName*" --no-daemon --stacktrace`
- Full API test suite:
  - `cd apps/api`
  - `.\gradlew.bat test --no-daemon --stacktrace`

## Admin changes (`apps/admin`)

- `cd apps/admin`
- `npm ci`
- `npx tsc --noEmit`
- `npm run build`

If `npm ci` is not appropriate for the current local state, use `npm install`.

## Mobile changes (`apps/mobile`)

- `cd apps/mobile`
- `..\..\scripts\flutterw.ps1 analyze`
- `..\..\scripts\flutterw.ps1 test`

## Cross-area changes

- Validate each touched area with its own matrix above.
- For API contract/schema changes, run API tests plus at least one affected client check (admin or mobile).

## Docs or script-only changes

- Conflict marker scan:
  - `rg -n "^(<<<<<<<|=======|>>>>>>>)+$" README.md docs CLAUDE.md`
- For staging script changes, run safe checks where available:
  - `powershell -NoProfile -File .\scripts\staging-preflight.ps1 -Repo <owner/repo> -SkipTerraformPlan`
  - `powershell -NoProfile -File .\scripts\staging-rehearsal.ps1 -Repo <owner/repo> -Ref <branch> -SkipPreflight -DryRun`

## Report format

- List commands run.
- List pass/fail per command.
- State any skipped checks with reason.
