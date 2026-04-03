package com.aalap.aalapbackend.exception;

import com.aalap.aalapbackend.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        // Message is safe to surface: "Email already registered."
        return build(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }

    @ExceptionHandler(NullUserException.class)
    public ResponseEntity<ErrorResponse> handleNullUser(NullUserException e) {
        return build(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<ErrorResponse> handleWrongPassword(WrongPasswordException e) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", e.getMessage());
    }

    @ExceptionHandler(NoolNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoolNotFound(NoolNotFoundException e) {
        return build(HttpStatus.NOT_FOUND, "Not Found", e.getMessage());
    }

    @ExceptionHandler(DuplicateRoleException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateRole(DuplicateRoleException e) {
        return build(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, "Validation Failed", message);
    }

    /** ResponseStatusException (thrown by services for 400 / 403 / 404 etc.) */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException e) {
        // reason() is the developer-supplied, safe-to-expose message
        String reason = e.getReason() != null ? e.getReason() : e.getStatusCode().toString();
        return build(HttpStatus.valueOf(e.getStatusCode().value()), "Request Error", reason);
    }

    /**
     * Catch-all for any unhandled exception.
     *
     * The full stack trace is logged SERVER-SIDE (visible in Render/Docker logs)
     * but a generic message is returned to the client so no internal details leak.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        log.error("Unhandled exception [{}]: {}", e.getClass().getName(), e.getMessage(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    // ─── Helper ─────────────────────────────────────────────────────────────────
    private ResponseEntity<ErrorResponse> build(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                new ErrorResponse(LocalDateTime.now(), status.value(), error, message)
        );
    }
}