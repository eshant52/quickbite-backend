package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface segregated for order checkout and payment initiation.
 * Consumed by order processing services to initiate payments.
 */
public interface PaymentProcessingService {

    /**
     * Selects the correct PaymentStrategy and initiates payment for the given order.
     * For COD: transitions order to PLACED and publishes OrderPlacedEvent immediately.
     * For online methods: creates a PENDING payment and leaves order in AWAITING_PAYMENT.
     */
    PaymentResult initiatePayment(Order order, PaymentMethod method);

    /**
     * Verifies the HMAC-SHA256 signature returned by the gateway's Checkout UI to the client,
     * then transitions payment → SUCCESS and order → PLACED atomically.
     *
     * <p>Idempotent: if the payment is already SUCCESS, returns silently.
     *
     * @param gatewayOrderId   Gateway's order ID (Razorpay: razorpay_order_id).
     * @param gatewayPaymentId Gateway's payment ID (Razorpay: razorpay_payment_id).
     * @param gatewaySignature HMAC-SHA256 signature from the client.
     * @throws com.quickbite.quickbite.payment.exception.PaymentVerificationException if signature is invalid.
     */
    void verifyOnlinePayment(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature);


    /**
     * Cancels any active PENDING payment attempts for the given order.
     *
     * @param orderId the order's UUID
     * @param reason  the reason for cancellation
     */
    void cancelPendingPayments(UUID orderId, String reason);

    /**
     * Reconciles all previous gateway payment attempts for the given order in reverse
     * chronological order (newest first).
     *
     * <p>If a paid attempt is found, it is reconciled to SUCCESS and returned as the winning
     * payment. Any secondary paid attempts (e.g. customer approved multiple retries on bank app)
     * are reconciled to SUCCESS and automatically published for refund.
     *
     * @param orderId the order's UUID
     * @return Optional containing the winning PaymentResult if any attempt was paid, or empty
     */
    Optional<PaymentResult> reconcileAllPaymentAttempts(UUID orderId);

    /**
     * Handles refunding or cancelling any successful payment associated with the order:
     * <ul>
     *   <li><b>Online payments:</b> emits a {@code PaymentRefundRequestedEvent} to refund via the gateway.</li>
     *   <li><b>COD payments:</b> marks the payment {@code REFUNDED} only if the order is {@code DELIVERED};
     *       otherwise marks the uncollected COD payment {@code CANCELLED}.</li>
     * </ul>
     *
     * @param orderId the order's UUID
     * @param reason  the reason for refund or cancellation
     */
    void refundSuccessfulPayment(UUID orderId, String reason);
}
