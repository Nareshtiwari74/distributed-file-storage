package com.dfs.file.dto;

import com.dfs.file.FileMetadata;

import java.time.Instant;

/**
 * File metadata returned to clients. Never exposes the object key or owner
 * internals — just what the user needs.
 */
public record FileResponse(
        Long id,
        String filename,
        long size,
        String contentType,
        String checksum,
        Instant createdAt) {

    public static FileResponse from(FileMetadata f) {
        return new FileResponse(
                f.getId(), f.getFilename(), f.getSize(),
                f.getContentType(), f.getChecksum(), f.getCreatedAt());
    }
}
