package com.dfs.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for POST /api/auth/login.
 */
public record LoginRequest(

        @NotBlank(message = "email must not be blank")
        String email,

        @NotBlank(message = "password must not be blank")
        String password) {
}
