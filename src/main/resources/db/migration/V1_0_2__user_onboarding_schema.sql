CREATE TABLE user_onboarding
(
    id                UUID                           NOT NULL,
    onboarding_status VARCHAR(255) DEFAULT 'PENDING' NOT NULL,
    CONSTRAINT pk_user_onboarding PRIMARY KEY (id)
);

ALTER TABLE users
    DROP COLUMN email;

ALTER TABLE users
    DROP COLUMN role;

ALTER TABLE users
    ALTER COLUMN avatar_url SET DEFAULT '';

ALTER TABLE users
    ALTER COLUMN bio SET DEFAULT '';

ALTER TABLE users
    ALTER COLUMN is_active SET DEFAULT TRUE;

ALTER TABLE comments
    ALTER COLUMN parent_comment_id DROP NOT NULL;

ALTER TABLE users
    ADD role VARCHAR(255) DEFAULT 'USER';

ALTER TABLE users
    ALTER COLUMN role SET DEFAULT 'USER';