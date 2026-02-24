# Eunhye Hymn 데이터 모델

기준: `apps/api/src/main/resources/db/migration` (`V1`~`V16`)

## 1. 핵심 테이블

### 1.1 `users`
- `id` (UUID, PK)
- `display_name` (varchar, NOT NULL)
- `role` (varchar, `USER`/`ADMIN`)
- `status` (varchar, `ACTIVE`/`DISABLED`)
- `created_at` (timestamp)
- `last_login_at` (timestamp, nullable)

### 1.2 `auth_identities`
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`)
- `provider` (varchar, `KAKAO` 등)
- `provider_subject` (varchar)
- `email` (varchar, nullable)
- `created_at` (timestamp)
- UNIQUE(`provider`, `provider_subject`)

### 1.3 `refresh_tokens`
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`)
- `token_hash` (varchar)
- `expires_at` (timestamp)
- `revoked_at` (timestamp, nullable)
- `created_at` (timestamp)
- INDEX(`user_id`)

### 1.4 `user_password_credentials`
- `user_id` (UUID, PK, FK -> `users.id`, ON DELETE CASCADE)
- `password_hash` (varchar)
- `created_at` (timestamp)
- `updated_at` (timestamp)

### 1.5 `admin_password_credentials`
- `id` (UUID, PK)
- `login_id` (varchar, NOT NULL)
- `password_hash` (varchar, NOT NULL)
- `created_at` (timestamp)
- `updated_at` (timestamp)

## 2. 찬양/에셋 테이블

### 2.1 `hymns`
- `id` (UUID, PK)
- `title` (varchar, NOT NULL)
- `number` (varchar, nullable)
- `tags` (varchar, nullable, 콤마 구분 문자열)
- `enabled` (boolean, NOT NULL)
- `created_at` (timestamp)

### 2.2 `assets`
- `id` (UUID, PK)
- `hymn_id` (UUID, FK -> `hymns.id`)
- `type` (varchar, `PNG`/`MIDI`)
- `part` (varchar, `S`/`A`/`T`/`B`/`ALL`, NOT NULL)
- `url` (varchar)
- `object_key` (varchar, NOT NULL)
- `checksum` (varchar, nullable)
- `version` (varchar, nullable)
- `created_at` (timestamp)
- INDEX(`hymn_id`)

## 3. 개인화/인증 상태 테이블

### 3.1 `user_profiles`
- `user_id` (UUID, PK, FK -> `users.id`, ON DELETE CASCADE)
- `church_name` (varchar, NOT NULL)
- `member_name` (varchar, NOT NULL)
- `group_name` (varchar, NOT NULL)
- `gender` (varchar, NOT NULL, `UNKNOWN` 포함)
- `updated_at` (timestamp)

### 3.2 `user_verifications`
- `user_id` (UUID, PK, FK -> `users.id`, ON DELETE CASCADE)
- `invite_code` (varchar, nullable)
- `invite_verified_at` (timestamp, nullable)
- `phone_number` (varchar, nullable)
- `phone_verified_at` (timestamp, nullable)
- `updated_at` (timestamp)
- INDEX(`phone_number`)

### 3.3 `sms_verification_requests`
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`, ON DELETE CASCADE)
- `phone_number` (varchar, NOT NULL)
- `code_hash` (varchar, NOT NULL)
- `expires_at` (timestamp, NOT NULL)
- `attempts` (int, NOT NULL)
- `verified_at` (timestamp, nullable)
- `created_at` (timestamp, NOT NULL)
- `updated_at` (timestamp, NOT NULL)
- INDEX(`user_id`, `created_at`)

### 3.4 `profile_change_requests`
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`, ON DELETE CASCADE)
- `church_name` (varchar, NOT NULL)
- `member_name` (varchar, NOT NULL)
- `group_name` (varchar, NOT NULL)
- `gender` (varchar, NOT NULL)
- `status` (varchar, `PENDING`/`APPROVED`/`REJECTED`)
- `requested_at` (timestamp, NOT NULL)
- `reviewed_by` (UUID, FK -> `users.id`, ON DELETE SET NULL, nullable)
- `reviewed_at` (timestamp, nullable)
- `reject_reason` (varchar(500), nullable)
- INDEX(`user_id`, `requested_at` DESC)
- INDEX(`status`, `requested_at` DESC)

## 4. 멤버 활동/운영 테이블

### 4.1 `hymn_notes`
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`)
- `hymn_id` (UUID, FK -> `hymns.id`)
- `content` (text)
- `updated_at` (timestamp)
- UNIQUE(`user_id`, `hymn_id`)

### 4.2 `user_hymn_state`
- `user_id` (UUID, FK -> `users.id`)
- `hymn_id` (UUID, FK -> `hymns.id`)
- `favorite` (boolean, NOT NULL)
- `last_opened_at` (timestamp, nullable)
- `last_part_played` (varchar, nullable)
- `last_play_position_ms` (bigint, nullable)
- PRIMARY KEY(`user_id`, `hymn_id`)
- INDEX(`user_id`, `last_opened_at`)

### 4.3 `events`
- `id` (UUID, PK)
- `user_id` (UUID, FK -> `users.id`)
- `event_type` (varchar, `HYMN_OPENED`/`PART_PLAYED`/`NOTE_SAVED`/`FAVORITE_TOGGLED`)
- `hymn_id` (UUID, nullable)
- `part` (varchar, nullable)
- `metadata_json` (text, nullable)
- `created_at` (timestamp)
- INDEX(`user_id`, `created_at`)
- INDEX(`created_at` DESC)
- INDEX(`event_type`, `created_at` DESC)
- INDEX(`hymn_id`, `created_at` DESC)

### 4.4 `invite_codes`
- `code` (varchar, PK)
- `created_by` (UUID, FK -> `users.id`, nullable)
- `description` (text, nullable)
- `max_uses` (integer, nullable)
- `used_count` (integer, NOT NULL, default 0)
- `enabled` (boolean, NOT NULL, default true)
- `expires_at` (timestamp, nullable)
- `created_at` (timestamp, default `now()`)

## 5. 감사 로그 비동기 export 테이블

### 5.1 `event_export_jobs`
- `id` (UUID, PK)
- `requested_by` (UUID, FK -> `users.id`)
- `event_type` (varchar, nullable)
- `user_id` (UUID, nullable)
- `hymn_id` (UUID, nullable)
- `from_inclusive` (timestamp, nullable)
- `to_exclusive` (timestamp, nullable)
- `export_limit` (integer, NOT NULL)
- `status` (varchar, `QUEUED`/`RUNNING`/`COMPLETED`/`FAILED`)
- `row_count` (bigint, nullable)
- `file_name` (varchar, nullable)
- `csv_content` (text, nullable)
- `error_message` (text, nullable)
- `created_at` (timestamp, NOT NULL)
- `started_at` (timestamp, nullable)
- `completed_at` (timestamp, nullable)
- INDEX(`requested_by`, `created_at` DESC)
- INDEX(`status`, `completed_at`)

### 5.2 `event_export_job_cleanup_runs`
- `id` (UUID, PK)
- `executed_at` (timestamp, NOT NULL)
- `retention_days` (integer, NOT NULL)
- `deleted_count` (bigint, NOT NULL)
- INDEX(`executed_at` DESC)

## 6. 메모
- 태그는 현재 콤마 문자열(`hymns.tags`)로 저장한다.
- `events.metadata_json`은 문자열 컬럼이며 필요 시 JSONB로 확장 가능하다.
- 인증/운영 확장으로 `user_profiles`, `user_verifications`, `profile_change_requests`, `event_export_jobs` 계열 테이블이 추가되었다.
