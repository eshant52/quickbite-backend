package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.GatewayWebhookEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class PaymentServiceImpl implements PaymentProcessingService, PaymentQueryService, PaymentWebhookService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final List<PaymentStrategy> strategies;
    private final PaymentGateway paymentGateway;
    private final PaymentLifecycleService paymentLifecycle;
    private final PaymentReconciliationService paymentReconciliationService;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            List<PaymentStrategy> strategies,
            PaymentGateway paymentGateway,
            PaymentLifecycleService paymentLifecycle,
            PaymentReconciliationService paymentReconciliationService,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.strategies = strategies;
        this.paymentGateway = paymentGateway;
        this.paymentLifecycle = paymentLifecycle;
        this.paymentReconciliationService = paymentReconciliationService;
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
    public void refundSuccessfulPayment(UUID orderId, String reason) {
        List<Payment> successfulPayments = paymentRepository.findByOrderIdAndCurrentStatus(orderId, PaymentStatus.SUCCESS);
        for (Payment payment : successfulPayments) {
            if (payment.getPaymentMethod() == null) {
                continue;
            }

            if (payment.getPaymentMethod().isOnline()
                    && payment.getGatewayPaymentId() != null
                    && !payment.getGatewayPaymentId().isBlank()) {
                eventPublisher.publishEvent(new PaymentRefundRequestedEvent(
                        payment.getId(),
                        payment.getGatewayPaymentId(),
                        payment.getAmount(),
                        reason
                ));
            } else if (payment.getPaymentMethod() == PaymentMethod.COD) {
                paymentLifecycle.refundOrCancelCodPayment(payment, reason);
            }
        }
    }

    @Override
    public Optional<PaymentResult> reconcileAllPaymentAttempts(UUID orderId) {
        return paymentReconciliationService.reconcileAllPaymentAttempts(orderId);
    }

    @Override
    public PaymentResponse getPaymentByOrderId(UUID orderId, UUID customerId) {
        return resolvePaymentResponse(
                orderId,
                () -> paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId)
        );
    }

    @Override
    public PaymentResponse getPaymentByOrderIdForAdmin(UUID orderId) {
        return resolvePaymentResponse(
                orderId,
                () -> paymentRepository.findAllByOrderIdOrderByCreatedAtAsc(orderId)
        );
    }

    private PaymentResponse resolvePaymentResponse(UUID orderId, Supplier<List<Payment>> paymentsLoader) {
        List<Payment> payments = paymentsLoader.get();
        if (payments.isEmpty()) {
            throw new PaymentNotFoundException("Payment not found for order " + orderId);
        }

        // Active reconciliation: if the latest attempt is PENDING and online,
        // synchronize with the gateway so polling reflects the live status immediately.
        Payment latest = payments.getLast();
        if (latest.getCurrentStatus() == PaymentStatus.PENDING && latest.getGatewayOrderId() != null) {
            paymentReconciliationService.reconcileAllPaymentAttempts(orderId);
            payments = paymentsLoader.get();
        }

        // First Success Wins:
        // 1. Pick the earliest SUCCESS payment (the one that fulfilled the order)
        // 2. If none has succeeded, fallback to the latest attempt (active attempt)
        Payment primaryPayment = payments.stream()
                .filter(p -> p.getCurrentStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElse(payments.getLast());

        List<PaymentAttemptSummary> attempts = payments.stream()
                .map(PaymentAttemptSummary::from)
                .toList();

        return PaymentResponse.from(primaryPayment, orderId, attempts);
    }

    @Override
    public void handleStubOnlineWebhook(String transactionId, PaymentStatus newStatus) {
        paymentLifecycle.processStubPayment(transactionId, newStatus);
    }

    @Override
    public void verifyOnlinePayment(UUID customerId, String gatewayOrderId, String gatewayPaymentId, String gatewaySignature) {
        // 1. Cryptographic verification first — throws PaymentVerificationException on failure
        paymentGateway.verifyPaymentSignature(gatewayOrderId, gatewayPaymentId, gatewaySignature);

        // 2. Delegate state transition, ownership verification, and order sync to payment lifecycle service
        paymentLifecycle.processOnlinePaymentSuccess(customerId, gatewayOrderId, gatewayPaymentId);
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
