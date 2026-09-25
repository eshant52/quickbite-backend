package com.quickbite.quickbite.order.listener;

import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentCancelledEvent;
import com.quickbite.quickbite.common.event.payment.PaymentFailedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentRefundRequestedEvent;
import com.quickbite.quickbite.common.event.payment.PaymentSucceededEvent;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.payment.model.PaymentMethod;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderStatusHistoryRepository orderStatusHistoryRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private com.quickbite.quickbite.common.config.property.OrderProperties orderProperties;

    @InjectMocks
    private PaymentEventListener listener;

    private UUID orderId;
    private UUID paymentId;
    private Order order;
    private User customer;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(orderProperties.restaurantAcceptanceWindow()).thenReturn(java.time.Duration.ofMinutes(2));

        orderId = UUID.randomUUID();
        paymentId = UUID.randomUUID();

        customer = new User();
        customer.setId(UUID.randomUUID());
        customer.setName("John Customer");
        customer.setEmail("john@example.com");

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Pasta Palace");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(new BigDecimal("350.00"));
        order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("handlePaymentSucceeded")
    class PaymentSucceededTests {

        @Test
        @DisplayName("When order is AWAITING_PAYMENT, transitions to PLACED and emits OrderPlacedEvent")
        void orderAwaitingPayment_transitionsToPlaced() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentId, orderId, PaymentMethod.UPI, "pay_rzp_123", new BigDecimal("350.00"));

            listener.handlePaymentSucceeded(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.PLACED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));

            ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().orderId()).isEqualTo(orderId);
            assertThat(eventCaptor.getValue().customerName()).isEqualTo("John Customer");
        }

        @Test
        @DisplayName("When order is ABANDONED, late payment triggers auto-refund request")
        void orderAbandoned_triggersAutoRefund() {
            order.setCurrentStatus(OrderStatus.ABANDONED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentId, orderId, PaymentMethod.UPI, "pay_rzp_late", new BigDecimal("350.00"));

            listener.handlePaymentSucceeded(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.ABANDONED); // unchanged
            verify(orderRepository, never()).save(order);
            verify(eventPublisher, never()).publishEvent(any(OrderPlacedEvent.class));

            ArgumentCaptor<PaymentRefundRequestedEvent> refundCaptor =
                    ArgumentCaptor.forClass(PaymentRefundRequestedEvent.class);
            verify(eventPublisher).publishEvent(refundCaptor.capture());
            assertThat(refundCaptor.getValue().paymentId()).isEqualTo(paymentId);
            assertThat(refundCaptor.getValue().gatewayPaymentId()).isEqualTo("pay_rzp_late");
            assertThat(refundCaptor.getValue().reason()).contains("ABANDONED");
        }

        @Test
        @DisplayName("When order is CANCELLED, late payment triggers auto-refund request")
        void orderCancelled_triggersAutoRefund() {
            order.setCurrentStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentId, orderId, PaymentMethod.CARD, "pay_rzp_late2", new BigDecimal("350.00"));

            listener.handlePaymentSucceeded(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository, never()).save(order);

            ArgumentCaptor<PaymentRefundRequestedEvent> refundCaptor =
                    ArgumentCaptor.forClass(PaymentRefundRequestedEvent.class);
            verify(eventPublisher).publishEvent(refundCaptor.capture());
            assertThat(refundCaptor.getValue().reason()).contains("CANCELLED");
        }

        @Test
        @DisplayName("When order is ABANDONED but payment has no gateway capture ID, auto-refund is skipped")
        void orderAbandoned_noGatewayId_skipsRefund() {
            order.setCurrentStatus(OrderStatus.ABANDONED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentId, orderId, PaymentMethod.COD, null, new BigDecimal("350.00"));

            listener.handlePaymentSucceeded(event);

            verify(eventPublisher, never()).publishEvent(any(PaymentRefundRequestedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(OrderPlacedEvent.class));
        }

        @Test
        @DisplayName("When order is already PLACED, no-op")
        void orderAlreadyPlaced_noOp() {
            order.setCurrentStatus(OrderStatus.PLACED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentId, orderId, PaymentMethod.UPI, "pay_rzp_dup", new BigDecimal("350.00"));

            listener.handlePaymentSucceeded(event);

            verify(orderRepository, never()).save(order);
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Throws OrderNotFoundException when order does not exist")
        void orderNotFound_throwsException() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            PaymentSucceededEvent event = new PaymentSucceededEvent(
                    paymentId, orderId, PaymentMethod.UPI, "pay_1", new BigDecimal("100.00"));

            assertThatThrownBy(() -> listener.handlePaymentSucceeded(event))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("handlePaymentFailed")
    class PaymentFailedTests {

        @Test
        @DisplayName("When order is AWAITING_PAYMENT, transitions to PAYMENT_FAILED")
        void orderAwaitingPayment_transitionsToPaymentFailed() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentFailedEvent event = new PaymentFailedEvent(paymentId, orderId, "Card declined");

            listener.handlePaymentFailed(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        }

        @Test
        @DisplayName("When order is already PLACED, does not modify order")
        void orderAlreadyPlaced_noOp() {
            order.setCurrentStatus(OrderStatus.PLACED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentFailedEvent event = new PaymentFailedEvent(paymentId, orderId, "Late failure");

            listener.handlePaymentFailed(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.PLACED);
            verify(orderRepository, never()).save(order);
            verify(orderStatusHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("handlePaymentCancelled")
    class PaymentCancelledTests {

        @Test
        @DisplayName("When order is AWAITING_PAYMENT, transitions to PAYMENT_FAILED (fixes issue #14)")
        void orderAwaitingPayment_transitionsToPaymentFailed() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentCancelledEvent event = new PaymentCancelledEvent(
                    paymentId, orderId, "Gateway order expired");

            listener.handlePaymentCancelled(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        }

        @Test
        @DisplayName("When order is already CANCELLED, does not modify order")
        void orderAlreadyCancelled_noOp() {
            order.setCurrentStatus(OrderStatus.CANCELLED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            PaymentCancelledEvent event = new PaymentCancelledEvent(
                    paymentId, orderId, "Already cancelled");

            listener.handlePaymentCancelled(event);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository, never()).save(order);
            verify(orderStatusHistoryRepository, never()).save(any());
        }
    }
}
