CREATE TABLE user_verifications (
    user_id UUID PRIMARY KEY,
    invite_code VARCHAR(50) NULL,
    invite_verified_at TIMESTAMP NULL,
    phone_number VARCHAR(20) NULL,
    phone_verified_at TIMESTAMP NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_verifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_verifications_phone_number ON user_verifications(phone_number);

CREATE TABLE sms_verification_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INT NOT NULL,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_sms_verification_requests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_sms_verification_requests_user_created_at ON sms_verification_requests(user_id, created_at);
