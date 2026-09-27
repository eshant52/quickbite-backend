package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayOrderStatus;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PaymentLifecycleService paymentLifecycle;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentReconciliationServiceImpl reconciliationService;

    private Order order;
    private Payment payment;
    private UUID orderId;
    private UUID paymentId;

    @BeforeEach
    void setUp() {
        reconciliationService = new PaymentReconciliationServiceImpl(
                paymentRepository,
                paymentGateway,
                paymentLifecycle,
                eventPublisher
        );

        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        User customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setName("John Doe");
        customer.setEmail("john@example.com");

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pasta Palace");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(BigDecimal.valueOf(499.00));
        order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(Instant.now());

        payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setTransactionId("STUB-12345678");
        payment.setAmount(BigDecimal.valueOf(499.00));
        payment.setCurrentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("Reconciles single paid attempt and returns winning result")
    void singlePaid_returnsWinningResult() {
        payment.setGatewayOrderId("order_rzp_1");
        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(payment));
        when(paymentGateway.fetchOrderStatus("order_rzp_1"))
                .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_123"));
        when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().status()).isEqualTo(PaymentStatus.SUCCESS);
        verify(paymentLifecycle).markSuccess(payment.getId(), "pay_123");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("First Success Wins: When multiple attempts are paid, earliest wins and subsequent is refunded as duplicate")
    void multiPaid_refundsDuplicate() {
        Payment attempt1 = new Payment();
        attempt1.setId(UUID.randomUUID());
        attempt1.setOrder(order);
        attempt1.setGatewayOrderId("order_rzp_1");
        attempt1.setTransactionId("TXN-1");
        attempt1.setAmount(BigDecimal.valueOf(499.00));
        attempt1.setPaymentMethod(PaymentMethod.UPI);
        attempt1.setCurrentStatus(PaymentStatus.PENDING);

        Payment attempt2 = new Payment();
        attempt2.setId(UUID.randomUUID());
        attempt2.setOrder(order);
        attempt2.setGatewayOrderId("order_rzp_2");
        attempt2.setTransactionId("TXN-2");
        attempt2.setAmount(BigDecimal.valueOf(499.00));
        attempt2.setPaymentMethod(PaymentMethod.UPI);
        attempt2.setCurrentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(attempt1, attempt2));
        when(paymentGateway.fetchOrderStatus("order_rzp_1"))
                .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_111"));
        when(paymentGateway.fetchOrderStatus("order_rzp_2"))
                .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_222"));
        when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().paymentId()).isEqualTo(attempt1.getId());
        assertThat(result.get().status()).isEqualTo(PaymentStatus.SUCCESS);

        verify(paymentLifecycle).markSuccess(attempt1.getId(), "pay_111");
        verify(paymentLifecycle).markSuccess(attempt2.getId(), "pay_222");
        verify(eventPublisher).publishEvent(any(PaymentRefundRequestedEvent.class));
    }

    @Test
    @DisplayName("When DB already has a SUCCESS payment, any newly found paid attempt is refunded")
    void existingSuccess_refundsSubsequentGatewayPaid() {
        Payment attempt1 = new Payment();
        attempt1.setId(UUID.randomUUID());
        attempt1.setOrder(order);
        attempt1.setGatewayOrderId("order_rzp_1");
        attempt1.setGatewayPaymentId("pay_existing");
        attempt1.setTransactionId("TXN-1");
        attempt1.setAmount(BigDecimal.valueOf(499.00));
        attempt1.setPaymentMethod(PaymentMethod.UPI);
        attempt1.setCurrentStatus(PaymentStatus.SUCCESS);

        Payment attempt2 = new Payment();
        attempt2.setId(UUID.randomUUID());
        attempt2.setOrder(order);
        attempt2.setGatewayOrderId("order_rzp_2");
        attempt2.setTransactionId("TXN-2");
        attempt2.setAmount(BigDecimal.valueOf(499.00));
        attempt2.setPaymentMethod(PaymentMethod.UPI);
        attempt2.setCurrentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(attempt1, attempt2));
        when(paymentGateway.fetchOrderStatus("order_rzp_2"))
                .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_222"));
        when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().paymentId()).isEqualTo(attempt1.getId());

        verify(paymentGateway, never()).fetchOrderStatus("order_rzp_1");
        verify(paymentLifecycle).markSuccess(attempt2.getId(), "pay_222");
        verify(eventPublisher).publishEvent(any(PaymentRefundRequestedEvent.class));
    }

    @Test
    @DisplayName("Deduplicates multiple Payment rows sharing the same gatewayOrderId")
    void duplicateGatewayOrderId_deduplicated() {
        Payment row1 = new Payment();
        row1.setId(UUID.randomUUID());
        row1.setOrder(order);
        row1.setGatewayOrderId("order_rzp_same");
        row1.setTransactionId("TXN-1");
        row1.setAmount(BigDecimal.valueOf(499.00));
        row1.setPaymentMethod(PaymentMethod.UPI);
        row1.setCurrentStatus(PaymentStatus.PENDING);

        Payment row2 = new Payment();
        row2.setId(UUID.randomUUID());
        row2.setOrder(order);
        row2.setGatewayOrderId("order_rzp_same");
        row2.setTransactionId("TXN-2");
        row2.setAmount(BigDecimal.valueOf(499.00));
        row2.setPaymentMethod(PaymentMethod.UPI);
        row2.setCurrentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(row1, row2));
        when(paymentGateway.fetchOrderStatus("order_rzp_same"))
                .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_winner"));
        when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().paymentId()).isEqualTo(row1.getId());

        verify(paymentGateway, times(1)).fetchOrderStatus("order_rzp_same");
        verify(paymentLifecycle, times(1)).markSuccess(row1.getId(), "pay_winner");
        verify(paymentLifecycle).markCancelled(row2.getId(), "Superseded by winning attempt");
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Returns empty when no attempts were paid and cancels expired payments")
    void nonePaid_returnsEmpty() {
        payment.setGatewayOrderId("order_rzp_expired");
        payment.setCurrentStatus(PaymentStatus.PENDING);

        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(payment));
        when(paymentGateway.fetchOrderStatus("order_rzp_expired"))
                .thenReturn(GatewayOrderDetails.of(GatewayOrderStatus.EXPIRED));

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isEmpty();
        verify(paymentLifecycle).markCancelled(payment.getId(), "Gateway order EXPIRED");
    }

    @Test
    @DisplayName("Returns CodPaymentResult when a COD payment attempt is already SUCCESS")
    void codSuccess_returnsCodPaymentResult() {
        payment.setPaymentMethod(PaymentMethod.COD);
        payment.setCurrentStatus(PaymentStatus.SUCCESS);
        payment.setGatewayOrderId(null);

        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(payment));

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isPresent();
        assertThat(result.get()).isInstanceOf(CodPaymentResult.class);
        assertThat(result.get().paymentId()).isEqualTo(paymentId);
        verifyNoInteractions(paymentGateway, paymentLifecycle, eventPublisher);
    }

    @Test
    @DisplayName("When earlier online attempt was paid and subsequent COD attempt is SUCCESS, online wins and COD is cancelled")
    void onlinePaidThenCod_onlineWinsAndCodCancelled() {
        Payment onlineAttempt = new Payment();
        onlineAttempt.setId(UUID.randomUUID());
        onlineAttempt.setOrder(order);
        onlineAttempt.setGatewayOrderId("order_rzp_1");
        onlineAttempt.setTransactionId("TXN-ONLINE");
        onlineAttempt.setAmount(BigDecimal.valueOf(499.00));
        onlineAttempt.setPaymentMethod(PaymentMethod.UPI);
        onlineAttempt.setCurrentStatus(PaymentStatus.PENDING);

        Payment codAttempt = new Payment();
        codAttempt.setId(UUID.randomUUID());
        codAttempt.setOrder(order);
        codAttempt.setTransactionId("TXN-COD");
        codAttempt.setAmount(BigDecimal.valueOf(499.00));
        codAttempt.setPaymentMethod(PaymentMethod.COD);
        codAttempt.setCurrentStatus(PaymentStatus.SUCCESS);

        order.setCurrentStatus(OrderStatus.PLACED);

        when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                .thenReturn(List.of(onlineAttempt, codAttempt));
        when(paymentGateway.fetchOrderStatus("order_rzp_1"))
                .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_online_1"));
        when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

        Optional<PaymentResult> result = reconciliationService.reconcileAllPaymentAttempts(orderId);

        assertThat(result).isPresent();
        assertThat(result.get().paymentId()).isEqualTo(onlineAttempt.getId());
        verify(paymentLifecycle).markSuccess(onlineAttempt.getId(), "pay_online_1");
        verify(paymentLifecycle).refundOrCancelCodPayment(codAttempt, "Superseded by winning payment attempt");
        verifyNoInteractions(eventPublisher);
    }
}
