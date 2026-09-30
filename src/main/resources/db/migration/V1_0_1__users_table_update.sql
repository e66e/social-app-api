ALTER TABLE users
    ADD onboarding_status VARCHAR(255) DEFAULT 'PENDING';

ALTER TABLE users
    ALTER COLUMN onboarding_status SET NOT NULL;

ALTER TABLE users
    DROP COLUMN role;

ALTER TABLE users
    ADD role VARCHAR(255);