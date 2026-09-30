CREATE TABLE post_creation_request
(
    request_hash    VARCHAR(255) NOT NULL,
    status          VARCHAR(255) NOT NULL,
    post_id         UUID,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE,
    author_id       UUID         NOT NULL,
    idempotency_key VARCHAR(64)  NOT NULL,
    CONSTRAINT pk_post_creation_request PRIMARY KEY (author_id, idempotency_key)
);

ALTER TABLE post_creation_request
    ADD CONSTRAINT uc_post_creation_request_post UNIQUE (post_id);

ALTER TABLE users
    DROP COLUMN onboarding_status;
