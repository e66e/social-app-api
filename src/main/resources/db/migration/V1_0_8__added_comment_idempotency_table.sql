CREATE TABLE comment_creation_requests
(
    request_hash    VARCHAR(32)                    NOT NULL,
    status          VARCHAR(255)                   NOT NULL,
    resource_id     UUID                                   ,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE         ,
    author_id       UUID                           NOT NULL,
    idempotency_key VARCHAR(64)                    NOT NULL,
    CONSTRAINT pk_comment_creation_requests PRIMARY KEY (author_id, idempotency_key)
);

ALTER TABLE comment_creation_requests
    ADD CONSTRAINT uc_comment_creation_requests_resource UNIQUE (resource_id);
