ALTER TABLE assets
    ADD COLUMN object_key VARCHAR(255) NOT NULL DEFAULT '';

UPDATE assets
SET object_key = CONCAT('hymns/', hymn_id, '/legacy')
WHERE object_key = '';

ALTER TABLE assets
    ALTER COLUMN object_key DROP DEFAULT;
