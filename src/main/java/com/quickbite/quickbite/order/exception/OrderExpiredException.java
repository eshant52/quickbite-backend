package com.quickbite.quickbite.order.exception;

import java.util.UUID;

/**
 * Thrown when an order's payment TTL has expired during a retry attempt.
 *
 * <p>This exception is <b>not</b> mapped to an HTTP response directly.
 * It acts as a structured signal from {@code OrderLifecycleService.prepareOrderForRetry}
 * to the caller ({@code OrderServiceImpl.retryPayment}), which catches it, abandons
 * the order in a <em>separate committed transaction</em>, and then returns a
 * {@code BadRequestException} (HTTP 400) to the client.
 *
 * <p>Separating the signal from the abandon step is intentional: calling
 * {@code abandonOrder} inside the same TX as the TTL check would cause it
 * to be rolled back when the unchecked exception propagates.
 */
public class OrderExpiredException extends RuntimeException {

    private final UUID orderId;

    public OrderExpiredException(UUID orderId) {
        super("Payment window for order " + orderId + " has expired.");
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
