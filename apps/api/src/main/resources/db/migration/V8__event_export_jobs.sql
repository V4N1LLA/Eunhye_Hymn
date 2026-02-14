CREATE TABLE event_export_jobs (
    id UUID PRIMARY KEY,
    requested_by UUID NOT NULL REFERENCES users(id),
    event_type VARCHAR(100),
    user_id UUID,
    hymn_id UUID,
    from_inclusive TIMESTAMP,
    to_exclusive TIMESTAMP,
    export_limit INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    row_count BIGINT,
    file_name VARCHAR(255),
    csv_content TEXT,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP
);

CREATE INDEX idx_event_export_jobs_requested_by_created
    ON event_export_jobs(requested_by, created_at DESC);
