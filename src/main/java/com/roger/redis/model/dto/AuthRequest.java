package com.roger.redis.model.dto;

/**
 * Request DTO for login and registration.
 *
 * @param username the username
 * @param password the plain-text password
 * @author Roger
 */
public record AuthRequest(String username, String password) {
}
