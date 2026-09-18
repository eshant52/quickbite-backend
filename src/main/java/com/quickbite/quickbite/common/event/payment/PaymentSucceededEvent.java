package com.quickbite.quickbite.common.event.payment;

import com.quickbite.quickbite.payment.model.PaymentMethod;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published within the same transaction when a {@link com.quickbite.quickbite.payment.model.Payment}
 * transitions to {@code SUCCESS}.
 *
 * <p>The order domain's {@link com.quickbite.quickbite.order.listener.PaymentEventListener}
 * handles order status sync ({@code AWAITING_PAYMENT → PLACED}) and auto-refund requests
 * for payments that succeed after the order was already {@code ABANDONED} or {@code CANCELLED}.
 *
 * <p>This is an internal Spring application event — not published to Kafka.
 * Kafka dispatch happens via {@code PaymentStatusChangedEvent} published separately.
 */
public record PaymentSucceededEvent(
        UUID paymentId,
        UUID orderId,
        PaymentMethod paymentMethod,
        String gatewayPaymentId,
        BigDecimal amount
) {}
