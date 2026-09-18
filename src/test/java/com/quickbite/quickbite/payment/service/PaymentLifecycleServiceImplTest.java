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
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentLifecycleServiceImplTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentLifecycleServiceImpl paymentLifecycle;

    private UUID paymentId;
    private UUID orderId;
    private UUID customerId;
    private UUID restaurantId;
    private Payment payment;
    private Order order;
    private User customer;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        paymentId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        customerId = UUID.randomUUID();
        restaurantId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);
        customer.setName("John Doe");
        customer.setEmail("john@example.com");

        restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setName("Pizza Place");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(new BigDecimal("450.00"));
        order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(Instant.now());

        payment = new Payment();
        payment.setId(paymentId);
        payment.setOrder(order);
        payment.setPaymentMethod(PaymentMethod.UPI);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrentStatus(PaymentStatus.PENDING);
        payment.setTransactionId("TXN-12345");
        payment.setGatewayOrderId("order_rzp_123");
    }

    @Nested
    @DisplayName("createPendingPayment & updateGatewayOrder")
    class CreationTests {

        @Test
        @DisplayName("createPendingPayment with gatewayName saves payment and status history")
        void createPendingPayment_withGateway() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment created = paymentLifecycle.createPendingPayment(order, "TXN-999", PaymentMethod.UPI, "Razorpay");

            assertThat(created.getOrder()).isEqualTo(order);
            assertThat(created.getPaymentMethod()).isEqualTo(PaymentMethod.UPI);
            assertThat(created.getGatewayName()).isEqualTo("Razorpay");
            assertThat(created.getCurrentStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepository).save(any(Payment.class));
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));
        }

        @Test
        @DisplayName("createPendingPayment without gatewayName saves payment for COD")
        void createPendingPayment_cod() {
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment created = paymentLifecycle.createPendingPayment(order, "COD-111", PaymentMethod.COD);

            assertThat(created.getOrder()).isEqualTo(order);
            assertThat(created.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
            assertThat(created.getGatewayName()).isNull();
            assertThat(created.getCurrentStatus()).isEqualTo(PaymentStatus.PENDING);
            verify(paymentRepository).save(any(Payment.class));
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));
        }

        @Test
        @DisplayName("updateGatewayOrder updates gateway order ID")
        void updateGatewayOrder_success() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            when(paymentRepository.save(payment)).thenReturn(payment);

            Payment updated = paymentLifecycle.updateGatewayOrder(paymentId, new GatewayOrder("order_rzp_new", "key_123"));

            assertThat(updated.getGatewayOrderId()).isEqualTo("order_rzp_new");
            verify(paymentRepository).save(payment);
        }

        @Test
        @DisplayName("updateGatewayOrder throws PaymentNotFoundException when not found")
        void updateGatewayOrder_notFound() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentLifecycle.updateGatewayOrder(paymentId, new GatewayOrder("order_1", "k1")))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("processOnlinePaymentSuccess")
    class OnlinePaymentSuccessTests {

        @Test
        @DisplayName("Transitions payment to SUCCESS, publishes PaymentSucceededEvent and PaymentStatusChangedEvent")
        void processOnlinePaymentSuccess_orderAwaitingPayment() {
            when(paymentRepository.findByGatewayOrderIdForUpdate("order_rzp_123")).thenReturn(Optional.of(payment));

            paymentLifecycle.processOnlinePaymentSuccess("order_rzp_123", "pay_rzp_captured");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getGatewayPaymentId()).isEqualTo("pay_rzp_captured");

            verify(paymentRepository).save(payment);
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));

            ArgumentCaptor<PaymentSucceededEvent> succeededCaptor = ArgumentCaptor.forClass(PaymentSucceededEvent.class);
            verify(eventPublisher).publishEvent(succeededCaptor.capture());
            assertThat(succeededCaptor.getValue().paymentId()).isEqualTo(paymentId);
            assertThat(succeededCaptor.getValue().orderId()).isEqualTo(orderId);
            assertThat(succeededCaptor.getValue().gatewayPaymentId()).isEqualTo("pay_rzp_captured");

            ArgumentCaptor<PaymentStatusChangedEvent> statusCaptor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(statusCaptor.capture());
            assertThat(statusCaptor.getValue().newStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("Throws PaymentNotFoundException if gateway order not found")
        void processOnlinePaymentSuccess_notFound() {
            when(paymentRepository.findByGatewayOrderIdForUpdate("order_unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentLifecycle.processOnlinePaymentSuccess("order_unknown", "pay_1"))
                    .isInstanceOf(PaymentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("processOnlinePaymentFailed")
    class OnlinePaymentFailedTests {

        @Test
        @DisplayName("Transitions payment to FAILED, publishes PaymentFailedEvent and PaymentStatusChangedEvent")
        void processOnlinePaymentFailed_success() {
            when(paymentRepository.findByGatewayOrderIdForUpdate("order_rzp_123")).thenReturn(Optional.of(payment));

            paymentLifecycle.processOnlinePaymentFailed("order_rzp_123", "Insufficient funds");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.FAILED);

            verify(paymentRepository).save(payment);
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));

            ArgumentCaptor<PaymentFailedEvent> failedCaptor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
            verify(eventPublisher).publishEvent(failedCaptor.capture());
            assertThat(failedCaptor.getValue().paymentId()).isEqualTo(paymentId);
            assertThat(failedCaptor.getValue().orderId()).isEqualTo(orderId);
            assertThat(failedCaptor.getValue().reason()).isEqualTo("Insufficient funds");

            ArgumentCaptor<PaymentStatusChangedEvent> statusCaptor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(statusCaptor.capture());
            assertThat(statusCaptor.getValue().newStatus()).isEqualTo(PaymentStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("reconcile & cancellation")
    class ReconciliationTests {

        @Test
        @DisplayName("reconcilePaidPayment marks payment SUCCESS and publishes PaymentSucceededEvent")
        void reconcilePaidPayment_success() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.reconcilePaidPayment(paymentId, "pay_rzp_rec");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(payment.getGatewayPaymentId()).isEqualTo("pay_rzp_rec");

            verify(paymentRepository).save(payment);
            verify(eventPublisher).publishEvent(any(PaymentSucceededEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("reconcileExpiredPayment marks payment CANCELLED and publishes PaymentCancelledEvent")
        void reconcileExpiredPayment_success() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.reconcileExpiredPayment(paymentId, "Gateway order expired or closed");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.CANCELLED);

            verify(paymentRepository).save(payment);
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));
            verify(eventPublisher).publishEvent(any(PaymentCancelledEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("cancelPendingPayments cancels all pending payments for order")
        void cancelPendingPayments_success() {
            when(paymentRepository.findByOrderIdAndCurrentStatus(orderId, PaymentStatus.PENDING))
                    .thenReturn(List.of(payment));

            paymentLifecycle.cancelPendingPayments(orderId, "Customer cancelled order");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.CANCELLED);

            verify(paymentRepository).save(payment);
            verify(paymentStatusHistoryRepository).save(any(PaymentStatusHistory.class));
            verify(eventPublisher).publishEvent(any(PaymentCancelledEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }
    }

    @Nested
    @DisplayName("markSuccess, markFailed, markCancelled, markRefunded & markRefundFailed")
    class ExplicitStatusTests {

        @Test
        @DisplayName("markSuccess updates status to SUCCESS and publishes events")
        void markSuccess_success() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markSuccess(paymentId, "pay_explicit");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentRepository).save(payment);
            verify(eventPublisher).publishEvent(any(PaymentSucceededEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("markCancelled updates status to CANCELLED and publishes events")
        void markCancelled_success() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markCancelled(paymentId, "User action");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.CANCELLED);
            verify(paymentRepository).save(payment);
            verify(eventPublisher).publishEvent(any(PaymentCancelledEvent.class));
            verify(eventPublisher).publishEvent(any(PaymentStatusChangedEvent.class));
        }

        @Test
        @DisplayName("markRefunded updates status to REFUNDED and publishes event")
        void markRefunded_success() {
            payment.setCurrentStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markRefunded(paymentId, "Customer request");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(paymentRepository).save(payment);

            ArgumentCaptor<PaymentStatusChangedEvent> captor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().newStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("markRefundFailed updates status to REFUND_FAILED and publishes event")
        void markRefundFailed_success() {
            payment.setCurrentStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markRefundFailed(paymentId, "Gateway error: account invalid");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);
            verify(paymentRepository).save(payment);

            ArgumentCaptor<PaymentStatusChangedEvent> captor = ArgumentCaptor.forClass(PaymentStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().newStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);
        }

        @Test
        @DisplayName("markFailed marks payment as FAILED")
        void markFailed_success() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markFailed(paymentId, "Gateway error");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.FAILED);
            verify(paymentRepository).save(payment);
            verify(eventPublisher).publishEvent(any(PaymentFailedEvent.class));
        }
    }

    @Nested
    @DisplayName("Guards & Edge cases")
    class GuardTests {

        @Test
        @DisplayName("Idempotency: duplicate transition to same status does nothing")
        void idempotency_sameStatus() {
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.reconcileExpiredPayment(paymentId, "Already cancelled");
            reset(paymentRepository, eventPublisher);

            // Re-apply same CANCELLED status
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));
            paymentLifecycle.reconcileExpiredPayment(paymentId, "Already cancelled");

            verify(paymentRepository, never()).save(payment);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Terminal state guard: ignores FAILED transition if already SUCCESS")
        void terminalState_ignoreFailedAfterSuccess() {
            payment.setCurrentStatus(PaymentStatus.SUCCESS);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markFailed(paymentId, "Late error");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentRepository, never()).save(payment);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Terminal state guard: does not overwrite REFUNDED with SUCCESS")
        void terminalState_neverOverwriteRefundedWithSuccess() {
            payment.setCurrentStatus(PaymentStatus.REFUNDED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markSuccess(paymentId, "Late success");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.REFUNDED);
            verify(paymentRepository, never()).save(payment);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("Terminal state guard: does not overwrite REFUND_FAILED with SUCCESS")
        void terminalState_neverOverwriteRefundFailedWithSuccess() {
            payment.setCurrentStatus(PaymentStatus.REFUND_FAILED);
            when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

            paymentLifecycle.markSuccess(paymentId, "Late success");

            assertThat(payment.getCurrentStatus()).isEqualTo(PaymentStatus.REFUND_FAILED);
            verify(paymentRepository, never()).save(payment);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        @DisplayName("processStubPayment with invalid status throws BadRequestException")
        void processStubPayment_invalidStatus() {
            when(paymentRepository.findByTransactionId("TXN-123")).thenReturn(Optional.of(payment));

            assertThatThrownBy(() -> paymentLifecycle.processStubPayment("TXN-123", PaymentStatus.PENDING))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
