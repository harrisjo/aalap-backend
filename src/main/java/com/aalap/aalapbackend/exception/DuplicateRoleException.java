package com.aalap.aalapbackend.exception;

public class DuplicateRoleException extends RuntimeException {
    public DuplicateRoleException(String message) {
        super(message);
    }
}