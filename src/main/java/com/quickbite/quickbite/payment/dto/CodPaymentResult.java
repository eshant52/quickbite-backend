package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record CodPaymentResult(
        UUID paymentId,
        UUID orderId,
        String transactionId,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        BigDecimal amount
) implements PaymentResult {

    /** Convenience constructor — status is always PENDING for COD at order time. */
    public CodPaymentResult(UUID paymentId, UUID orderId, String transactionId, BigDecimal amount) {
        this(paymentId, orderId, transactionId, PaymentMethod.COD, PaymentStatus.PENDING, amount);
    }
}
