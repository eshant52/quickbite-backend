package com.quickbite.quickbite.review.exception;

import java.util.UUID;

public class ReviewNotFoundException extends RuntimeException {
    public ReviewNotFoundException(UUID reviewId) {
        super("Review not found with id: " + reviewId);
    }

    public ReviewNotFoundException(String message) {
        super(message);
    }
}
