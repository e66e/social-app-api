ALTER TABLE post_creation_request
    ADD resource_id UUID;

ALTER TABLE post_creation_request
    ADD CONSTRAINT uc_post_creation_request_resource UNIQUE (resource_id);

ALTER TABLE post_creation_request
    DROP COLUMN post_id;

ALTER TABLE post_creation_request
    ALTER COLUMN request_hash TYPE VARCHAR(32) USING (request_hash::VARCHAR(32));
