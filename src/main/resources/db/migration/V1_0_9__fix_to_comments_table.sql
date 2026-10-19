ALTER TABLE comments
    ADD author_id UUID;

ALTER TABLE comments
    ALTER COLUMN author_id SET NOT NULL;