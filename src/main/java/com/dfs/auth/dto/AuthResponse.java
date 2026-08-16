package com.dfs.auth.dto;

/**
 * Returned after successful register/login.
 *
 * @param token      the signed JWT the client sends on subsequent requests
 * @param tokenType  always "Bearer" (how the token is used in the Authorization header)
 * @param email      the authenticated user's email
 */
public record AuthResponse(String token, String tokenType, String email) {

    public static AuthResponse bearer(String token, String email) {
        return new AuthResponse(token, "Bearer", email);
    }
}
