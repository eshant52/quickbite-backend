package com.quickbite.quickbite.common.event.payment;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentRefundRequestedEvent(
        UUID paymentId,
        String gatewayPaymentId,
        BigDecimal amount,
        String reason
) {
}
