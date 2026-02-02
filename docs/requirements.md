# Eunhye Hymn MVP Requirements

## 1. Purpose
- Provide an internal church platform to manage hymn content, scores, and part-practice audio.
- Support administrators with content curation and member access management.
- Enable members to discover hymns and practice parts with PDFs/MP3s.

## 2. Scope (MVP)
### 2.1 In Scope
- User authentication via:
  - Invite code (required for first-time access).
  - Google login.
  - Kakao login.
  - JWT access/refresh token flow.
- Hymn content management:
  - Create, update, archive hymns.
  - Manage hymn metadata (title, number, key, tempo, tags).
- Media delivery:
  - PDF score files served from S3.
  - MP3 part-practice audio served from S3.
- Search and browse:
  - Text search (title, number).
  - Filter by tag, key, tempo, and service season.
- Admin portal:
  - Manage hymns and media links.
  - Manage invites and user roles.
- Basic analytics:
  - View counts for hymns.

### 2.2 Out of Scope (Future)
- MIDI automation for part extraction.
- In-app audio processing or mixing.
- Public access or anonymous users.

## 3. Personas
- **Admin**: Curates hymns, uploads media, manages users and invite codes.
- **Member**: Searches hymns, views scores, listens to part audio.

## 4. Functional Requirements
### 4.1 Authentication & Authorization
- Invite code must be validated before social login succeeds.
- Social login providers: Google, Kakao.
- Access token (short-lived) + refresh token (long-lived, revocable).
- Roles: `admin`, `member`.

### 4.2 Hymn Management
- Create hymn records with required fields:
  - Hymn number, title, key, tempo, tags, season.
- Attach media references:
  - Score PDF URL (S3).
  - Part audio URLs (S3, multiple parts allowed).
- Soft-delete/archiving support.

### 4.3 Search & Browse
- Search by title or number.
- Filter by tags, key, season, tempo.
- Paginated results.

### 4.4 Media Access
- Secure S3 access via signed URLs.
- Download/view score PDF.
- Stream or download MP3s.

### 4.5 Admin Functions
- Create and revoke invite codes.
- Assign roles to users.
- Audit recent changes.

## 5. Non-Functional Requirements
- Maintainable structure with clear boundaries (Clean Architecture).
- API-first design with explicit contracts.
- Logs and audit trail for admin actions.
- Basic monitoring hooks (health checks).
- Compatibility:
  - Web admin (React or similar).
  - Mobile (Flutter).

## 6. Success Criteria
- Admin can publish a hymn with valid PDF + MP3 links.
- Member can login with invite code and access hymns within 3 minutes.
- Search results return within 1 second for typical query sizes.

## 7. Risks & Assumptions
- Assumes S3 bucket exists and access is controlled by signed URLs.
- Assumes availability of Google/Kakao OAuth credentials.
- Data volume is modest (church-level scale).
