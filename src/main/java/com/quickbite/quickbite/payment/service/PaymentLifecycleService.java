package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.GatewayOrder;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;

import java.util.UUID;

/**
 * Domain service managing the lifecycle, state machine transitions, status history,
 * order status synchronization, and domain event emissions for {@link Payment} entities.
 */
public interface PaymentLifecycleService {

    /**
     * Creates a new Payment entity with status PENDING for an online payment method.
     */
    Payment createPendingPayment(Order order, String transactionId, PaymentMethod paymentMethod, String gatewayName);

    /**
     * Creates a new Payment entity with status PENDING for Cash on Delivery (COD).
     */
    Payment createPendingPayment(Order order, String transactionId, PaymentMethod paymentMethod);

    /**
     * Updates the gateway order ID on a pending payment.
     */
    Payment updateGatewayOrder(UUID paymentId, GatewayOrder gatewayOrder);

    /**
     * Processes online payment capture confirmed by gateway order ID.
     * Synchronizes associated Order to PLACED (or requests auto-refund if already ABANDONED/CANCELLED)
     * and publishes events.
     */
    void processOnlinePaymentSuccess(String gatewayOrderId, String gatewayPaymentId);

    default void processOnlinePaymentSuccess(UUID customerId, String gatewayOrderId, String gatewayPaymentId) {
        processOnlinePaymentSuccess(gatewayOrderId, gatewayPaymentId);
    }

    /**
     * Processes online payment failure reported by gateway.
     */
    void processOnlinePaymentFailed(String gatewayOrderId, String reason);

    /**
     * Processes stub online payment status change (dev environment).
     */
    void processStubPayment(String transactionId, PaymentStatus status);

    /**
     * Cancels all pending payments associated with the given order ID.
     */
    void cancelPendingPayments(UUID orderId, String reason);

    /**
     * Marks a payment as SUCCESS in an isolated transaction.
     * Invoked after successful payment capture at the payment gateway or for COD initiation.
     */
    void markSuccess(UUID paymentId, String gatewayPaymentId);

    /**
     * Marks a payment as FAILED with a failure reason.
     */
    void markFailed(UUID paymentId, String reason);

    /**
     * Marks a payment as CANCELLED in an isolated transaction.
     * Invoked after successful cancellation at the payment gateway or when an order is abandoned.
     */
    void markCancelled(UUID paymentId, String reason);

    /**
     * Marks a payment as REFUNDED in an isolated transaction.
     * Invoked after successful refund dispatch at the payment gateway.
     */
    void markRefunded(UUID paymentId, String reason);

    /**
     * Marks a payment as {@code REFUND_FAILED}, recording the error reason in
     * {@code PaymentStatusHistory}.
     *
     * <p>Invoked by {@link com.quickbite.quickbite.payment.listener.PaymentRefundListener}
     * when a gateway refund call throws an exception. No automated retry is performed —
     * the support team investigates via the payment's status history.
     *
     * <p>Runs in its own isolated transaction ({@code REQUIRES_NEW}) so the failure
     * record is always persisted even if the calling listener's context is rolling back.
     */
    void markRefundFailed(UUID paymentId, String reason);

    /**
     * Refunds a COD payment if its associated order is {@code DELIVERED},
     * or cancels the payment if cash was never collected (pre-delivery).
     */
    void refundOrCancelCodPayment(Payment payment, String reason);

}
