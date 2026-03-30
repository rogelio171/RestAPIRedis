package com.roger.redis.exception;

import com.roger.redis.model.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Global exception handler that intercepts exceptions thrown by controllers
 * and returns structured {@link ErrorResponse} bodies with appropriate HTTP status codes.
 *
 * <p>Handled exceptions:</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} &rarr; 404 NOT_FOUND</li>
 *   <li>{@link UsernameConflictException} &rarr; 409 CONFLICT</li>
 *   <li>{@link MethodArgumentNotValidException} &rarr; 400 BAD_REQUEST</li>
 *   <li>{@link IllegalArgumentException} &rarr; 400 BAD_REQUEST</li>
 *   <li>{@link Exception} (catch-all) &rarr; 500 INTERNAL_SERVER_ERROR</li>
 * </ul>
 *
 * @author Roger
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(UsernameConflictException.class)
    public ResponseEntity<ErrorResponse> handleUsernameConflictException(
            UsernameConflictException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        logger.warn("Validation failed at [{}]: {}", request.getRequestURI(), message);

        var errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception ex, HttpStatus status, HttpServletRequest request) {

        String path = request.getRequestURI();

        switch (ex) {
            case ResourceNotFoundException rnfe ->
                    logger.warn("Resource not found at [{}]: {}", path, rnfe.getMessage());
            case UsernameConflictException uce ->
                    logger.warn("Username conflict at [{}]: {}", path, uce.getMessage());
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
