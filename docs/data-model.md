# Eunhye Hymn 데이터 모델 (MVP)

## 1. 테이블
### 1.1 users
- `id` (UUID, PK)
- `display_name` (varchar)
- `role` (varchar, USER/ADMIN)
- `status` (varchar, ACTIVE/DISABLED)
- `created_at` (timestamp)
- `last_login_at` (timestamp, nullable)

### 1.2 auth_identities
- `id` (UUID, PK)
- `user_id` (UUID, FK -> users.id)
- `provider` (varchar, GOOGLE/KAKAO)
- `provider_subject` (varchar)
- `email` (varchar, nullable)
- `created_at` (timestamp)
- UNIQUE(`provider`, `provider_subject`)

### 1.3 refresh_tokens
- `id` (UUID, PK)
- `user_id` (UUID, FK -> users.id)
- `token_hash` (varchar)
- `expires_at` (timestamp)
- `revoked_at` (timestamp, nullable)
- `created_at` (timestamp)
- INDEX(`user_id`)

### 1.4 hymns
- `id` (UUID, PK)
- `title` (varchar)
- `number` (varchar, nullable)
- `tags` (varchar, nullable, 콤마 구분)
- `enabled` (boolean)
- `created_at` (timestamp)

### 1.5 assets
- `id` (UUID, PK)
- `hymn_id` (UUID, FK -> hymns.id)
- `type` (varchar, PDF/AUDIO)
- `part` (varchar, nullable, S/A/T/B/ALL)
- `url` (varchar)
- `checksum` (varchar, nullable)
- `version` (varchar, nullable)
- `created_at` (timestamp)
- INDEX(`hymn_id`)

### 1.6 hymn_notes
- `id` (UUID, PK)
- `user_id` (UUID, FK -> users.id)
- `hymn_id` (UUID, FK -> hymns.id)
- `content` (text)
- `updated_at` (timestamp)
- UNIQUE(`user_id`, `hymn_id`)

### 1.7 user_hymn_state
- `user_id` (UUID, FK -> users.id)
- `hymn_id` (UUID, FK -> hymns.id)
- `favorite` (boolean)
- `last_opened_at` (timestamp, nullable)
- `last_part_played` (varchar, nullable)
- `last_play_position_ms` (bigint, nullable)
- PRIMARY KEY(`user_id`, `hymn_id`)
- INDEX(`user_id`, `last_opened_at`)

### 1.8 events
- `id` (UUID, PK)
- `user_id` (UUID, FK -> users.id)
- `event_type` (varchar, HYMN_OPENED/PART_PLAYED/NOTE_SAVED/FAVORITE_TOGGLED)
- `hymn_id` (UUID, nullable)
- `part` (varchar, nullable)
- `metadata_json` (text, nullable)
- `created_at` (timestamp)
- INDEX(`user_id`, `created_at`)

## 2. 메모
- MVP에서는 태그를 콤마 구분 문자열로 저장한다.
- JSON은 MVP에서 문자열로 저장하고, 확장 시 JSON 컬럼으로 변경한다.
