package com.projectguard.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid username or password",
                request
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid username or password",
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(
            AuthenticationException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                safeMessage(exception, "Authentication failed"),
                request
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.NOT_FOUND,
                "Not Found",
                safeMessage(exception, "Resource not found"),
                request
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.CONFLICT,
                "Conflict",
                safeMessage(exception, "Resource already exists"),
                request
        );
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequestException(
            BadRequestException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                safeMessage(exception, "Bad request"),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String message = fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        return build(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                message,
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            IllegalArgumentException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                safeMessage(exception, "Invalid request"),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                "Access denied",
                request
        );
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiErrorResponse> handleSecurityException(
            SecurityException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                safeMessage(exception, "Access denied"),
                request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            IllegalStateException exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.CONFLICT,
                "Conflict",
                safeMessage(exception, "Request conflicts with the current resource state"),
                request
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(
            ResponseStatusException exception,
            HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return build(
                status,
                status.getReasonPhrase(),
                exception.getReason() != null ? exception.getReason() : exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(
            RuntimeException exception,
            HttpServletRequest request) {

        String msg = exception.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("already exists") || lower.contains("conflict")) {
                return build(HttpStatus.CONFLICT, "Conflict", msg, request);
            }
            if (lower.contains("not found") || lower.contains("no student profile")) {
                return build(HttpStatus.NOT_FOUND, "Not Found", msg, request);
            }
            if (lower.contains("required") || lower.contains("invalid") || lower.contains("must be")) {
                return build(HttpStatus.BAD_REQUEST, "Bad Request", msg, request);
            }
            if (lower.contains("access denied") || lower.contains("not own")) {
                return build(HttpStatus.FORBIDDEN, "Forbidden", msg, request);
            }
        }

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                safeMessage(exception, "An unexpected error occurred"),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(
            Exception exception,
            HttpServletRequest request) {

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                safeMessage(exception, "An unexpected error occurred"),
                request
        );
    }

    private ResponseEntity<ApiErrorResponse> build(
            HttpStatus status,
            String error,
            String message,
            HttpServletRequest request) {

        ApiErrorResponse response = new ApiErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(response);
    }

    private String safeMessage(Exception exception, String fallback) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return fallback;
        }

        return message;
    }
}
