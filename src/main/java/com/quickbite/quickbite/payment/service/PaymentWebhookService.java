package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.payment.model.PaymentStatus;

/**
 * Interface segregated for payment gateway webhook ingestion and client-side verification.
 * Consumed by webhook ingress controllers.
 */
public interface PaymentWebhookService {

    /**
     * Handles a stubbed online webhook
     *
     * @param transactionId The transaction ID of the payment.
     * @param newStatus The new status of the payment. It can be either {@link PaymentStatus#SUCCESS} or {@link PaymentStatus#FAILED}.
     */
    void handleStubOnlineWebhook(String transactionId, PaymentStatus newStatus);

    /**
     * Handles a verified webhook from Razorpay (or a compatible gateway).
     * Verifies the HMAC-SHA256 signature, parses the event, and delegates to the appropriate payment lifecycle service method.
     *
     * <p><b>Must be called with the raw, unparsed request body.</b>
     *
     * @param rawBody   Raw JSON body of the webhook request.
     * @param signature Value of the {@code X-Razorpay-Signature} header.
     * @throws com.quickbite.quickbite.payment.exception.PaymentVerificationException if signature is invalid.
     */
    void handleRazorpayWebhook(String rawBody, String signature);
}
