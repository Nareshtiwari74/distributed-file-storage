-- Deduplication allows multiple file records to reference the same stored
-- object (and therefore the same object_key). The per-file UNIQUE constraint
-- on files.object_key is no longer valid — uniqueness now lives on
-- stored_objects.object_key instead.
ALTER TABLE files DROP CONSTRAINT IF EXISTS files_object_key_key;
