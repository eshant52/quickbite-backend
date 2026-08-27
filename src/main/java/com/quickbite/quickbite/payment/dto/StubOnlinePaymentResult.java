package com.quickbite.quickbite.payment.dto;

import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Returned for all online payment methods (UPI, CARD, NET_BANKING, WALLET)
 * until real gateway adapters (Razorpay, Stripe) are implemented.
 * <p>
 * The {@code paymentUrl} is a stub redirect URL the client can use to
 * simulate a gateway redirect. Replace this record with
 * {@code RazorpayPaymentResult} / {@code StripePaymentResult} once the
 * real adapters are wired up.
 */
public record StubOnlinePaymentResult(
        UUID paymentId,
        UUID orderId,
        String transactionId,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        BigDecimal amount,
        String paymentUrl
) implements PaymentResult {}
