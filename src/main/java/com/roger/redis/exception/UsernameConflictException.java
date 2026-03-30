package com.roger.redis.exception;

/**
 * Exception thrown when attempting to register a username that already exists.
 * Results in an HTTP 409 Conflict response.
 *
 * @author Roger
 */
public class UsernameConflictException extends RuntimeException {

    private final String username;

    public UsernameConflictException(String username) {
        super("Username already exists: " + username);
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
