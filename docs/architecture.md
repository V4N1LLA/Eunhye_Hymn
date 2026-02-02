# Eunhye Hymn Architecture

## 1. Goals
- Maintainable, testable services with strict boundaries.
- Clear API contracts to enable AI-assisted development.
- Support web admin and mobile clients from a single API.

## 2. System Overview
- **Client Apps**:
  - Admin Web (apps/admin)
  - Mobile App (apps/mobile)
- **API Service** (apps/api):
  - Authentication, hymn metadata, media access, invites.
- **Storage**:
  - Relational DB for metadata.
  - S3 for PDFs and MP3s.

## 3. Clean Architecture Boundaries
### 3.1 Layers
- **Domain (Entities + Value Objects)**
  - Core hymn/user/invite models.
  - No framework dependencies.
- **Use Cases (Application Services)**
  - Orchestrate business logic (create hymn, issue invite).
  - Depend on interfaces (ports) for persistence and external services.
- **Interface Adapters**
  - Controllers, presenters, DTOs, mappers.
  - Translate HTTP requests into use case calls.
- **Infrastructure**
  - Database implementation, S3 client, OAuth clients.

### 3.2 Dependency Rule
- Dependencies must point inward.
- Outer layers must not be referenced by inner layers.
- Cross-cutting concerns (logging, metrics) are provided via interfaces.

## 4. Bounded Contexts
- **Identity & Access**: invite codes, social login, JWT issuance.
- **Hymn Catalog**: hymn metadata, tags, and search.
- **Media Access**: S3 pre-signed URL generation.
- **Audit & Analytics**: basic events (view counts, admin changes).

## 5. API Contract First
- API request/response structures defined in `docs/api-contract.md`.
- Versioning via `/api/v1`.
- Additive changes only in minor versions.

## 6. Data Management
- Relational DB with indexed search fields.
- Use soft deletes for hymns and invites.
- Maintain audit tables for admin actions.

## 7. Security
- JWT access/refresh with rotation.
- Role-based authorization in use case layer.
- S3 access via short-lived signed URLs.

## 8. AI-Assisted Development Practices
- Keep documentation explicit and structured.
- Use stable interfaces and DTOs for predictable code generation.
- Maintain consistent naming for entities and endpoints.
