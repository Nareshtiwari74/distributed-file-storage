package com.dfs.auth;

import com.dfs.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when login fails. Deliberately vague ("invalid email or password")
 * so it does not reveal whether the email exists.
 */
public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
}
