-- 초기 MVP 스키마
CREATE TABLE users (
    id UUID PRIMARY KEY,
    display_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP NULL
);

CREATE TABLE auth_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    email VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auth_identities_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_auth_identity_provider_subject UNIQUE (provider, provider_subject)
);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

CREATE TABLE hymns (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    number VARCHAR(50) NULL,
    tags VARCHAR(255) NULL,
    enabled BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE assets (
    id UUID PRIMARY KEY,
    hymn_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    part VARCHAR(50) NULL,
    url VARCHAR(500) NOT NULL,
    checksum VARCHAR(255) NULL,
    version VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_assets_hymn FOREIGN KEY (hymn_id) REFERENCES hymns(id)
);
CREATE INDEX idx_assets_hymn_id ON assets(hymn_id);

CREATE TABLE hymn_notes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    hymn_id UUID NOT NULL,
    content TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_hymn_notes_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_hymn_notes_hymn FOREIGN KEY (hymn_id) REFERENCES hymns(id),
    CONSTRAINT uq_hymn_notes_user_hymn UNIQUE (user_id, hymn_id)
);

CREATE TABLE user_hymn_state (
    user_id UUID NOT NULL,
    hymn_id UUID NOT NULL,
    favorite BOOLEAN NOT NULL,
    last_opened_at TIMESTAMP NULL,
    last_part_played VARCHAR(50) NULL,
    last_play_position_ms BIGINT NULL,
    CONSTRAINT pk_user_hymn_state PRIMARY KEY (user_id, hymn_id),
    CONSTRAINT fk_user_hymn_state_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_user_hymn_state_hymn FOREIGN KEY (hymn_id) REFERENCES hymns(id)
);
CREATE INDEX idx_user_hymn_state_user_opened ON user_hymn_state(user_id, last_opened_at);

CREATE TABLE events (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    hymn_id UUID NULL,
    part VARCHAR(50) NULL,
    metadata_json TEXT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_events_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_events_hymn FOREIGN KEY (hymn_id) REFERENCES hymns(id)
);
CREATE INDEX idx_events_user_created ON events(user_id, created_at);
