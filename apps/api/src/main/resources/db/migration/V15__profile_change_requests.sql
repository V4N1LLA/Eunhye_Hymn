CREATE TABLE profile_change_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    church_name VARCHAR(255) NOT NULL,
    member_name VARCHAR(255) NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_at TIMESTAMP NOT NULL,
    reviewed_by UUID NULL,
    reviewed_at TIMESTAMP NULL,
    reject_reason VARCHAR(500) NULL,
    CONSTRAINT fk_profile_change_requests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_profile_change_requests_reviewer FOREIGN KEY (reviewed_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_profile_change_requests_user_requested_at
    ON profile_change_requests (user_id, requested_at DESC);

CREATE INDEX idx_profile_change_requests_status_requested_at
    ON profile_change_requests (status, requested_at DESC);
