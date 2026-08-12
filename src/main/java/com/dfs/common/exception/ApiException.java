package com.dfs.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final transient HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
