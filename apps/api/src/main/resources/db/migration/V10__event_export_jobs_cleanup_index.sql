CREATE INDEX idx_event_export_jobs_status_completed_at
    ON event_export_jobs(status, completed_at);
