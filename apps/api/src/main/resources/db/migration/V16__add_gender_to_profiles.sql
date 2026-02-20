ALTER TABLE user_profiles
    ADD COLUMN gender VARCHAR(16);

UPDATE user_profiles
SET gender = 'UNKNOWN'
WHERE gender IS NULL;

ALTER TABLE user_profiles
    ALTER COLUMN gender SET NOT NULL;

ALTER TABLE profile_change_requests
    ADD COLUMN gender VARCHAR(16);

UPDATE profile_change_requests
SET gender = 'UNKNOWN'
WHERE gender IS NULL;

ALTER TABLE profile_change_requests
    ALTER COLUMN gender SET NOT NULL;
