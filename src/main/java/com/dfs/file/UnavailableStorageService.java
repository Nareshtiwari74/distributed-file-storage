package com.dfs.file;

import com.dfs.common.exception.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Fallback storage used when object storage is not configured (e.g. an
 * auth-only deployment before R2 is wired up). Auth and health work normally;
 * any file operation returns a clear 503 instead of crashing the app at startup.
 * Active only when storage.enabled=false.
 */
@Service
@ConditionalOnProperty(name = "storage.enabled", havingValue = "false")
public class UnavailableStorageService implements StorageService {

    private static final String MSG = "File storage is not configured in this environment yet";

    @Override
    public void store(String objectKey, InputStream data, long size, String contentType) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, MSG);
    }

    @Override
    public InputStream retrieve(String objectKey) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, MSG);
    }

    @Override
    public void delete(String objectKey) {
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, MSG);
    }
}
