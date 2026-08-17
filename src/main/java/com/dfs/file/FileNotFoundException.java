package com.dfs.file;

import com.dfs.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a file does not exist or is not owned by the requesting user.
 * Uses 404 (not 403) so we do not reveal that someone else's file exists.
 */
public class FileNotFoundException extends ApiException {

    public FileNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "File " + id + " not found");
    }
}
