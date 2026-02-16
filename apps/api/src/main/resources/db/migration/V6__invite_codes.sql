CREATE TABLE invite_codes (
    code VARCHAR(50) PRIMARY KEY,
    created_by UUID REFERENCES users(id),
    description TEXT,
    max_uses INTEGER,
    used_count INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
