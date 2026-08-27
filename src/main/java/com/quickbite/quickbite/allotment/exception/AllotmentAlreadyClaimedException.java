package com.quickbite.quickbite.allotment.exception;

public class AllotmentAlreadyClaimedException extends RuntimeException {
    public AllotmentAlreadyClaimedException(String message) {
        super(message);
    }
}
