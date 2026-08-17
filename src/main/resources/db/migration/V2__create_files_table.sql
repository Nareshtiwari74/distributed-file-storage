CREATE TABLE files (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_id     BIGINT       NOT NULL REFERENCES users(id),
    filename     VARCHAR(255) NOT NULL,
    size         BIGINT       NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    checksum     VARCHAR(64)  NOT NULL,
    object_key   VARCHAR(512) NOT NULL UNIQUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_files_owner_id ON files (owner_id);
CREATE INDEX idx_files_checksum ON files (checksum);
