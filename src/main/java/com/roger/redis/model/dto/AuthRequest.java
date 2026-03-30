package com.roger.redis.model.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for login and registration.
 *
 * @param username the username
 * @param password the plain-text password
 * @author Roger
 */
public record AuthRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
) {
}
