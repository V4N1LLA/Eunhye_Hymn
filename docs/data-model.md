# Eunhye Hymn Data Model (MVP)

## 1. Tables
### 1.1 users
- `id` (PK, UUID)
- `email` (unique, nullable for Kakao if not provided)
- `name`
- `role` (enum: admin, member)
- `provider` (enum: google, kakao)
- `provider_user_id`
- `created_at`, `updated_at`

### 1.2 invites
- `code` (PK, string)
- `max_uses`
- `use_count`
- `expires_at`
- `revoked` (bool)
- `created_by` (FK -> users.id)
- `created_at`, `updated_at`

### 1.3 hymns
- `id` (PK, UUID)
- `number` (int)
- `title` (string)
- `key` (string)
- `tempo` (int)
- `season` (string)
- `tags` (string[])
- `score_pdf_key` (string)
- `archived` (bool)
- `created_at`, `updated_at`

### 1.4 hymn_parts
- `id` (PK, UUID)
- `hymn_id` (FK -> hymns.id)
- `part` (enum: soprano, alto, tenor, bass, piano, etc.)
- `audio_key` (string)
- `created_at`, `updated_at`

### 1.5 refresh_tokens
- `id` (PK, UUID)
- `user_id` (FK -> users.id)
- `token_hash` (string)
- `expires_at`
- `revoked` (bool)
- `created_at`, `updated_at`

### 1.6 hymn_views
- `id` (PK, UUID)
- `hymn_id` (FK -> hymns.id)
- `user_id` (FK -> users.id)
- `viewed_at`

### 1.7 admin_audit_log
- `id` (PK, UUID)
- `actor_id` (FK -> users.id)
- `action` (string)
- `entity_type` (string)
- `entity_id` (string)
- `metadata` (json)
- `created_at`

## 2. Indexes
- `users(provider, provider_user_id)` unique
- `users(email)` unique (nullable)
- `invites(expires_at)`
- `hymns(number)`
- `hymns(title)`
- `hymns(tags)` (GIN index if supported)
- `hymn_parts(hymn_id)`
- `refresh_tokens(user_id)`
- `hymn_views(hymn_id, viewed_at)`
- `admin_audit_log(actor_id, created_at)`

## 3. Notes
- Use soft delete via `archived` on hymns and `revoked` on invites/tokens.
- `score_pdf_key` and `audio_key` map to S3 object keys.
