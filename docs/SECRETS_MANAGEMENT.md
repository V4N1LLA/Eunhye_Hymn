# Secrets Management Guide

## 1. Principle
- Do not commit real secrets to Git, even in private repositories.
- Keep `.env` local-only and commit only templates (`.env.example`).
- Store runtime secrets in dedicated secret stores.

## 2. Where to Store Secrets
1. Local development
- Store values in `.env` (ignored by Git).
- Use non-production values only.

2. CI/CD (GitHub Actions)
- Use `Settings > Secrets and variables > Actions`.
- Split secrets by environment (`staging`, `production`).

3. Runtime (AWS)
- Use AWS Secrets Manager or SSM Parameter Store.
- Inject at deploy/runtime time, not at build time.

## 3. Rotation Policy
- Rotate immediately if exposure is suspected.
- Rotate on a fixed schedule for high-impact secrets:
  - `JWT_SECRET`
  - database passwords
  - cloud access keys

## 4. PR/Review Checklist
- No real keys/tokens/passwords in changed files.
- No secrets in logs, screenshots, PR body, or comments.
- `.env` files are not tracked by Git.
- `.env.example` contains placeholders or local-safe defaults only.

## 5. Incident Response (Secret Leak)
1. Revoke/rotate the leaked secret.
2. Audit access logs and token usage.
3. Remove leaked values from history if needed.
4. Record follow-up in `docs/changelog-dev.md`.

