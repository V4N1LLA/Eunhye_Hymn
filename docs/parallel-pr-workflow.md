# Parallel Terminal PR Workflow

## 목적
- 여러 터미널에서 동시에 작업해도 충돌/실수를 줄인다.
- 모든 변경을 PR 단위로 추적 가능하게 유지한다.

## 핵심 원칙
1. 터미널 하나당 작업 브랜치 하나
2. 같은 브랜치를 여러 터미널에서 동시에 수정하지 않음
3. 직접 push 금지 브랜치(`develop`, `staging`, `production`)로는 PR만 사용
4. PR 생성 전 로컬 검증 + CI 통과 확인

## 권장 방식: git worktree
같은 저장소를 여러 폴더로 분리해 병렬 작업한다.

```powershell
.\scripts\new-worktree-task.ps1 -TaskName "admin-user-filter" -BaseBranch develop
```

생성 후:
1. 각 터미널은 자신의 worktree 경로에서만 작업
2. 브랜치 이름은 task 기준으로 유지 (`feat/*`, `fix/*`, `chore/*`)

## 병렬 작업 체크리스트
- 작업 시작 시 `docs/parallel-task-board.md`에 본인 작업/브랜치/경로 등록
- 같은 파일을 동시에 건드릴 가능성이 있으면 먼저 담당자 합의
- 공용 파일(`README.md`, `.github/workflows/*`, `docs/*`) 변경 시 PR 설명에 명시

## PR 흐름 (필수)
1. `feature/*` -> `develop` PR
2. 릴리즈 후보 시 `develop` -> `staging` PR
3. 스테이징 검증 완료 후 `staging` -> `production` PR

## PR 전 최소 검증
- 변경 영역별 로컬 테스트 통과
- `PR Gate` 워크플로우 성공 확인
- 릴리즈 관련 변경이면 `Release Readiness` 결과 첨부

## 충돌 발생 시
1. 먼저 `git fetch origin`
2. 대상 브랜치 리베이스/머지 후 충돌 해결
3. 충돌 파일이 공용 정책 파일이면 관련 터미널 담당자와 먼저 합의

## Collision Guard Automation (2026-02-24)
- `scripts/new-worktree-task.ps1` now validates ownership before creating a worktree.
- Recommended command:

```powershell
.\scripts\new-worktree-task.ps1 `
  -TaskName "admin-user-filter" `
  -BaseBranch develop `
  -Owner codex `
  -ClaimedPaths "apps/admin/src/pages/UserListPage.tsx","docs/runbook.md"
```

- Guard rules enforced:
  - block local/remote branch name collisions
  - block task-board worktree path collisions
  - block claimed-path overlap with active rows from other owners
- On success, the script auto-upserts `docs/parallel-task-board.md` with `status=in_progress` and current UTC.
- Use `-AllowClaimedPathConflict` only for explicit, coordinated exceptions.
