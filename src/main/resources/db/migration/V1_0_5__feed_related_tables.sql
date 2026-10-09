CREATE TABLE dead_letter_feed_events
(
    id               UUID                           NOT NULL,
    event_id         UUID                           NOT NULL,
    stream_record_id VARCHAR(255)                   NOT NULL,
    payload_json     VARCHAR(255)                   NOT NULL,
    failure_reason   VARCHAR(255)                   NOT NULL,
    attempts         INTEGER                        NOT NULL,
    moved_to_dlq_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_dead_letter_feed_events PRIMARY KEY (id)
);

CREATE TABLE feed_event_failures
(
    id              UUID                           NOT NULL,
    attempt_count   INTEGER                        NOT NULL,
    last_error      VARCHAR(1024)                  NOT NULL,
    first_failed_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    last_failed_at  TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_feed_event_failures PRIMARY KEY (id)
);

CREATE TABLE processed_feed_events
(
    id           UUID                           NOT NULL,
    processed_at TIMESTAMP(6) WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_processed_feed_events PRIMARY KEY (id)
);

ALTER TABLE users
    ADD is_hot_user BOOLEAN DEFAULT FALSE;

ALTER TABLE users
    ALTER COLUMN is_hot_user SET NOT NULL;

ALTER TABLE post_creation_request
    ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE post_creation_request
    ALTER COLUMN post_id SET NOT NULL;

ALTER TABLE posts
    ALTER COLUMN updated_at DROP NOT NULL;