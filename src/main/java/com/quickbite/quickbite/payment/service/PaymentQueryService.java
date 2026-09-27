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

    /**
     * Retrieves the payment details and attempt history for any order (Admin/Support).
     *
     * @param orderId the UUID of the order
     * @return PaymentResponse containing payment details and all attempt histories
     * @throws com.quickbite.quickbite.payment.exception.PaymentNotFoundException if no payment is found for the order
     */
    PaymentResponse getPaymentByOrderIdForAdmin(UUID orderId);
}
