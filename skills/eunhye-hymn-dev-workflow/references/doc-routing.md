# Doc Routing

Load only what is needed for the current task.

## Always read first

- `AGENTS.md`
- `README.md`

## Area-based loading

### API backend (`apps/api`)

- `docs/api-contract.md`
- `docs/data-model.md`
- `docs/architecture.md`
- `docs/dev-guide.md`
- `docs/usecases/README.md` and relevant `docs/usecases/*.md`

### Admin web (`apps/admin`)

- `docs/admin/README.md`
- `docs/api-contract.md`
- `docs/current-usable-scope.md`

### Mobile app (`apps/mobile`)

- `docs/mobile/README.md`
- `apps/mobile/README.md`
- `docs/usecases/ai-hymn-recommendations.md` and other relevant `docs/usecases/*.md`

### Local environment and troubleshooting

- `docs/LOCAL_SETUP.md`
- `docs/TEAM_LOCAL_DEVELOPMENT.md`
- `infra/docker/README.md`
- `docs/SECRETS_MANAGEMENT.md`

### CI/CD, release, branch flow

- `docs/monorepo-cicd.md`
- `docs/release-management.md`
- `docs/parallel-pr-workflow.md`
- `docs/parallel-task-board.md`

### Staging and operations

- `docs/runbook.md`
- `docs/staging-smoke-checklist.md`
- `docs/staging-feedback-checklist.md`
- `docs/staging-rehearsal-log.md`
- `docs/staging-smoke-log.md`
- `docs/deployment-readiness-audit.md`
- `infra/aws/README.md`

### Work-cycle and documentation policy

- `docs/WORK_CYCLE.md`
- `CLAUDE.md`
- `docs/changelog-dev.md`

## Priority rules

- Treat `docs/api-contract.md` as API source-of-truth.
- Treat `docs/data-model.md` as schema/entity source-of-truth.
- If docs conflict, update stale documents in the same change.
