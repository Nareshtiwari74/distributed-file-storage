package com.dfs.file;

import java.io.InputStream;

/**
 * Stores and retrieves file bytes. Metadata lives in PostgreSQL; the bytes
 * live here. Kept as an interface so the storage backend (MinIO today,
 * multiple nodes later) can change without touching callers.
 */
public interface StorageService {

    void store(String objectKey, InputStream data, long size, String contentType);

    InputStream retrieve(String objectKey);

    void delete(String objectKey);
}
