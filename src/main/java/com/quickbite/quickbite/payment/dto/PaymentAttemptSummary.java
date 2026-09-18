package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptSummary(
        UUID id,
        String transactionId,
        PaymentMethod method,
        BigDecimal amount,
        PaymentStatus status,
        String gatewayOrderId,
        String gatewayPaymentId,
        Instant createdAt
) {
}
