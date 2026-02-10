CREATE TABLE invite_codes (
    id          UUID PRIMARY KEY,
    code        VARCHAR(64) NOT NULL UNIQUE,
    max_uses    INTEGER,
    used_count  INTEGER NOT NULL DEFAULT 0,
    expires_at  TIMESTAMP WITH TIME ZONE,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_invite_codes_code ON invite_codes(code);
