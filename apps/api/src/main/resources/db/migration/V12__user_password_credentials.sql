CREATE TABLE user_password_credentials (
    user_id UUID PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_password_credentials_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
