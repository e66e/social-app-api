CREATE TABLE comments
(
    id                UUID         NOT NULL,
    post_id           UUID         NOT NULL,
    content           VARCHAR(255) NOT NULL,
    parent_comment_id UUID         NOT NULL,
    CONSTRAINT pk_comments PRIMARY KEY (id)
);

CREATE TABLE posts
(
    id           UUID         NOT NULL,
    user_id      UUID         NOT NULL,
    text_content VARCHAR(255) NOT NULL,
    image_url    VARCHAR(255) NOT NULL,
    CONSTRAINT pk_post PRIMARY KEY (id)
);

CREATE TABLE users
(
    id              UUID         NOT NULL,
    email           VARCHAR(255) NOT NULL,
    username        VARCHAR(255) NOT NULL,
    public_username VARCHAR(255) NOT NULL,
    avatar_url      VARCHAR(255),
    bio             VARCHAR(255),
    is_active       BOOLEAN,
    role            SMALLINT,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS event_publication
(
    id                     UUID NOT NULL,
    listener_id            TEXT NOT NULL,
    event_type             TEXT NOT NULL,
    serialized_event       TEXT NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    status                 TEXT,
    completion_attempts    INT,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx ON event_publication USING hash(serialized_event);
CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx ON event_publication (completion_date);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);
