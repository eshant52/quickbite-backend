package com.quickbite.quickbite.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class NoAvailableDeliveryAgentException extends RuntimeException {
    public NoAvailableDeliveryAgentException(String message) {
        super(message);
    }
}
