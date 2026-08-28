package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.util.UUID;

public interface PaymentService {
    /**
     * Selects the correct PaymentStrategy and initiates payment for the given order.
     * For COD: transitions order to PLACED and publishes OrderPlacedEvent immediately.
     * For online methods: creates a PENDING payment and leaves order in AWAITING_PAYMENT.
     */
    PaymentResult initiatePayment(Order order, PaymentMethod method);

    /**
     * Returns the payment record for a given order, used by the customer GET endpoint.
     */
    PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId);

    /**
     * Handles payment gateway webhook callbacks, transitioning payment and order state atomically.
     */
    void handleWebhook(String transactionId, PaymentStatus newStatus);
}
