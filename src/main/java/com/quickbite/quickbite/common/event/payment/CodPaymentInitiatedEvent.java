package com.quickbite.quickbite.common.event.payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published within the same transaction ({@code BEFORE_COMMIT}) when a Cash on Delivery (COD)
 * {@link com.quickbite.quickbite.payment.model.Payment} is created in {@code PENDING} state.
 *
 * <p>Unlike online payments (which transition the order to {@code PLACED} only upon
 * {@link PaymentSucceededEvent}), COD payments remain {@code PENDING} until doorstep cash
 * collection at delivery time, while still confirming the order ({@code AWAITING_PAYMENT /
 * PAYMENT_FAILED → PLACED}) immediately at checkout.
 */
public record CodPaymentInitiatedEvent(
        UUID paymentId,
        UUID orderId,
        BigDecimal amount
) {}
