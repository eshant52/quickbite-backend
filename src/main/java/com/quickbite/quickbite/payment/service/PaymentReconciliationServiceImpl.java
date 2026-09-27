package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayOrderStatus;
import com.quickbite.quickbite.payment.dto.OnlinePaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentLifecycleService paymentLifecycle;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentReconciliationServiceImpl(
            PaymentRepository paymentRepository,
            PaymentGateway paymentGateway,
            PaymentLifecycleService paymentLifecycle,
            ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
        this.paymentLifecycle = paymentLifecycle;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Optional<PaymentResult> reconcileAllPaymentAttempts(UUID orderId) {
        List<Payment> attempts = paymentRepository.findAttemptsForReconciliation(
                orderId, List.of(PaymentStatus.REFUNDED, PaymentStatus.REFUND_FAILED));

        Set<String> successGatewayOrderIds = new HashSet<>();
        Map<String, GatewayOrderDetails> orderDetailsCache = new HashMap<>();
        PaymentResult winningResult = null;

        for (Payment payment : attempts) {
            if (payment.getPaymentMethod() == PaymentMethod.COD) {
                winningResult = reconcileCodAttempt(payment, winningResult);
                continue;
            }

            String gatewayOrderId = payment.getGatewayOrderId();
            if (gatewayOrderId == null) {
                continue;
            }

            if (successGatewayOrderIds.contains(gatewayOrderId)) {
                cancelIfPending(payment, "Superseded by winning attempt");
                continue;
            }

            winningResult = reconcileOnlineAttempt(
                    payment, gatewayOrderId, orderDetailsCache, successGatewayOrderIds, winningResult);
        }

        return Optional.ofNullable(winningResult);
    }

    private PaymentResult reconcileCodAttempt(Payment payment, PaymentResult winningResult) {
        if (payment.getCurrentStatus() == PaymentStatus.SUCCESS) {
            if (winningResult == null) {
                return toSuccessPaymentResult(payment);
            }
            paymentLifecycle.refundOrCancelCodPayment(payment, "Superseded by winning payment attempt");
        } else if (payment.getCurrentStatus() == PaymentStatus.PENDING && winningResult != null) {
            paymentLifecycle.markCancelled(payment.getId(), "Superseded by winning attempt");
        }
        return winningResult;
    }

    private PaymentResult reconcileOnlineAttempt(
            Payment payment,
            String gatewayOrderId,
            Map<String, GatewayOrderDetails> orderDetailsCache,
            Set<String> successGatewayOrderIds,
            PaymentResult winningResult) {

        // Case A: Payment is ALREADY recorded as SUCCESS in our database
        if (payment.getCurrentStatus() == PaymentStatus.SUCCESS) {
            successGatewayOrderIds.add(gatewayOrderId);
            return claimWinnerOrRefundOnlineDuplicate(
                    payment,
                    payment.getGatewayPaymentId(),
                    winningResult,
                    "Auto-refund: Duplicate successful payment on retried order"
            );
        }

        // Case B: Payment is PENDING or CANCELLED locally — query live status from Gateway
        GatewayOrderDetails orderDetails = orderDetailsCache.computeIfAbsent(
                gatewayOrderId, paymentGateway::fetchOrderStatus);

        if (orderDetails.status() == GatewayOrderStatus.PAID) {
            Payment resolvedPayment = syncGatewayPaidPayment(
                    payment, gatewayOrderId, orderDetails.gatewayPaymentId());
            successGatewayOrderIds.add(gatewayOrderId);
            return claimWinnerOrRefundOnlineDuplicate(
                    resolvedPayment,
                    orderDetails.gatewayPaymentId(),
                    winningResult,
                    "Auto-refund: Duplicate payment captured at gateway on retried order"
            );
        }

        if (orderDetails.status() == GatewayOrderStatus.EXPIRED) {
            cancelIfPending(payment, "Gateway order EXPIRED");
        }

        return winningResult;
    }

    private Payment syncGatewayPaidPayment(Payment payment, String gatewayOrderId, String gatewayPaymentId) {
        Optional<Payment> existingPaidPayment = paymentRepository.findByGatewayOrderIdAndGatewayPaymentIdAndCurrentStatus(
                gatewayOrderId,
                gatewayPaymentId,
                PaymentStatus.SUCCESS
        );
        if (existingPaidPayment.isPresent()) {
            return existingPaidPayment.get();
        }
        paymentLifecycle.markSuccess(payment.getId(), gatewayPaymentId);
        return payment;
    }

    private PaymentResult claimWinnerOrRefundOnlineDuplicate(
            Payment payment,
            String gatewayPaymentId,
            PaymentResult winningResult,
            String duplicateRefundReason) {
        if (winningResult == null) {
            return toSuccessPaymentResult(payment);
        }
        eventPublisher.publishEvent(new PaymentRefundRequestedEvent(
                payment.getId(),
                gatewayPaymentId,
                payment.getAmount(),
                duplicateRefundReason
        ));
        return winningResult;
    }

    private void cancelIfPending(Payment payment, String reason) {
        if (payment.getCurrentStatus() == PaymentStatus.PENDING) {
            paymentLifecycle.markCancelled(payment.getId(), reason);
        }
    }

    private PaymentResult toSuccessPaymentResult(Payment payment) {
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
                PaymentStatus.SUCCESS,
                payment.getAmount(),
                payment.getGatewayOrderId(),
                paymentGateway.getPublishableKey()
        );
    }
}
