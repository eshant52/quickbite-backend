package com.quickbite.quickbite.cart.exception;

public class CartConflictException extends RuntimeException {
    public CartConflictException(String message) {
        super(message);
    }
}
