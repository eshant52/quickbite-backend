package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.payment.dto.PaymentResponse;

import java.util.UUID;

/**
 * Interface segregated for customer payment queries.
 * Consumed by customer-facing payment controllers.
 */
public interface PaymentQueryService {

    /**
     * Retrieves the payment details for a given order and customer.
     *
     * @param orderId    the UUID of the order
     * @param customerId the UUID of the customer
     * @return PaymentResponse containing payment details
     * @throws com.quickbite.quickbite.payment.exception.PaymentNotFoundException if no payment is found for the given order and customer
     */
    PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId);
}
