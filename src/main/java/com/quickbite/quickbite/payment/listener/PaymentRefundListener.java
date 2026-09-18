package com.quickbite.quickbite.payment.listener;

import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import com.quickbite.quickbite.payment.service.PaymentLifecycleService;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentRefundListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundListener.class);

    private final PaymentGateway paymentGateway;
    private final PaymentLifecycleService paymentLifecycle;

    public PaymentRefundListener(PaymentGateway paymentGateway, PaymentLifecycleService paymentLifecycle) {
        this.paymentGateway = paymentGateway;
        this.paymentLifecycle = paymentLifecycle;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleRefundRequested(PaymentRefundRequestedEvent event) {
        log.info("Processing asynchronous refund for payment {} (gatewayPaymentId: {}, amount: {})",
                event.paymentId(), event.gatewayPaymentId(), event.amount());
        try {
            paymentGateway.refund(event.gatewayPaymentId(), event.amount(), event.reason());
            paymentLifecycle.markRefunded(event.paymentId(), event.reason());
            log.info("Successfully processed refund for payment {}", event.paymentId());
        } catch (Exception e) {
            log.error("Gateway refund failed for payment {}: {}", event.paymentId(), e.getMessage(), e);
            // Mark the payment as REFUND_FAILED so the support team can investigate via payment history.
            // No automated retry — this follows the agreed policy (no admin alerts, no retry queue).
            try {
                paymentLifecycle.markRefundFailed(event.paymentId(),
                        "Gateway refund failed: " + e.getMessage());
            } catch (Exception inner) {
                log.error("Failed to mark payment {} as REFUND_FAILED: {}",
                        event.paymentId(), inner.getMessage(), inner);
            }
        }
    }
}
