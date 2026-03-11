package com.roger.redis.model.dto;

/**
 * Response DTO returned after successful login.
 *
 * @param token     the JWT access token
 * @param expiresIn expiration time in seconds
 * @author Roger
 */
public record AuthResponse(String token, long expiresIn) {
}
