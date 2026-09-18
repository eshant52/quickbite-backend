package com.quickbite.quickbite.payment.dto;

/**
 * Razorpay implementation of {@link GatewayWebhookEvent}.
 *
 * @param eventType        Razorpay event name (e.g. "payment.captured", "payment.failed").
 * @param gatewayOrderId   Razorpay order ID ("order_xxx").
 * @param gatewayPaymentId Razorpay payment ID ("pay_xxx").
 * @param message          Error description or failure reason (e.g. "Payment was declined by the bank").
 *                         Null for successful events.
 */
public record RazorpayGatewayWebhookEvent(
        String eventType,
        String gatewayOrderId,
        String gatewayPaymentId,
        String message
) implements GatewayWebhookEvent {
}
