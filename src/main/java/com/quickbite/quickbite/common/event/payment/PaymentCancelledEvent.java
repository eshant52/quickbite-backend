package com.quickbite.quickbite.common.event.payment;

import java.util.UUID;

/**
 * Published within the same transaction when a {@link com.quickbite.quickbite.payment.model.Payment}
 * transitions to {@code CANCELLED}.
 *
 * <p>The order domain's {@link com.quickbite.quickbite.order.listener.PaymentEventListener}
 * handles order status sync ({@code AWAITING_PAYMENT → PAYMENT_FAILED}) — fixing the
 * previously unhandled case where {@code reconcileExpiredPayment} cancelled the payment
 * but left the order permanently stuck in {@code AWAITING_PAYMENT}.
 *
 * <p>This is an internal Spring application event — not published to Kafka.
 * Kafka dispatch happens via {@code PaymentStatusChangedEvent} published separately.
 */
public record PaymentCancelledEvent(
        UUID paymentId,
        UUID orderId,
        String reason
) {}
