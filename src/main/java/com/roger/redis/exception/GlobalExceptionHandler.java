package com.roger.redis.exception;

import com.roger.redis.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

/**
 * Global exception handler that intercepts exceptions thrown by controllers
 * and returns structured {@link ErrorResponse} bodies with appropriate HTTP status codes.
 *
 * <p>Handled exceptions:</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} &rarr; 404 NOT_FOUND</li>
 *   <li>{@link IllegalArgumentException} &rarr; 400 BAD_REQUEST</li>
 *   <li>{@link Exception} (catch-all) &rarr; 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 *
 * @author Roger
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link ResourceNotFoundException} and returns a 404 response.
     *
     * @param ex      the thrown exception
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} containing the error details with HTTP 404
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles {@link IllegalArgumentException} and returns a 400 response.
     *
     * @param ex      the thrown exception
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} containing the error details with HTTP 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Catch-all handler for any unhandled exceptions. Returns a 500 response.
     *
     * @param ex      the thrown exception
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} containing the error details with HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    /**
     * Builds a structured {@link ErrorResponse} and logs the exception using
     * Java 25 pattern matching in a switch expression to vary log severity
     * based on exception type.
     *
     * @param ex      the thrown exception
     * @param status  the HTTP status to return
     * @param request the current HTTP request
     * @return a {@link ResponseEntity} with the error response and status
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, HttpStatus status, HttpServletRequest request) {

        String path = request.getRequestURI();

        // Use pattern matching in switch to vary log level by exception type
        switch (ex) {
            case ResourceNotFoundException rnfe ->
                    logger.warn("Resource not found at [{}]: {}", path, rnfe.getMessage());
            case IllegalArgumentException iae ->
                    logger.warn("Bad request at [{}]: {}", path, iae.getMessage());
            default ->
                    logger.error("Unexpected error at [{}]: {}", path, ex.getMessage(), ex);
        }

        var errorResponse = new ErrorResponse(
                status.value(),
                ex.getMessage(),
                LocalDateTime.now(),
                path
        );

        return new ResponseEntity<>(errorResponse, status);
    }
}
