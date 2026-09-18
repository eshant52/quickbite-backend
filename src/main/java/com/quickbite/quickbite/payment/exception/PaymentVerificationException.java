package com.quickbite.quickbite.payment.exception;

/**
 * Thrown when a payment gateway signature verification fails.
 *
 * <p>This covers two scenarios:
 * <ol>
 *   <li>Client-side verify: the HMAC-SHA256 of (razorpay_order_id + "|" + razorpay_payment_id)
 *       does not match the signature returned by Razorpay Checkout JS.</li>
 *   <li>Webhook verify: the HMAC-SHA256 of the raw webhook payload does not match
 *       the {@code X-Razorpay-Signature} header.</li>
 * </ol>
 *
 * <p>Maps to HTTP 400 Bad Request via
 * {@link com.quickbite.quickbite.common.exception.GlobalExceptionHandler}.
 */
public class PaymentVerificationException extends RuntimeException {
    public PaymentVerificationException(String message) {
        super(message);
    }
}
