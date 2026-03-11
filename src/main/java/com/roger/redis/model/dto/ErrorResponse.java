package com.roger.redis.model.dto;

import java.time.LocalDateTime;

/**
 * Structured error response returned by the global exception handler.
 *
 * @param status    the HTTP status code
 * @param message   the error message
 * @param timestamp the time the error occurred
 * @param path      the request URI that caused the error
 */
public record ErrorResponse(int status, String message, LocalDateTime timestamp, String path) {
}
