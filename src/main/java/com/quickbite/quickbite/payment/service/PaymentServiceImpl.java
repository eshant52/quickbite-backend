package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayWebhookEvent;
import com.quickbite.quickbite.payment.dto.OnlinePaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentAttemptSummary;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import com.quickbite.quickbite.payment.service.strategy.PaymentStrategy;
import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import com.quickbite.quickbite.payment.dto.GatewayOrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentProcessingService, PaymentQueryService, PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final List<PaymentStrategy> strategies;
    private final PaymentGateway paymentGateway;
    private final PaymentLifecycleService paymentLifecycle;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            List<PaymentStrategy> strategies,
            PaymentGateway paymentGateway,
            PaymentLifecycleService paymentLifecycle,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.strategies = strategies;
        this.paymentGateway = paymentGateway;
        this.paymentLifecycle = paymentLifecycle;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentResult initiatePayment(Order order, PaymentMethod method) {
        PaymentStrategy strategy = strategies.stream()
                .filter(s -> s.supports(method))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Unsupported payment method: " + method));

        return strategy.initiate(order, method);
    }

    @Override
    public void cancelPendingPayments(UUID orderId, String reason) {
        paymentLifecycle.cancelPendingPayments(orderId, reason);
    }

    @Override
    public Optional<PaymentResult> reconcileAllPaymentAttempts(UUID orderId) {
        List<Payment> attempts = paymentRepository.findAttemptsForReconciliation(
                orderId, List.of(PaymentStatus.REFUNDED, PaymentStatus.REFUND_FAILED));

        Set<String> successGatewayOrderIds = new HashSet<>();
        Map<String, GatewayOrderDetails> orderDetailsCache = new HashMap<>();
        boolean isPaid = false;
        PaymentResult winningResult = null;

        for (Payment payment : attempts) {
            String gatewayOrderId = payment.getGatewayOrderId();

            if (successGatewayOrderIds.contains(gatewayOrderId)) {
                if (payment.getCurrentStatus() == PaymentStatus.PENDING) {
                    paymentLifecycle.reconcileExpiredPayment(payment.getId(), "Superseded by winning attempt");
                }
                continue;
            }

            // Case A: Payment is ALREADY recorded as SUCCESS in our database
            if (payment.getCurrentStatus() == PaymentStatus.SUCCESS) {
                if (!isPaid) {
                    isPaid = true;
                    winningResult = toPaymentResult(payment);
                } else {
                    eventPublisher.publishEvent(new PaymentRefundRequestedEvent(
                            payment.getId(),
                            payment.getGatewayPaymentId(),
                            payment.getAmount(),
                            "Auto-refund: Duplicate successful payment on retried order"
                    ));
                }
                successGatewayOrderIds.add(gatewayOrderId);
                continue;
            }

            // Case B: Payment is PENDING or CANCELLED locally — query live status from Gateway
            GatewayOrderDetails orderDetails = orderDetailsCache.computeIfAbsent(
                    gatewayOrderId, paymentGateway::fetchOrderStatus);

            if (orderDetails.status() == GatewayOrderStatus.PAID) {
                Optional<Payment> existingPaidPayment = paymentRepository.findByGatewayOrderIdAndGatewayPaymentIdAndCurrentStatus(
                        gatewayOrderId,
                        orderDetails.gatewayPaymentId(),
                        PaymentStatus.SUCCESS
                );

                if (existingPaidPayment.isPresent()) {
                    payment = existingPaidPayment.get();
                } else {
                    paymentLifecycle.reconcilePaidPayment(payment.getId(), orderDetails.gatewayPaymentId());
                }

                if (!isPaid) {
                    // For existing payment already marked as SUCCESS, we can safely return the existing payment details without creating a new one.
                    isPaid = true;
                    winningResult = new OnlinePaymentResult(
                            payment.getId(),
                            orderId,
                            payment.getTransactionId(),
                            payment.getPaymentMethod(),
                            PaymentStatus.SUCCESS,
                            payment.getAmount(),
                            payment.getGatewayOrderId(),
                            paymentGateway.getPublishableKey()
                    );
                } else {
                    eventPublisher.publishEvent(new PaymentRefundRequestedEvent(
                            payment.getId(),
                            orderDetails.gatewayPaymentId(),
                            payment.getAmount(),
                            "Auto-refund: Duplicate payment captured at gateway on retried order"
                    ));
                }

                successGatewayOrderIds.add(gatewayOrderId);
            } else if (orderDetails.status() == GatewayOrderStatus.EXPIRED) {
                if (payment.getCurrentStatus() == PaymentStatus.PENDING) {
                    paymentLifecycle.reconcileExpiredPayment(payment.getId(), "Gateway order EXPIRED");
                }
            }
        }

        return Optional.ofNullable(winningResult);
    }

    private PaymentResult toPaymentResult(Payment payment) {
        if (!payment.getPaymentMethod().isOnline()) {
            return new CodPaymentResult(
                    payment.getId(),
                    payment.getOrder().getId(),
                    payment.getTransactionId(),
                    payment.getAmount()
            );
        }
        return new OnlinePaymentResult(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getTransactionId(),
                payment.getPaymentMethod(),
                payment.getCurrentStatus(),
                payment.getAmount(),
                payment.getGatewayOrderId(),
                paymentGateway.getPublishableKey()
        );
    }

    @Override
    @Transactional
    public PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId) {
        List<Payment> payments = paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId);
        if (payments.isEmpty()) {
            throw new PaymentNotFoundException("Payment not found for order " + orderId);
        }

        // Active reconciliation: if the latest attempt is PENDING and online,
        // synchronize with the gateway so polling reflects the live status immediately.
        Payment latest = payments.getLast();
        if (latest.getCurrentStatus() == PaymentStatus.PENDING && latest.getGatewayOrderId() != null) {
            reconcileAllPaymentAttempts(orderId);
            payments = paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId);
        }

        // First Success Wins:
        // 1. Pick the earliest SUCCESS payment (the one that fulfilled the order)
        // 2. If none has succeeded, fallback to the latest attempt (active attempt)
        Payment primaryPayment = payments.stream()
                .filter(p -> p.getCurrentStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(payments.getLast());

        List<PaymentAttemptSummary> attempts = payments.stream()
                .map(p -> new PaymentAttemptSummary(
                        p.getId(),
                        p.getTransactionId(),
                        p.getPaymentMethod(),
                        p.getAmount(),
                        p.getCurrentStatus(),
                        p.getGatewayOrderId(),
                        p.getGatewayPaymentId(),
                        p.getCreatedAt()
                ))
                .toList();

        return new PaymentResponse(
                primaryPayment.getId(),
                orderId,
                primaryPayment.getTransactionId(),
                primaryPayment.getPaymentMethod(),
                primaryPayment.getAmount(),
                primaryPayment.getCurrentStatus(),
                primaryPayment.getGatewayOrderId(),
                primaryPayment.getGatewayPaymentId(),
                primaryPayment.getCreatedAt(),
                attempts
        );
    }

    @Override
    public void handleStubOnlineWebhook(String transactionId, PaymentStatus newStatus) {
        paymentLifecycle.processStubPayment(transactionId, newStatus);
    }

    @Override
    public void verifyOnlinePayment(String gatewayOrderId, String gatewayPaymentId, String gatewaySignature) {
        // 1. Cryptographic verification first — throws PaymentVerificationException on failure
        paymentGateway.verifyPaymentSignature(gatewayOrderId, gatewayPaymentId, gatewaySignature);

        // 2. Delegate state transition and order sync to payment lifecycle service
        paymentLifecycle.processOnlinePaymentSuccess(gatewayOrderId, gatewayPaymentId);
    }

    @Override
    public void handleRazorpayWebhook(String rawBody, String signature) {
        // 1. Cryptographic verification — throws PaymentVerificationException on failure
        paymentGateway.verifyWebhookSignature(rawBody, signature);

        // 2. Parse the event
        GatewayWebhookEvent event = paymentGateway.parseWebhookEvent(rawBody);
        log.info("Received Razorpay webhook event: {} for gatewayOrderId: {}",
                event.eventType(), event.gatewayOrderId());

        if (event.gatewayOrderId() == null) {
            log.debug("Razorpay webhook event '{}' has no gatewayOrderId, acknowledging silently", event.eventType());
            return;
        }

        // 3. Route known events to payment lifecycle; silently acknowledge unknown ones
        //    (Razorpay retries if we return non-200, so we must return 200 for all events)
        switch (event.eventType()) {
            case "payment.captured" -> paymentLifecycle.processOnlinePaymentSuccess(event.gatewayOrderId(), event.gatewayPaymentId());
            case "payment.failed" -> {
                String failureReason = (event.message() != null && !event.message().isBlank())
                        ? event.message()
                        : "Razorpay webhook: payment failed";
                paymentLifecycle.processOnlinePaymentFailed(event.gatewayOrderId(), failureReason);
            }
            default -> log.debug("Razorpay webhook event '{}' not handled, acknowledging silently",
                    event.eventType());
        }
    }
}
