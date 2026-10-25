CREATE TABLE follow_relationship_creation_request
(
    request_hash    VARCHAR(64)                    NOT NULL,
    status          VARCHAR(255)                   NOT NULL,
    resource_id     UUID                                   ,
    created_at      TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITHOUT TIME ZONE         ,
    author_id       UUID                           NOT NULL,
    idempotency_key VARCHAR(64)                    NOT NULL,
    CONSTRAINT pk_follow_relationship_creation_request PRIMARY KEY (author_id, idempotency_key)
);

ALTER TABLE follow_relationship_creation_request
    ADD CONSTRAINT uc_follow_relationship_creation_request_resource UNIQUE (author_id, resource_id);
