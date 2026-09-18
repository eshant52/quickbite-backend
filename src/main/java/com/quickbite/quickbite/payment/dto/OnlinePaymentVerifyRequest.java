package com.quickbite.quickbite.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the client-side payment verification endpoint.
 *
 * <p>After the customer completes payment via the gateway's Checkout JS,
 * the gateway returns three values to the client. The client must POST them
 * to {@code /api/v1/webhooks/payment/online/verify} so the backend can
 * cryptographically verify authenticity before transitioning order state.
 *
 * @param gatewayOrderId   Gateway's order ID (Razorpay: razorpay_order_id).
 * @param gatewayPaymentId Gateway's payment ID (Razorpay: razorpay_payment_id).
 * @param gatewaySignature HMAC-SHA256 signature (Razorpay: razorpay_signature).
 */
public record OnlinePaymentVerifyRequest(
        @NotBlank(message = "Gateway order ID is required") String gatewayOrderId,
        @NotBlank(message = "Gateway payment ID is required") String gatewayPaymentId,
        @NotBlank(message = "Gateway signature is required") String gatewaySignature
) {
}
