package com.quickbite.quickbite.common.event.payment;

import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusChangedEvent(
        UUID paymentId,
        UUID orderId,
        UUID customerId,
        PaymentStatus previousStatus,
        PaymentStatus newStatus,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        Instant changedAt
) {
}
