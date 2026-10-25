ALTER TABLE comment_creation_requests
    ALTER COLUMN request_hash TYPE VARCHAR(64) USING (request_hash::VARCHAR(64));

ALTER TABLE comment_creation_requests
    ADD CONSTRAINT comment_creation_check_hash_min_length
        CHECK (char_length(request_hash) >= 64);

ALTER TABLE comment_creation_requests
    ADD CONSTRAINT comment_creation_check_idempotency_key_min_length
    CHECK (char_length(idempotency_key) >= 32);

ALTER TABLE post_creation_request
    ALTER COLUMN request_hash TYPE VARCHAR(64) USING (request_hash::VARCHAR(64));

ALTER TABLE post_creation_request
    ADD CONSTRAINT post_creation_check_hash_min_length
        CHECK (char_length(request_hash) >= 64);

ALTER TABLE post_creation_request
    ADD CONSTRAINT post_creation_check_idempotency_key_min_length
    CHECK (char_length(idempotency_key) >= 32);
