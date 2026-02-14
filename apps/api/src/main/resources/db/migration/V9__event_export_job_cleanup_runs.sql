CREATE TABLE event_export_job_cleanup_runs (
    id UUID PRIMARY KEY,
    executed_at TIMESTAMP NOT NULL,
    retention_days INTEGER NOT NULL,
    deleted_count BIGINT NOT NULL
);

CREATE INDEX idx_event_export_job_cleanup_runs_executed_at_desc
    ON event_export_job_cleanup_runs(executed_at DESC);
