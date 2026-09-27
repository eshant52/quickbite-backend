package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.event.payment.PaymentCancelledEvent;
import com.quickbite.quickbite.common.event.payment.PaymentFailedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentStatusChangedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentSucceededEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.payment.dto.GatewayOrder;
import com.quickbite.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.model.PaymentStatusHistory;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.repository.PaymentStatusHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Manages the full lifecycle of {@link Payment} entities.
 *
 * <h2>Cross-domain boundary</h2>
 * <p>This service no longer directly imports {@code OrderRepository} or
 * {@code OrderStatusHistoryRepository}. Order state changes are driven by
 * internal Spring application events ({@link PaymentSucceededEvent},
 * {@link PaymentFailedEvent}, {@link PaymentCancelledEvent}) that are
 * published within the same DB transaction and handled by
 * {@link com.quickbite.quickbite.order.listener.PaymentEventListener}
 * ({@code BEFORE_COMMIT} phase) in the order domain.
 *
 * <h2>Terminal state guards</h2>
 * <p>No transition may overwrite a terminal payment status
 * ({@code REFUNDED} or {@code REFUND_FAILED}), and {@code SUCCESS}
 * can never be overwritten by {@code FAILED}.
 */
@Service
public class PaymentLifecycleServiceImpl implements PaymentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(PaymentLifecycleServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentLifecycleServiceImpl(
            PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository paymentStatusHistoryRepository,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentStatusHistoryRepository = paymentStatusHistoryRepository;
        this.eventPublisher = eventPublisher;
    }

    // ── Creation ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public Payment createPendingPayment(Order order, String transactionId,
            PaymentMethod paymentMethod, String gatewayName) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(transactionId);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrentStatus(PaymentStatus.PENDING);
        payment.setGatewayName(gatewayName);

        Payment savedPayment = paymentRepository.save(payment);

        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(savedPayment);
        history.setStatus(PaymentStatus.PENDING);
        paymentStatusHistoryRepository.save(history);

        return savedPayment;
    }

    @Override
    @Transactional
    public Payment createPendingPayment(Order order, String transactionId,
            PaymentMethod paymentMethod) {
        return createPendingPayment(order, transactionId, paymentMethod, null);
    }

    @Override
    @Transactional
    public Payment updateGatewayOrder(UUID paymentId, GatewayOrder gatewayOrder) {
        Payment payment = loadPayment(paymentId);
        payment.setGatewayOrderId(gatewayOrder.gatewayOrderId());
        return paymentRepository.save(payment);
    }

    // ── Gateway-driven transitions ────────────────────────────────────────────

    @Override
    @Transactional
    public void processOnlinePaymentSuccess(String gatewayOrderId, String gatewayPaymentId) {
        Payment payment = paymentRepository.findByGatewayOrderIdForUpdate(gatewayOrderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for gateway order: " + gatewayOrderId));
        applySuccessTransition(payment, gatewayPaymentId);
    }

    @Override
    @Transactional
    public void processOnlinePaymentFailed(String gatewayOrderId, String reason) {
        Payment payment = paymentRepository.findByGatewayOrderIdForUpdate(gatewayOrderId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for gateway order: " + gatewayOrderId));
        applyFailedTransition(payment, reason);
    }

    @Override
    @Transactional
    public void processStubPayment(String transactionId, PaymentStatus status) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for transaction " + transactionId));

        if (!"STUB_GATEWAY".equals(payment.getGatewayName())) {
            throw new BadRequestException("Stub webhook is only allowed for STUB_GATEWAY payments");
        }

        if (status != PaymentStatus.SUCCESS && status != PaymentStatus.FAILED) {
            throw new BadRequestException("Invalid payment status for webhook: " + status);
        }

        if (status == PaymentStatus.FAILED) {
            applyFailedTransition(payment, "Stub webhook: payment failed");
        } else {
            applySuccessTransition(payment, "stub_pay_" + payment.getTransactionId());
        }
    }

    @Override
    @Transactional
    public void cancelPendingPayments(UUID orderId, String reason) {
        List<Payment> pending = paymentRepository.findByOrderIdAndCurrentStatus(
                orderId, PaymentStatus.PENDING);
        for (Payment payment : pending) {
            applyCancelledTransition(payment, reason);
        }
    }

    // ── Explicit status setters ───────────────────────────────────────────────

    @Override
    @Transactional
    public void markSuccess(UUID paymentId, String gatewayPaymentId) {
        applySuccessTransition(loadPayment(paymentId), gatewayPaymentId);
    }

    @Override
    @Transactional
    public void markFailed(UUID paymentId, String reason) {
        applyFailedTransition(loadPayment(paymentId), reason);
    }

    @Override
    @Transactional
    public void markCancelled(UUID paymentId, String reason) {
        applyCancelledTransition(loadPayment(paymentId), reason);
    }

    @Override
    @Transactional
    public void markRefunded(UUID paymentId, String reason) {
        applyRefundTerminalTransition(paymentId, PaymentStatus.REFUNDED, reason);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRefundFailed(UUID paymentId, String reason) {
        applyRefundTerminalTransition(paymentId, PaymentStatus.REFUND_FAILED, reason);
    }

    @Override
    @Transactional
    public void refundOrCancelCodPayment(Payment payment, String reason) {
        OrderStatus orderStatus = payment.getOrder().getCurrentStatus();
        if (orderStatus == OrderStatus.DELIVERED) {
            applyRefundTerminalTransition(payment.getId(), PaymentStatus.REFUNDED, reason);
        } else {
            applyCancelledTransition(loadPayment(payment.getId()), reason);
        }
    }

    // ── Private transition methods (SRP-compliant, separated by terminal state) ─

    private void applyRefundTerminalTransition(UUID paymentId, PaymentStatus targetStatus, String reason) {
        Payment payment = loadPayment(paymentId);
        PaymentStatus previousStatus = payment.getCurrentStatus();

        if (previousStatus == targetStatus) {
            log.info("Payment {} is already {}. Skipping.", paymentId, targetStatus);
            return;
        }

        updatePaymentStatus(payment, targetStatus, reason);
        publishStatusChangedEvent(payment, previousStatus, targetStatus);
    }

    private void applySuccessTransition(Payment payment, String gatewayPaymentId) {
        if (gatewayPaymentId != null && !gatewayPaymentId.isBlank()) {
            payment.setGatewayPaymentId(gatewayPaymentId);
        }

        PaymentStatus previousStatus = payment.getCurrentStatus();

        if (previousStatus == PaymentStatus.SUCCESS) {
            log.info("Payment {} is already SUCCESS. Skipping.", payment.getId());
            return;
        }

        // Never overwrite a terminal refund state
        if (isRefundTerminal(previousStatus)) {
            log.warn("Skipping SUCCESS transition — payment {} is in terminal state {}",
                    payment.getId(), previousStatus);
            return;
        }

        updatePaymentStatus(payment, PaymentStatus.SUCCESS, null);

        Order order = payment.getOrder();

        // Publish internal event for order domain to sync state (BEFORE_COMMIT listener)
        eventPublisher.publishEvent(new PaymentSucceededEvent(
                payment.getId(),
                order.getId(),
                payment.getPaymentMethod(),
                payment.getGatewayPaymentId(),
                payment.getAmount()
        ));

        // Publish Kafka-dispatched event (AFTER_COMMIT)
        publishStatusChangedEvent(payment, previousStatus, PaymentStatus.SUCCESS);
    }

    private void applyFailedTransition(Payment payment, String reason) {
        PaymentStatus previousStatus = payment.getCurrentStatus();

        if (previousStatus == PaymentStatus.FAILED) {
            log.info("Payment {} is already FAILED. Skipping.", payment.getId());
            return;
        }

        // Terminal state guard: SUCCESS or CANCELLED should not be overwritten by FAILED
        if (previousStatus == PaymentStatus.SUCCESS || previousStatus == PaymentStatus.CANCELLED) {
            log.warn("Ignoring FAILED transition for already {} payment: {}", previousStatus, payment.getId());
            return;
        }

        if (isRefundTerminal(previousStatus)) {
            log.warn("Skipping FAILED transition — payment {} is in terminal state {}",
                    payment.getId(), previousStatus);
            return;
        }

        updatePaymentStatus(payment, PaymentStatus.FAILED, reason);

        Order order = payment.getOrder();

        // Publish internal event for order domain to sync state (BEFORE_COMMIT listener)
        eventPublisher.publishEvent(new PaymentFailedEvent(
                payment.getId(),
                order.getId(),
                reason
        ));

        // Publish Kafka-dispatched event (AFTER_COMMIT)
        publishStatusChangedEvent(payment, previousStatus, PaymentStatus.FAILED);
    }

    private void applyCancelledTransition(Payment payment, String reason) {
        PaymentStatus previousStatus = payment.getCurrentStatus();

        if (previousStatus == PaymentStatus.CANCELLED) {
            log.info("Payment {} is already CANCELLED. Skipping.", payment.getId());
            return;
        }

        if (previousStatus == PaymentStatus.FAILED) {
            log.warn("Ignoring CANCELLED transition for already FAILED payment: {}", payment.getId());
            return;
        }

        // Online SUCCESS payments captured money and can only transition to REFUNDED / REFUND_FAILED.
        // Only COD payments may transition from SUCCESS -> CANCELLED (when cancelled prior to delivery).
        if (previousStatus == PaymentStatus.SUCCESS
                && payment.getPaymentMethod() != null
                && payment.getPaymentMethod().isOnline()) {
            log.warn("Ignoring CANCELLED transition for already SUCCESS online payment: {}", payment.getId());
            return;
        }

        if (isRefundTerminal(previousStatus)) {
            log.warn("Skipping CANCELLED transition — payment {} is in terminal state {}",
                    payment.getId(), previousStatus);
            return;
        }

        updatePaymentStatus(payment, PaymentStatus.CANCELLED, reason);

        Order order = payment.getOrder();

        // Publish internal event for order domain to sync state (BEFORE_COMMIT listener).
        eventPublisher.publishEvent(new PaymentCancelledEvent(
                payment.getId(),
                order.getId(),
                reason
        ));

        // Publish Kafka-dispatched event (AFTER_COMMIT)
        publishStatusChangedEvent(payment, previousStatus, PaymentStatus.CANCELLED);
    }

    private Payment loadPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));
    }

    private void publishStatusChangedEvent(
            Payment payment, PaymentStatus previousStatus, PaymentStatus newStatus) {
        Order order = payment.getOrder();
        eventPublisher.publishEvent(new PaymentStatusChangedEvent(
                payment.getId(),
                order.getId(),
                order.getCustomer().getId(),
                previousStatus,
                newStatus,
                payment.getPaymentMethod(),
                payment.getAmount(),
                Instant.now()
        ));
    }

    private void updatePaymentStatus(Payment payment, PaymentStatus newStatus, String reason) {
        payment.setCurrentStatus(newStatus);
        paymentRepository.save(payment);

        PaymentStatusHistory history = new PaymentStatusHistory();
        history.setPayment(payment);
        history.setStatus(newStatus);
        history.setReason(reason);
        paymentStatusHistoryRepository.save(history);
    }

    /** Returns {@code true} if the status is a terminal refund state that should never be overwritten. */
    private boolean isRefundTerminal(PaymentStatus status) {
        return status == PaymentStatus.REFUNDED || status == PaymentStatus.REFUND_FAILED;
    }
}