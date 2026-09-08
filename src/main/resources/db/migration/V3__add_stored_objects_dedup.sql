-- Phase 4: content deduplication via reference-counted stored objects.
-- One stored_object per unique file content (by checksum); many files may
-- reference the same stored_object.

CREATE TABLE stored_objects (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    checksum        VARCHAR(64)  NOT NULL UNIQUE,
    object_key      VARCHAR(512) NOT NULL UNIQUE,
    size            BIGINT       NOT NULL,
    reference_count INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Link column on files (nullable for now so we can backfill existing rows)
ALTER TABLE files ADD COLUMN stored_object_id BIGINT;

-- Backfill: create one stored_object per distinct checksum from existing files,
-- summing how many files share it into reference_count.
INSERT INTO stored_objects (checksum, object_key, size, reference_count)
SELECT checksum,
       MIN(object_key)      AS object_key,
       MIN(size)            AS size,
       COUNT(*)             AS reference_count
FROM files
GROUP BY checksum;

-- Point each existing file at its matching stored_object.
UPDATE files f
SET stored_object_id = s.id
FROM stored_objects s
WHERE f.checksum = s.checksum;

-- Now enforce the link and add the foreign key.
ALTER TABLE files ALTER COLUMN stored_object_id SET NOT NULL;
ALTER TABLE files ADD CONSTRAINT fk_files_stored_object
    FOREIGN KEY (stored_object_id) REFERENCES stored_objects(id);

CREATE INDEX idx_files_stored_object_id ON files (stored_object_id);
CREATE INDEX idx_stored_objects_checksum ON stored_objects (checksum);

-- The old per-file object_key is now redundant (the stored_object owns it),
-- but we keep it for one migration to be safe; a later phase can drop it.
