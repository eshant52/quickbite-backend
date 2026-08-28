package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.model.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebhookPayloadRequest(
        @NotBlank(message = "Transaction ID is required") String transactionId,
        @NotNull(message = "Payment status is required") PaymentStatus status
) {
}
