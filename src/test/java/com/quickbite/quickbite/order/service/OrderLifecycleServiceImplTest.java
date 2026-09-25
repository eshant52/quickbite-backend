package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.event.order.OrderCancelledEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.order.exception.OrderExpiredException;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderItemRepository;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private PaymentProcessingService paymentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OrderLifecycleServiceImpl lifecycleService;

    private User customer;
    private Restaurant restaurant;
    private Order order;
    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        lifecycleService = new OrderLifecycleServiceImpl(
                orderRepository,
                orderItemRepository,
                orderStatusHistoryRepository,
                paymentService,
                eventPublisher,
                new com.quickbite.quickbite.common.config.property.OrderProperties(15, 100, "0 */5 * * * *", 5)
        );

        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(BigDecimal.valueOf(250.00));
        order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("prepareOrderForRetry")
    class PrepareOrderForRetryTests {

        @Test
        @DisplayName("Throws OrderNotFoundException when order does not exist")
        void orderNotFound() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lifecycleService.prepareOrderForRetry(customerId, orderId))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Throws OrderNotFoundException when customer ID does not match")
        void customerMismatch() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            UUID otherCustomerId = UUID.randomUUID();
            assertThatThrownBy(() -> lifecycleService.prepareOrderForRetry(otherCustomerId, orderId))
                    .isInstanceOf(OrderNotFoundException.class)
                    .hasMessageContaining("Order not found");
        }

        @Test
        @DisplayName("Throws BadRequestException when order status is not eligible for retry")
        void ineligibleStatus() {
            order.setCurrentStatus(OrderStatus.DELIVERED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> lifecycleService.prepareOrderForRetry(customerId, orderId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be retried");
        }

        @Test
        @DisplayName("Throws BadRequestException when order status is already ABANDONED")
        void abandonedStatus_throwsBadRequestException() {
            order.setCurrentStatus(OrderStatus.ABANDONED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> lifecycleService.prepareOrderForRetry(customerId, orderId))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be retried");

            verify(orderRepository, never()).save(order);
            verify(paymentService, never()).cancelPendingPayments(any(), any());
        }

        @Test
        @DisplayName("Returns locked order when eligible and within TTL (AWAITING_PAYMENT)")
        void success_awaitingPayment() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            Order result = lifecycleService.prepareOrderForRetry(customerId, orderId);

            assertThat(result).isSameAs(order);
            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
        }

        @Test
        @DisplayName("Returns locked order when eligible and within TTL (PAYMENT_FAILED)")
        void success_paymentFailed() {
            order.setCurrentStatus(OrderStatus.PAYMENT_FAILED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            Order result = lifecycleService.prepareOrderForRetry(customerId, orderId);

            assertThat(result).isSameAs(order);
            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
        }
    }

    @Nested
    @DisplayName("resetOrderStatusForRetry")
    class ResetOrderStatusForRetryTests {

        @Test
        @DisplayName("Throws OrderNotFoundException when order does not exist")
        void orderNotFound() {
            when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> lifecycleService.resetOrderStatusForRetry(orderId))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("Transitions PAYMENT_FAILED to AWAITING_PAYMENT")
        void transitionsFailedToAwaiting() {
            order.setCurrentStatus(OrderStatus.PAYMENT_FAILED);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            lifecycleService.resetOrderStatusForRetry(orderId);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        }

        @Test
        @DisplayName("Does nothing when order is already in AWAITING_PAYMENT")
        void alreadyAwaiting_noOp() {
            order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            lifecycleService.resetOrderStatusForRetry(orderId);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
            verify(orderRepository, never()).save(any());
            verify(orderStatusHistoryRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("abandonOrder")
    class AbandonOrderTests {

        @Test
        @DisplayName("Sets ABANDONED, saves history, cancels pending payment, and fires event")
        void abandonOrder_success() {
            lifecycleService.abandonOrder(order, "Test reason");

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.ABANDONED);
            verify(orderRepository).save(order);

            ArgumentCaptor<OrderStatusHistory> historyCaptor = ArgumentCaptor.forClass(OrderStatusHistory.class);
            verify(orderStatusHistoryRepository).save(historyCaptor.capture());
            assertThat(historyCaptor.getValue().getOrderStatus()).isEqualTo(OrderStatus.ABANDONED);

            verify(paymentService).cancelPendingPayments(order.getId(), "Test reason");

            ArgumentCaptor<OrderStatusChangedEvent> eventCaptor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().previousStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
            assertThat(eventCaptor.getValue().newStatus()).isEqualTo(OrderStatus.ABANDONED);
        }

        @Test
        @DisplayName("Idempotency: no-op when order is already in terminal state")
        void abandonOrder_alreadyTerminal_isNoOp() {
            order.setCurrentStatus(OrderStatus.ABANDONED);

            lifecycleService.abandonOrder(order, "Test reason");

            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("abandonOrderById")
    class AbandonOrderByIdTests {

        @Test
        @DisplayName("Abandons order when found and not in terminal state")
        void abandonOrderById_success() {
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            lifecycleService.abandonOrderById(orderId, "Payment window expired");

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.ABANDONED);
            verify(orderRepository).save(order);
            verify(paymentService).cancelPendingPayments(orderId, "Payment window expired");
        }

        @Test
        @DisplayName("Skips abandonment when order is already in terminal state")
        void abandonOrderById_alreadyTerminal_skips() {
            order.setCurrentStatus(OrderStatus.ABANDONED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

            lifecycleService.abandonOrderById(orderId, "Payment window expired");

            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Cancels PLACED order successfully")
        void cancelOrder_placed() {
            order.setCurrentStatus(OrderStatus.PLACED);

            lifecycleService.cancelOrder(order);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verify(paymentService).cancelPendingPayments(order.getId(), "Order cancelled by customer");
            verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("Cancels AWAITING_PAYMENT order successfully")
        void cancelOrder_awaitingPayment() {
            order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);

            lifecycleService.cancelOrder(order);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verify(paymentService).cancelPendingPayments(order.getId(), "Order cancelled by customer");
            verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("Idempotency: no-op when order is already cancelled")
        void cancelOrder_alreadyCancelled_isNoOp() {
            order.setCurrentStatus(OrderStatus.CANCELLED);

            lifecycleService.cancelOrder(order);

            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Cancels PAYMENT_FAILED order successfully")
        void cancelOrder_paymentFailed() {
            order.setCurrentStatus(OrderStatus.PAYMENT_FAILED);

            lifecycleService.cancelOrder(order);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verify(paymentService).cancelPendingPayments(order.getId(), "Order cancelled by customer");
            verify(eventPublisher).publishEvent(any(OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("Throws OrderStateException when order is already accepted by restaurant")
        void cancelOrder_alreadyAccepted() {
            order.setCurrentStatus(OrderStatus.ACCEPTED);

            assertThatThrownBy(() -> lifecycleService.cancelOrder(order))
                    .isInstanceOf(OrderStateException.class)
                    .hasMessageContaining("cannot be cancelled");
        }
    }

    @Nested
    @DisplayName("transitionStatus")
    class TransitionStatusTests {

        @Test
        @DisplayName("Successfully transitions status when current matches expected")
        void transitionStatus_success() {
            order.setCurrentStatus(OrderStatus.PLACED);
            when(orderRepository.save(order)).thenReturn(order);

            Order result = lifecycleService.transitionStatus(order, OrderStatus.PLACED, OrderStatus.ACCEPTED);

            assertThat(result.getCurrentStatus()).isEqualTo(OrderStatus.ACCEPTED);
            verify(orderRepository).save(order);
            verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
            verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Throws OrderStateException when current status does not match expected")
        void transitionStatus_mismatch() {
            order.setCurrentStatus(OrderStatus.PLACED);

            assertThatThrownBy(() -> lifecycleService.transitionStatus(order, OrderStatus.ACCEPTED, OrderStatus.PREPARING))
                    .isInstanceOf(OrderStateException.class)
                    .hasMessageContaining("Cannot transition order");
        }
    }

    @Nested
    @DisplayName("processAbandonmentBatch")
    class ProcessAbandonmentBatchTests {

        @Test
        @DisplayName("Successfully locks and abandons batch of stale orders")
        void processAbandonmentBatch_success() {
            Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
            Order order1 = new Order();
            order1.setId(UUID.randomUUID());
            order1.setCustomer(customer);
            order1.setRestaurant(restaurant);
            order1.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);

            Order order2 = new Order();
            order2.setId(UUID.randomUUID());
            order2.setCustomer(customer);
            order2.setRestaurant(restaurant);
            order2.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);

            when(orderRepository.findByCurrentStatusAndCreatedAtBeforeForUpdateSkipLocked(
                    eq(OrderStatus.AWAITING_PAYMENT), eq(cutoff), eq(Limit.of(100))))
                    .thenReturn(List.of(order1, order2));

            int processed = lifecycleService.processAbandonmentBatch(cutoff, 100);

            assertThat(processed).isEqualTo(2);
            assertThat(order1.getCurrentStatus()).isEqualTo(OrderStatus.ABANDONED);
            assertThat(order2.getCurrentStatus()).isEqualTo(OrderStatus.ABANDONED);
            verify(orderRepository).save(order1);
            verify(orderRepository).save(order2);
            verify(paymentService).cancelPendingPayments(order1.getId(), "Payment window expired (auto-abandoned)");
            verify(paymentService).cancelPendingPayments(order2.getId(), "Payment window expired (auto-abandoned)");
            verify(eventPublisher, times(2)).publishEvent(any(OrderStatusChangedEvent.class));
        }

        @Test
        @DisplayName("Returns 0 when no stale orders found")
        void processAbandonmentBatch_empty() {
            Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
            when(orderRepository.findByCurrentStatusAndCreatedAtBeforeForUpdateSkipLocked(
                    eq(OrderStatus.AWAITING_PAYMENT), eq(cutoff), eq(Limit.of(100))))
                    .thenReturn(List.of());

            int processed = lifecycleService.processAbandonmentBatch(cutoff, 100);

            assertThat(processed).isEqualTo(0);
            verify(orderRepository, never()).save(any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
        }

        @Test
        @DisplayName("Continues batch processing if one order throws exception")
        void processAbandonmentBatch_partialFailure() {
            Instant cutoff = Instant.now().minus(15, ChronoUnit.MINUTES);
            Order order1 = new Order();
            order1.setId(UUID.randomUUID());
            order1.setCustomer(customer);
            order1.setRestaurant(restaurant);
            order1.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);

            Order order2 = new Order();
            order2.setId(UUID.randomUUID());
            order2.setCustomer(customer);
            order2.setRestaurant(restaurant);
            order2.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);

            when(orderRepository.findByCurrentStatusAndCreatedAtBeforeForUpdateSkipLocked(
                    eq(OrderStatus.AWAITING_PAYMENT), eq(cutoff), eq(Limit.of(100))))
                    .thenReturn(List.of(order1, order2));

            when(orderRepository.save(order1)).thenThrow(new RuntimeException("DB error on order 1"));
            when(orderRepository.save(order2)).thenReturn(order2);

            int processed = lifecycleService.processAbandonmentBatch(cutoff, 100);

            assertThat(processed).isEqualTo(2);
            verify(orderRepository).save(order1);
            verify(orderRepository).save(order2);
            assertThat(order2.getCurrentStatus()).isEqualTo(OrderStatus.ABANDONED);
        }
    }

    @Nested
    @DisplayName("Restaurant acceptance timeout and dispatch exhaustion")
    class RestaurantAcceptanceTimeoutAndExhaustionTests {

        @Test
        @DisplayName("processRestaurantAcceptanceTimeoutBatch cancels expired PLACED orders and refunds payment")
        void processRestaurantAcceptanceTimeoutBatch_success() {
            Instant now = Instant.now();
            order.setCurrentStatus(OrderStatus.PLACED);
            when(orderRepository.findByCurrentStatusAndRestaurantAcceptanceDeadlineBeforeForUpdateSkipLocked(
                    eq(OrderStatus.PLACED), eq(now), eq(Limit.of(50))))
                    .thenReturn(List.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            int processed = lifecycleService.processRestaurantAcceptanceTimeoutBatch(now, 50);

            assertThat(processed).isEqualTo(1);
            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancellationReason()).isEqualTo(com.quickbite.quickbite.order.model.OrderCancellationReason.RESTAURANT_UNRESPONSIVE);
            verify(paymentService).refundSuccessfulPayment(eq(order.getId()), anyString());
            verify(paymentService).cancelPendingPayments(eq(order.getId()), anyString());
            verify(eventPublisher).publishEvent(any(com.quickbite.quickbite.common.event.order.OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("cancelDueToNoDeliveryAgent cancels order with NO_AGENT_FOUND and refunds payment")
        void cancelDueToNoDeliveryAgent_success() {
            order.setCurrentStatus(OrderStatus.ACCEPTED);
            when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));
            when(orderRepository.save(order)).thenReturn(order);

            lifecycleService.cancelDueToNoDeliveryAgent(orderId);

            assertThat(order.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancellationReason()).isEqualTo(com.quickbite.quickbite.order.model.OrderCancellationReason.NO_AGENT_FOUND);
            verify(paymentService).refundSuccessfulPayment(eq(order.getId()), anyString());
            verify(paymentService).cancelPendingPayments(eq(order.getId()), anyString());
            verify(eventPublisher).publishEvent(any(com.quickbite.quickbite.common.event.order.OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("processRestaurantAcceptanceTimeoutBatch continues processing remaining orders when one throws (Fix C4)")
        void processRestaurantAcceptanceTimeoutBatch_continuesOnSingleFailure() {
            Instant now = Instant.now();
            Order order1 = new Order();
            order1.setId(UUID.randomUUID());
            order1.setCustomer(order.getCustomer());
            order1.setRestaurant(order.getRestaurant());
            order1.setCurrentStatus(OrderStatus.PLACED);

            Order order2 = new Order();
            order2.setId(UUID.randomUUID());
            order2.setCustomer(order.getCustomer());
            order2.setRestaurant(order.getRestaurant());
            order2.setCurrentStatus(OrderStatus.PLACED);

            when(orderRepository.findByCurrentStatusAndRestaurantAcceptanceDeadlineBeforeForUpdateSkipLocked(
                    eq(OrderStatus.PLACED), eq(now), eq(Limit.of(50))))
                    .thenReturn(List.of(order1, order2));
            when(orderRepository.save(order1)).thenThrow(new RuntimeException("DB error on order1"));
            when(orderRepository.save(order2)).thenReturn(order2);

            int processed = lifecycleService.processRestaurantAcceptanceTimeoutBatch(now, 50);

            assertThat(processed).isEqualTo(2);
            verify(orderRepository).save(order1);
            verify(orderRepository).save(order2);
            assertThat(order2.getCurrentStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }
}
