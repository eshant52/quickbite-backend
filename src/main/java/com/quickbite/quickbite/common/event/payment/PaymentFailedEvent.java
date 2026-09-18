package com.quickbite.quickbite.common.event.payment;

import java.util.UUID;

/**
 * Published within the same transaction when a {@link com.quickbite.quickbite.payment.model.Payment}
 * transitions to {@code FAILED}.
 *
 * <p>The order domain's {@link com.quickbite.quickbite.order.listener.PaymentEventListener}
 * handles order status sync ({@code AWAITING_PAYMENT → PAYMENT_FAILED}).
 *
 * <p>This is an internal Spring application event — not published to Kafka.
 * Kafka dispatch happens via {@code PaymentStatusChangedEvent} published separately.
 */
public record PaymentFailedEvent(
        UUID paymentId,
        UUID orderId,
        String reason
) {}
