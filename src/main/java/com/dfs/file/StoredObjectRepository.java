package com.dfs.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access for StoredObject — find existing content by checksum (dedup)
 * and manage reference counts.
 */
public interface StoredObjectRepository extends JpaRepository<StoredObject, Long> {

    Optional<StoredObject> findByChecksum(String checksum);
}
