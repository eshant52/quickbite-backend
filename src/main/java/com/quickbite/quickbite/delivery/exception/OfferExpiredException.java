package com.quickbite.quickbite.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a delivery agent attempts to accept a delivery offer whose decision window has already elapsed.
 * Mapped to HTTP 422 Unprocessable Entity and configured with {@code noRollbackFor} so that the
 * offer's transition to {@code EXPIRED} commits to the database before the error response is returned.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
public class OfferExpiredException extends RuntimeException {

    public OfferExpiredException(String message) {
        super(message);
    }
}
