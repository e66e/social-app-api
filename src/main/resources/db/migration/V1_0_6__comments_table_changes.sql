ALTER TABLE comments
    ADD created_at TIMESTAMP(6) WITHOUT TIME ZONE;

ALTER TABLE comments
    ADD updated_at TIMESTAMP(6) WITHOUT TIME ZONE;

ALTER TABLE comments
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE post_creation_request
    ALTER COLUMN request_hash TYPE VARCHAR(64) USING (request_hash::VARCHAR(64));