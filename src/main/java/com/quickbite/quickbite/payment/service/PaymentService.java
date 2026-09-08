package com.quickbite.quickbite.payment.service;

/**
 * Composite payment service interface extending segregated payment contracts.
 * Prefer injecting the narrower interfaces ({@link PaymentProcessingService},
 * {@link PaymentQueryService}, or {@link PaymentWebhookService}) where appropriate.
 */
public interface PaymentService extends PaymentProcessingService, PaymentQueryService, PaymentWebhookService {
}
