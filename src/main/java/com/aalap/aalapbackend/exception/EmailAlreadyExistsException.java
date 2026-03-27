package com.aalap.aalapbackend.exception;

public class EmailAlreadyExistsException extends  RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
