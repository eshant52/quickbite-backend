package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;

/**
 * Sealed interface representing a parsed webhook event from a payment gateway.
 * Returned by {@link PaymentGateway#parseWebhookEvent}.
 *
 * <p>Decouples {@link com.quickbite.quickbite.payment.service.PaymentServiceImpl}
 * from gateway-specific webhook structures while preserving type safety via sealed hierarchy.
 *
 * <p>Concrete implementations:
 * <ul>
 *   <li>{@link RazorpayGatewayWebhookEvent} — Razorpay webhook event payload</li>
 * </ul>
 */
public sealed interface GatewayWebhookEvent permits RazorpayGatewayWebhookEvent {

    /** Gateway event name (e.g. "payment.captured", "payment.failed"). */
    String eventType();

    /** Correlates back to {@code Payment.gatewayOrderId} in our DB. */
    String gatewayOrderId();

    /** Gateway's payment identifier (e.g. "pay_xxx" from Razorpay). */
    String gatewayPaymentId();

    /**
     * Optional message or failure reason provided by the gateway
     * (e.g. error description on failed payments). Null for successful events.
     */
    String message();
}
