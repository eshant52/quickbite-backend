package com.quickbite.quickbite.payment.service;

import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.payment.dto.CodPaymentResult;
import com.quickbite.quickbite.payment.dto.GatewayOrderDetails;
import com.quickbite.quickbite.payment.dto.GatewayOrderStatus;
import com.quickbite.quickbite.payment.dto.OnlinePaymentResult;
import com.quickbite.quickbite.payment.dto.RazorpayGatewayWebhookEvent;
import com.quickbite.quickbite.payment.dto.PaymentResponse;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.exception.PaymentNotFoundException;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.payment.service.gateway.PaymentGateway;
import com.quickbite.quickbite.payment.service.strategy.PaymentStrategy;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentStrategy paymentStrategy;
    @Mock private PaymentGateway paymentGateway;
    @Mock private PaymentLifecycleService paymentLifecycle;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentServiceImpl paymentService;

    private User customer;
    private Restaurant restaurant;
    private Order order;
    private Payment payment;
    private UUID customerId;
    private UUID orderId;
    private UUID paymentId;
    private String transactionId;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                paymentRepository,
                List.of(paymentStrategy),
                paymentGateway,
                paymentLifecycle,
                eventPublisher
        );

        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();
        transactionId = "STUB-12345678";

        customer = new User();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");

        restaurant = new Restaurant();
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
        payment.setTransactionId(transactionId);
        payment.setAmount(BigDecimal.valueOf(499.00));
        payment.setCurrentStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("initiatePayment")
    class InitiatePaymentTests {

        @Test
        @DisplayName("Dispatches to matching strategy")
        void initiatePayment_success() {
            when(paymentStrategy.supports(PaymentMethod.COD)).thenReturn(true);
            PaymentResult mockResult = new CodPaymentResult(paymentId, orderId, transactionId, BigDecimal.valueOf(499.00));
            when(paymentStrategy.initiate(order, PaymentMethod.COD)).thenReturn(mockResult);

            PaymentResult result = paymentService.initiatePayment(order, PaymentMethod.COD);

            assertThat(result).isEqualTo(mockResult);
        }

        @Test
        @DisplayName("Throws BadRequestException when no strategy supports method")
        void initiatePayment_unsupported() {
            when(paymentStrategy.supports(PaymentMethod.WALLET)).thenReturn(false);

            assertThatThrownBy(() -> paymentService.initiatePayment(order, PaymentMethod.WALLET))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Unsupported payment method");
        }
    }

    @Nested
    @DisplayName("getPaymentByOrderId")
    class GetPaymentByOrderIdTests {

        @Test
        @DisplayName("Returns enriched PaymentResponse when payment exists and is not pending gateway")
        void getPaymentByOrderId_success() {
            payment.setCurrentStatus(PaymentStatus.SUCCESS);
            payment.setGatewayOrderId("order_rzp_1");
            payment.setGatewayPaymentId("pay_1");
            when(paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId))
                    .thenReturn(List.of(payment));

            PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

            assertThat(response.id()).isEqualTo(paymentId);
            assertThat(response.orderId()).isEqualTo(orderId);
            assertThat(response.transactionId()).isEqualTo(transactionId);
            assertThat(response.method()).isEqualTo(PaymentMethod.UPI);
            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(response.gatewayOrderId()).isEqualTo("order_rzp_1");
            assertThat(response.gatewayPaymentId()).isEqualTo("pay_1");
            assertThat(response.attempts()).hasSize(1);
            assertThat(response.attempts().getFirst().id()).isEqualTo(paymentId);
            verify(paymentGateway, never()).fetchOrderStatus(anyString());
        }

        @Test
        @DisplayName("Active reconciliation: polls gateway when latest attempt is PENDING and has gatewayOrderId")
        void getPaymentByOrderId_activeReconciliation() {
            payment.setCurrentStatus(PaymentStatus.PENDING);
            payment.setGatewayOrderId("order_rzp_pending");
            when(paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId))
                    .thenReturn(List.of(payment));
            when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                    .thenReturn(List.of(payment));
            when(paymentGateway.fetchOrderStatus("order_rzp_pending"))
                    .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_reconciled"));
            when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

            PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

            assertThat(response.orderId()).isEqualTo(orderId);
            verify(paymentLifecycle).reconcilePaidPayment(payment.getId(), "pay_reconciled");
        }

        @Test
        @DisplayName("Prioritizes winning SUCCESS attempt over older or newer attempts")
        void getPaymentByOrderId_prioritizesWinner() {
            Payment attempt1 = new Payment();
            attempt1.setId(UUID.randomUUID());
            attempt1.setOrder(order);
            attempt1.setTransactionId("TXN-1");
            attempt1.setPaymentMethod(PaymentMethod.UPI);
            attempt1.setCurrentStatus(PaymentStatus.CANCELLED);
            attempt1.setAmount(BigDecimal.valueOf(100.00));

            Payment attempt2 = new Payment();
            attempt2.setId(UUID.randomUUID());
            attempt2.setOrder(order);
            attempt2.setTransactionId("TXN-2");
            attempt2.setPaymentMethod(PaymentMethod.CARD);
            attempt2.setCurrentStatus(PaymentStatus.SUCCESS);
            attempt2.setGatewayOrderId("order_rzp_winner");
            attempt2.setGatewayPaymentId("pay_winner");
            attempt2.setAmount(BigDecimal.valueOf(100.00));

            // Attempts returned ASC by createdAt (attempt1 older, attempt2 newer)
            when(paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId))
                    .thenReturn(List.of(attempt1, attempt2));

            PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

            assertThat(response.id()).isEqualTo(attempt2.getId());
            assertThat(response.status()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(response.attempts()).hasSize(2);
        }

        @Test
        @DisplayName("First Success Wins: When multiple attempts succeeded, earliest success is primary")
        void getPaymentByOrderId_firstSuccessWins() {
            Payment attempt1 = new Payment();
            attempt1.setId(UUID.randomUUID());
            attempt1.setOrder(order);
            attempt1.setTransactionId("TXN-1");
            attempt1.setPaymentMethod(PaymentMethod.UPI);
            attempt1.setCurrentStatus(PaymentStatus.SUCCESS);
            attempt1.setAmount(BigDecimal.valueOf(100.00));

            Payment attempt2 = new Payment();
            attempt2.setId(UUID.randomUUID());
            attempt2.setOrder(order);
            attempt2.setTransactionId("TXN-2");
            attempt2.setPaymentMethod(PaymentMethod.CARD);
            attempt2.setCurrentStatus(PaymentStatus.SUCCESS);
            attempt2.setAmount(BigDecimal.valueOf(100.00));

            // Sorted ASC by createdAt
            when(paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId))
                    .thenReturn(List.of(attempt1, attempt2));

            PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

            assertThat(response.id()).isEqualTo(attempt1.getId());
            assertThat(response.transactionId()).isEqualTo("TXN-1");
        }

        @Test
        @DisplayName("When none succeeded, falls back to the latest active attempt")
        void getPaymentByOrderId_fallbackToLatestAttempt() {
            Payment attempt1 = new Payment();
            attempt1.setId(UUID.randomUUID());
            attempt1.setOrder(order);
            attempt1.setTransactionId("TXN-1");
            attempt1.setPaymentMethod(PaymentMethod.UPI);
            attempt1.setCurrentStatus(PaymentStatus.FAILED);
            attempt1.setAmount(BigDecimal.valueOf(100.00));

            Payment attempt2 = new Payment();
            attempt2.setId(UUID.randomUUID());
            attempt2.setOrder(order);
            attempt2.setTransactionId("TXN-2");
            attempt2.setPaymentMethod(PaymentMethod.CARD);
            attempt2.setCurrentStatus(PaymentStatus.PENDING);
            attempt2.setAmount(BigDecimal.valueOf(100.00));

            // Sorted ASC by createdAt
            when(paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId))
                    .thenReturn(List.of(attempt1, attempt2));

            PaymentResponse response = paymentService.getPaymentByOrderId(orderId, customerId);

            // Latest attempt (attempt2) is returned
            assertThat(response.id()).isEqualTo(attempt2.getId());
            assertThat(response.transactionId()).isEqualTo("TXN-2");
        }

        @Test
        @DisplayName("Throws PaymentNotFoundException when no payments belong to customer")
        void getPaymentByOrderId_notFound() {
            when(paymentRepository.findAllByOrderIdAndCustomerId(orderId, customerId))
                    .thenReturn(List.of());

            assertThatThrownBy(() -> paymentService.getPaymentByOrderId(orderId, customerId))
                    .isInstanceOf(PaymentNotFoundException.class)
                    .hasMessageContaining("Payment not found for order");
        }
    }

    @Nested
    @DisplayName("Webhooks and Verification")
    class WebhookTests {

        @Test
        @DisplayName("handleStubOnlineWebhook delegates to paymentLifecycle")
        void handleStubOnlineWebhook_delegates() {
            paymentService.handleStubOnlineWebhook(transactionId, PaymentStatus.SUCCESS);

            verify(paymentLifecycle).processStubPayment(transactionId, PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("verifyOnlinePayment checks signature and delegates to paymentLifecycle")
        void verifyOnlinePayment_success() {
            paymentService.verifyOnlinePayment("order_rzp_1", "pay_rzp_1", "sig_rzp_1");

            verify(paymentGateway).verifyPaymentSignature("order_rzp_1", "pay_rzp_1", "sig_rzp_1");
            verify(paymentLifecycle).processOnlinePaymentSuccess("order_rzp_1", "pay_rzp_1");
        }

        @Test
        @DisplayName("handleRazorpayWebhook processes payment.captured event")
        void handleRazorpayWebhook_paymentCaptured() {
            String rawBody = "{\"event\":\"payment.captured\"}";
            String sig = "sig_test";
            RazorpayGatewayWebhookEvent event = new RazorpayGatewayWebhookEvent("payment.captured", "order_rzp_1", "pay_rzp_1", null);

            when(paymentGateway.parseWebhookEvent(rawBody)).thenReturn(event);

            paymentService.handleRazorpayWebhook(rawBody, sig);

            verify(paymentGateway).verifyWebhookSignature(rawBody, sig);
            verify(paymentLifecycle).processOnlinePaymentSuccess("order_rzp_1", "pay_rzp_1");
        }

        @Test
        @DisplayName("handleRazorpayWebhook processes payment.failed event")
        void handleRazorpayWebhook_paymentFailed() {
            String rawBody = "{\"event\":\"payment.failed\"}";
            String sig = "sig_test";
            RazorpayGatewayWebhookEvent event = new RazorpayGatewayWebhookEvent("payment.failed", "order_rzp_1", null, "Bank failure");

            when(paymentGateway.parseWebhookEvent(rawBody)).thenReturn(event);

            paymentService.handleRazorpayWebhook(rawBody, sig);

            verify(paymentGateway).verifyWebhookSignature(rawBody, sig);
            verify(paymentLifecycle).processOnlinePaymentFailed("order_rzp_1", "Bank failure");
        }

        @Test
        @DisplayName("cancelPendingPayments delegates to paymentLifecycle")
        void cancelPendingPayments_delegates() {
            paymentService.cancelPendingPayments(orderId, "Test cancellation");

            verify(paymentLifecycle).cancelPendingPayments(orderId, "Test cancellation");
        }
    }


    @Nested
    @DisplayName("reconcileAllPaymentAttempts")
    class ReconcileAllPaymentAttemptsTests {

        @Test
        @DisplayName("Reconciles single paid attempt and returns winning result")
        void singlePaid_returnsWinningResult() {
            payment.setGatewayOrderId("order_rzp_1");
            when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                    .thenReturn(List.of(payment));
            when(paymentGateway.fetchOrderStatus("order_rzp_1"))
                    .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_123"));
            when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

            Optional<PaymentResult> result = paymentService.reconcileAllPaymentAttempts(orderId);

            assertThat(result).isPresent();
            assertThat(result.get().status()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentLifecycle).reconcilePaidPayment(payment.getId(), "pay_123");
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

            // Attempts returned ASC by createdAt (attempt1 older, attempt2 newer)
            when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                    .thenReturn(List.of(attempt1, attempt2));
            when(paymentGateway.fetchOrderStatus("order_rzp_1"))
                    .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_111"));
            when(paymentGateway.fetchOrderStatus("order_rzp_2"))
                    .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_222"));
            when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

            Optional<PaymentResult> result = paymentService.reconcileAllPaymentAttempts(orderId);

            assertThat(result).isPresent();
            assertThat(result.get().paymentId()).isEqualTo(attempt1.getId());
            assertThat(result.get().status()).isEqualTo(PaymentStatus.SUCCESS);

            // Attempt 1 (first/earliest) reconciled as winner
            verify(paymentLifecycle).reconcilePaidPayment(attempt1.getId(), "pay_111");

            // Attempt 2 (subsequent duplicate) marked SUCCESS and auto-refunded
            verify(paymentLifecycle).reconcilePaidPayment(attempt2.getId(), "pay_222");
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

            // Sorted ASC
            when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                    .thenReturn(List.of(attempt1, attempt2));
            when(paymentGateway.fetchOrderStatus("order_rzp_2"))
                    .thenReturn(new GatewayOrderDetails(GatewayOrderStatus.PAID, "pay_222"));
            when(paymentGateway.getPublishableKey()).thenReturn("rzp_key");

            Optional<PaymentResult> result = paymentService.reconcileAllPaymentAttempts(orderId);

            assertThat(result).isPresent();
            assertThat(result.get().paymentId()).isEqualTo(attempt1.getId());

            // No gateway call for attempt1 (already SUCCESS in DB)
            verify(paymentGateway, never()).fetchOrderStatus("order_rzp_1");

            // Attempt 2 marked SUCCESS and refunded
            verify(paymentLifecycle).reconcilePaidPayment(attempt2.getId(), "pay_222");
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

            Optional<PaymentResult> result = paymentService.reconcileAllPaymentAttempts(orderId);

            assertThat(result).isPresent();
            assertThat(result.get().paymentId()).isEqualTo(row1.getId());

            // Only called ONCE for the shared gatewayOrderId
            verify(paymentGateway, times(1)).fetchOrderStatus("order_rzp_same");
            verify(paymentLifecycle, times(1)).reconcilePaidPayment(row1.getId(), "pay_winner");
            // Skipped duplicate pending row is cancelled to prevent orphan pending state
            verify(paymentLifecycle).reconcileExpiredPayment(row2.getId(), "Superseded by winning attempt");
            // Zero refund events — no self-refund!
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Returns empty when no attempts were paid and reconciles expired payments")
        void nonePaid_returnsEmpty() {
            payment.setGatewayOrderId("order_rzp_expired");
            payment.setCurrentStatus(PaymentStatus.PENDING);

            when(paymentRepository.findAttemptsForReconciliation(eq(orderId), any()))
                    .thenReturn(List.of(payment));
            when(paymentGateway.fetchOrderStatus("order_rzp_expired"))
                    .thenReturn(GatewayOrderDetails.of(GatewayOrderStatus.EXPIRED));

            Optional<PaymentResult> result = paymentService.reconcileAllPaymentAttempts(orderId);

            assertThat(result).isEmpty();
            verify(paymentLifecycle).reconcileExpiredPayment(payment.getId(), "Gateway order EXPIRED");
        }
    }
}
