package com.aalap.aalapbackend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExistsException e) {
        return ResponseEntity.status(400).body(e.getMessage());
    }

    @ExceptionHandler(NullUserException.class)
    public ResponseEntity<String> handleNullUser(NullUserException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(WrongPasswordException.class)
    public ResponseEntity<String> handleWrongPassword(WrongPasswordException e) {
        return ResponseEntity.status(401).body(e.getMessage());
    }

    @ExceptionHandler(NoolNotFoundException.class)
    public ResponseEntity<String> handleNoolNotFound(NoolNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(DuplicateRoleException.class)
    public ResponseEntity<String> handleDuplicateRoleException(DuplicateRoleException e) {
        return ResponseEntity.status(400).body(e.getMessage());
    }
}
