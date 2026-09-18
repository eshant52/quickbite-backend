package com.quickbite.quickbite.payment.listener;

import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import com.quickbite.quickbite.payment.exception.PaymentVerificationException;
import com.quickbite.quickbite.payment.service.PaymentLifecycleService;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRefundListenerTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentLifecycleService paymentLifecycle;

    @InjectMocks
    private PaymentRefundListener paymentRefundListener;

    private UUID paymentId;
    private String gatewayPaymentId;
    private BigDecimal amount;
    private String reason;
    private PaymentRefundRequestedEvent event;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        gatewayPaymentId = "pay_test_123456";
        amount = new BigDecimal("499.00");
        reason = "Auto-refund: Order was ABANDONED before payment confirmation";
        event = new PaymentRefundRequestedEvent(paymentId, gatewayPaymentId, amount, reason);
    }

    @Test
    @DisplayName("Calls gateway refund and on success marks payment as refunded in persistence")
    void handleRefundRequested_success() {
        paymentRefundListener.handleRefundRequested(event);

        verify(paymentGateway).refund(gatewayPaymentId, amount, reason);
        verify(paymentLifecycle).markRefunded(paymentId, reason);
    }

    @Test
    @DisplayName("Catches exception when gateway refund fails and does not mark payment as refunded")
    void handleRefundRequested_gatewayFailure_doesNotMarkRefunded() {
        doThrow(new PaymentVerificationException("Razorpay refund API failed"))
                .when(paymentGateway).refund(gatewayPaymentId, amount, reason);

        paymentRefundListener.handleRefundRequested(event);

        verify(paymentGateway).refund(gatewayPaymentId, amount, reason);
        verify(paymentLifecycle, never()).markRefunded(any(), anyString());
    }

    @Test
    @DisplayName("Catches generic exception when gateway refund throws unexpected error")
    void handleRefundRequested_genericFailure_doesNotThrow() {
        doThrow(new RuntimeException("Connection timeout"))
                .when(paymentGateway).refund(gatewayPaymentId, amount, reason);

        paymentRefundListener.handleRefundRequested(event);

        verify(paymentGateway).refund(gatewayPaymentId, amount, reason);
        verify(paymentLifecycle, never()).markRefunded(any(), anyString());
    }
}
