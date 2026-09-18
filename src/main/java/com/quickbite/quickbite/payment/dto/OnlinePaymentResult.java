package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Returned to the client after {@link com.quickbite.quickbite.payment.service.strategy.OnlinePaymentStrategy}
 * successfully creates a gateway order.
 *
 * <p>The client uses {@code gatewayOrderId} and {@code keyId} to initialise the
 * gateway's Checkout JS (e.g. Razorpay Checkout). The {@code status} will always
 * be {@link PaymentStatus#PENDING} at this stage — the gateway confirms asynchronously
 * via webhook or client-side verification.
 *
 * @param gatewayOrderId  Gateway's order ID (Razorpay: "order_xxx") — required by Checkout JS.
 * @param keyId           Publishable key — safe to expose to the client; used by Checkout JS.
 */
public record OnlinePaymentResult(
        UUID paymentId,
        UUID orderId,
        String transactionId,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        BigDecimal amount,
        String gatewayOrderId,
        String keyId
) implements PaymentResult {
}
