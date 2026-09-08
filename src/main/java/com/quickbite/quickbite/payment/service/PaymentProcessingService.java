package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;

/**
 * Interface segregated for order checkout and payment initiation.
 * Consumed by order processing services to initiate payments.
 */
public interface PaymentProcessingService {

    /**
     * Selects the correct PaymentStrategy and initiates payment for the given order.
     * For COD: transitions order to PLACED and publishes OrderPlacedEvent immediately.
     * For online methods: creates a PENDING payment and leaves order in AWAITING_PAYMENT.
     */
    PaymentResult initiatePayment(Order order, PaymentMethod method);
}
