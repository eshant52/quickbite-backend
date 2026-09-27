package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        String transactionId,
        PaymentMethod method,
        BigDecimal amount,
        PaymentStatus status,
        String gatewayOrderId,
        String gatewayPaymentId,
        Instant createdAt,
        List<PaymentAttemptSummary> attempts) {

    public static PaymentResponse from(Payment payment, UUID orderId, List<PaymentAttemptSummary> attempts) {
        return new PaymentResponse(
                payment.getId(),
                orderId,
                payment.getTransactionId(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getCurrentStatus(),
                payment.getGatewayOrderId(),
                payment.getGatewayPaymentId(),
                payment.getCreatedAt(),
                attempts
        );
    }
}
