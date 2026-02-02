# Eunhye Hymn Monorepo

Internal church application suite for managing hymn content, scores, and part-practice media.
This monorepo is structured to support multiple clients (web admin, mobile), a single API,
shared infrastructure, and documentation designed for maintainability and AI-assisted
development.

## Repository Structure
- `apps/`
  - `apps/api`: Backend API service (no implementation yet)
  - `apps/admin`: Admin web app (no implementation yet)
  - `apps/mobile`: Mobile app (Flutter) (no implementation yet)
- `infra/`
  - `infra/docker`: Container and local dev orchestration
  - `infra/aws`: AWS infrastructure definitions
- `docs/`
  - `docs/requirements.md`: MVP requirements
  - `docs/architecture.md`: Architecture and Clean Architecture boundaries
  - `docs/api-contract.md`: API contract definitions
  - `docs/data-model.md`: Database schema and indexes
  - `docs/events.md`: Event taxonomy and metadata schema
  - `docs/dev-guide.md`: Local development guide (placeholder)
  - `docs/runbook.md`: Staging runbook (placeholder)
  - `docs/prompts`: AI prompt templates
  - `docs/usecases`: Use case write-ups
  - `docs/admin`: Admin-specific documentation
  - `docs/mobile`: Mobile-specific documentation
- `.github/workflows`: CI/CD workflows

## Notes
- Internal church use only.
- Authentication: invite code + Google/Kakao login with JWT access/refresh tokens.
- Media hosting: PDF scores and MP3 part-practice audio served from S3.

---

Suggested atomic commit message:
- `chore: finalize monorepo structure and placeholders`
