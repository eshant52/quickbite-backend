package com.quickbite.quickbite.exceptions;

/**
 * Thrown when a request is malformed or contains invalid parameters.
 * Mapped to HTTP 400 by GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}

