# Eunhye Hymn 데이터 모델 (MVP)

## 1. 테이블
### 1.1 users
- `id` (PK, UUID)
- `email` (unique, Kakao는 nullable 가능)
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

## 2. 인덱스
- `users(provider, provider_user_id)` unique
- `users(email)` unique (nullable)
- `invites(expires_at)`
- `hymns(number)`
- `hymns(title)`
- `hymns(tags)` (지원 시 GIN 인덱스)
- `hymn_parts(hymn_id)`
- `refresh_tokens(user_id)`
- `hymn_views(hymn_id, viewed_at)`
- `admin_audit_log(actor_id, created_at)`

## 3. 메모
- 찬송가는 `archived`, 초대 코드는 `revoked`로 소프트 삭제.
- `score_pdf_key`, `audio_key`는 S3 객체 키를 의미.
