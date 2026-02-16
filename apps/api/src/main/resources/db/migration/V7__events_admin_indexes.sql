-- Indexes for admin event list/export query patterns.
-- Main patterns:
-- 1) ORDER BY created_at DESC with optional time range filter
-- 2) event_type + time range filter
-- 3) hymn_id + time range filter
-- 4) user_id + time range filter (already had idx_events_user_created)

CREATE INDEX idx_events_created_at_desc ON events(created_at DESC);
CREATE INDEX idx_events_event_type_created ON events(event_type, created_at DESC);
CREATE INDEX idx_events_hymn_created ON events(hymn_id, created_at DESC);

