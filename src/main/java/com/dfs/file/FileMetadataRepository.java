package com.dfs.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link FileMetadata}.
 */
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    List<FileMetadata> findAllByOwnerId(Long ownerId);

    Optional<FileMetadata> findByIdAndOwnerId(Long id, Long ownerId);

    Optional<FileMetadata> findByChecksum(String checksum);
}
