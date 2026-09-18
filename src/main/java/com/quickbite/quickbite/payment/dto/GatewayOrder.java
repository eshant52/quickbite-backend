package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;

/**
 * Internal record representing a successfully created gateway-side order.
 * Returned by {@link PaymentGateway#createOrder}.
 *
 * <p>Decouples {@link com.quickbite.quickbite.payment.service.strategy.OnlinePaymentStrategy}
 * from any gateway-specific SDK types. Razorpay returns an {@code order_xxx} ID;
 * Stripe would return a Payment Intent ID — both map to {@code gatewayOrderId} here.
 *
 * @param gatewayOrderId  Gateway's order identifier (e.g. "order_xxx" from Razorpay).
 *                        Stored on the {@link com.quickbite.quickbite.payment.model.Payment}
 *                        entity for webhook correlation.
 *
 * @param providerKeyId   Publishable / client-safe key returned to the frontend
 *                        so it can initialize the gateway's Checkout JS.
 */
public record GatewayOrder(
        String gatewayOrderId,
        String providerKeyId
) {
}
