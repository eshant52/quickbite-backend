package com.quickbite.quickbite.payment.service.strategy;

import com.quickbite.quickbite.common.utils.TransactionIdGenerator;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.payment.dto.GatewayOrder;
import com.quickbite.quickbite.payment.dto.OnlinePaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnlinePaymentStrategyTest {

    @Mock
    private TransactionIdGenerator transactionIdGenerator;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentLifecycleService paymentLifecycle;

    @InjectMocks
    private OnlinePaymentStrategy strategy;

    private Order order;

    @BeforeEach
    void setUp() {
        order = new Order();
        order.setId(UUID.randomUUID());
        order.setTotalAmount(new BigDecimal("499.00"));

        com.quickbite.quickbite.user.model.User customer = new com.quickbite.quickbite.user.model.User();
        customer.setId(UUID.randomUUID());
        order.setCustomer(customer);
    }

    @Test
    @DisplayName("supports - returns true for all online methods")
    void supports_Online_ReturnsTrue() {
        assertThat(strategy.supports(PaymentMethod.UPI)).isTrue();
        assertThat(strategy.supports(PaymentMethod.CARD)).isTrue();
        assertThat(strategy.supports(PaymentMethod.NET_BANKING)).isTrue();
        assertThat(strategy.supports(PaymentMethod.WALLET)).isTrue();
    }

    @Test
    @DisplayName("supports - returns false for COD")
    void supports_Cod_ReturnsFalse() {
        assertThat(strategy.supports(PaymentMethod.COD)).isFalse();
    }

    @Test
    @DisplayName("initiate - creates pending payment, calls gateway, updates gateway order, and returns OnlinePaymentResult")
    void initiate_Success_PersistsPaymentAndReturnsOnlinePaymentResult() {
        UUID paymentId = UUID.randomUUID();
        String txnId = "PAY-260913-ABCD1234";
        GatewayOrder gatewayOrder = new GatewayOrder("order_TestXyz123", "rzp_test_key");

        when(transactionIdGenerator.generate()).thenReturn(txnId);
        when(paymentGateway.getName()).thenReturn("Razorpay");

        Payment pendingPayment = new Payment();
        pendingPayment.setId(paymentId);
        pendingPayment.setTransactionId(txnId);
        pendingPayment.setCurrentStatus(PaymentStatus.PENDING);

        when(paymentLifecycle.createPendingPayment(order, txnId, PaymentMethod.UPI, "Razorpay"))
                .thenReturn(pendingPayment);
        when(paymentGateway.createOrder(order.getTotalAmount(), paymentId.toString()))
                .thenReturn(gatewayOrder);

        Payment updatedPayment = new Payment();
        updatedPayment.setId(paymentId);
        updatedPayment.setTransactionId(txnId);
        updatedPayment.setCurrentStatus(PaymentStatus.PENDING);
        updatedPayment.setGatewayOrderId("order_TestXyz123");

        when(paymentLifecycle.updateGatewayOrder(paymentId, gatewayOrder))
                .thenReturn(updatedPayment);

        PaymentResult result = strategy.initiate(order, PaymentMethod.UPI);

        // Verify result shape
        assertThat(result).isInstanceOf(OnlinePaymentResult.class);
        OnlinePaymentResult onlineResult = (OnlinePaymentResult) result;
        assertThat(onlineResult.paymentId()).isEqualTo(paymentId);
        assertThat(onlineResult.orderId()).isEqualTo(order.getId());
        assertThat(onlineResult.transactionId()).isEqualTo(txnId);
        assertThat(onlineResult.gatewayOrderId()).isEqualTo("order_TestXyz123");
        assertThat(onlineResult.keyId()).isEqualTo("rzp_test_key");
        assertThat(onlineResult.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(onlineResult.paymentMethod()).isEqualTo(PaymentMethod.UPI);

        verify(paymentLifecycle).createPendingPayment(order, txnId, PaymentMethod.UPI, "Razorpay");
        verify(paymentGateway).createOrder(order.getTotalAmount(), paymentId.toString());
        verify(paymentLifecycle).updateGatewayOrder(paymentId, gatewayOrder);
    }

    @Test
    @DisplayName("initiate - gateway failure marks payment as failed and propagates exception")
    void initiate_GatewayFailure_MarksFailedAndThrows() {
        UUID paymentId = UUID.randomUUID();
        String txnId = "PAY-260913-FAIL1234";

        when(transactionIdGenerator.generate()).thenReturn(txnId);
        when(paymentGateway.getName()).thenReturn("Razorpay");

        Payment pendingPayment = new Payment();
        pendingPayment.setId(paymentId);
        pendingPayment.setTransactionId(txnId);
        pendingPayment.setCurrentStatus(PaymentStatus.PENDING);

        when(paymentLifecycle.createPendingPayment(order, txnId, PaymentMethod.CARD, "Razorpay"))
                .thenReturn(pendingPayment);
        when(paymentGateway.createOrder(any(), any()))
                .thenThrow(new RuntimeException("Payment gateway order creation failed"));

        assertThatThrownBy(() -> strategy.initiate(order, PaymentMethod.CARD))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment gateway order creation failed");

        // Verify that payment was marked as failed
        verify(paymentLifecycle).markFailed(paymentId, "Payment gateway order creation failed");
        verify(paymentLifecycle, never()).updateGatewayOrder(any(), any());
    }
}
