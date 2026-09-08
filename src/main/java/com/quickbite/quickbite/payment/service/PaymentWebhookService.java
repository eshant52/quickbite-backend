package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.payment.model.PaymentStatus;

/**
 * Interface segregated for payment gateway webhook ingestion.
 * Consumed by webhook ingress controllers.
 */
public interface PaymentWebhookService {

    /**
     * Handles payment gateway webhook callbacks, transitioning payment and order state atomically.
     */
    void handleWebhook(String transactionId, PaymentStatus newStatus);
}
