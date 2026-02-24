CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    church_name VARCHAR(255) NOT NULL,
    member_name VARCHAR(255) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
