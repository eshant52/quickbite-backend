package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.payment.dto.PaymentResponse;

import java.util.UUID;

/**
 * Interface segregated for customer payment queries.
 * Consumed by customer-facing payment controllers.
 */
public interface PaymentQueryService {

    /**
     * Returns the payment record for a given order, used by the customer GET endpoint.
     */
    PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId);
}
