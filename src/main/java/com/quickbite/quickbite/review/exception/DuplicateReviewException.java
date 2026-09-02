package com.quickbite.quickbite.review.exception;

import java.util.UUID;

public class DuplicateReviewException extends RuntimeException {
    public DuplicateReviewException(UUID orderId) {
        super("A review has already been submitted for order: " + orderId);
    }

    public DuplicateReviewException(String message) {
        super(message);
    }
}
