package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.delivery.service.DeliveryAssignmentService;
import com.quickbite.quickbite.payment.dto.OnlinePaymentResult;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.order.exception.OrderExpiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @Mock private OrderCreationService orderCreationService;
    @Mock private OrderLifecycleService orderLifecycleService;
    @Mock private PaymentProcessingService paymentService;
    @Mock private DeliveryAssignmentService deliveryAssignmentService;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;
    @Mock private RBucket<String> rBucket;

    private OrderServiceImpl orderService;

    private User customer;
    private Restaurant restaurant;
    private Order order;
    private UUID customerId;
    private UUID orderId;

    @BeforeEach
    void setUp() throws InterruptedException {
        // Default: Redisson lock & bucket are always acquired for tests
        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(rLock.isHeldByCurrentThread()).thenReturn(true);
        lenient().when(redissonClient.<String>getBucket(anyString())).thenReturn(rBucket);
        lenient().when(rBucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(true);

        orderService = new OrderServiceImpl(
                orderRepository,
                userRepository,
                restaurantRepository,
                orderCreationService,
                orderLifecycleService,
                paymentService,
                deliveryAssignmentService,
                redissonClient,
                new com.quickbite.quickbite.common.config.property.OrderProperties(15, 100, "0 */5 * * * *", 5)
        );

        customerId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        customer = new User();
        customer.setId(customerId);
        customer.setName("Test User");

        restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setName("Test Restaurant");

        order = new Order();
        order.setId(orderId);
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setTotalAmount(BigDecimal.valueOf(350.00));
        order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
        order.setCreatedAt(Instant.now());
        order.setItems(List.of());
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrderTests {

        @Test
        @DisplayName("Acquires 5s customer cooldown, creates order, and initiates payment without abandoning prior orders")
        void placeOrder_success() {
            PlaceOrderRequest req = new PlaceOrderRequest(UUID.randomUUID(), PaymentMethod.UPI, null);
            when(orderCreationService.createOrderWithItems(customerId, req)).thenReturn(order);
            PaymentResult expectedResult = new OnlinePaymentResult(
                    UUID.randomUUID(), order.getId(), "TXN-1", PaymentMethod.UPI, PaymentStatus.PENDING,
                    order.getTotalAmount(), "order_rzp1", "key_test"
            );
            when(paymentService.initiatePayment(order, PaymentMethod.UPI)).thenReturn(expectedResult);

            PaymentResult result = orderService.placeOrder(customerId, req);

            assertThat(result).isEqualTo(expectedResult);
            verify(rBucket).setIfAbsent(eq("LOCKED"), eq(Duration.ofSeconds(5)));
            verify(orderCreationService).createOrderWithItems(customerId, req);
            verify(paymentService).initiatePayment(order, PaymentMethod.UPI);
            verify(rBucket, never()).delete();
        }

        @Test
        @DisplayName("Throws BadRequestException when customer cooldown is active (frequent order placement)")
        void placeOrder_cooldownActive_throwsBadRequestException() {
            PlaceOrderRequest req = new PlaceOrderRequest(UUID.randomUUID(), PaymentMethod.UPI, null);
            when(rBucket.setIfAbsent(anyString(), any(Duration.class))).thenReturn(false);

            assertThatThrownBy(() -> orderService.placeOrder(customerId, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("recently submitted");

            verify(orderCreationService, never()).createOrderWithItems(any(), any());
            verify(paymentService, never()).initiatePayment(any(), any());
        }

        @Test
        @DisplayName("Releases cooldown key when order creation throws exception")
        void placeOrder_creationFails_releasesCooldown() {
            PlaceOrderRequest req = new PlaceOrderRequest(UUID.randomUUID(), PaymentMethod.UPI, null);
            when(orderCreationService.createOrderWithItems(customerId, req))
                    .thenThrow(new BadRequestException("Cart is empty"));

            assertThatThrownBy(() -> orderService.placeOrder(customerId, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Cart is empty");

            verify(rBucket).delete();
        }
    }

    @Nested
    @DisplayName("retryPayment")
    class RetryPaymentTests {

        @Test
        @DisplayName("Returns reconciled winning result directly when payment was captured at gateway")
        void retryPayment_reconciledSuccess() {
            when(orderLifecycleService.prepareOrderForRetry(customerId, orderId)).thenReturn(order);

            OnlinePaymentResult reconciledSuccess = new OnlinePaymentResult(
                    UUID.randomUUID(), orderId, "TXN-RECONCILED", PaymentMethod.UPI, PaymentStatus.SUCCESS,
                    order.getTotalAmount(), "order_existing", "key_test"
            );
            when(paymentService.reconcileAllPaymentAttempts(orderId))
                    .thenReturn(Optional.of(reconciledSuccess));

            PaymentResult result = orderService.retryPayment(customerId, orderId, PaymentMethod.UPI);

            assertThat(result).isEqualTo(reconciledSuccess);
            assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
            verify(paymentService, never()).initiatePayment(any(), any());
            verify(paymentService, never()).cancelPendingPayments(any(), any());
        }

        @Test
        @DisplayName("Creates new payment attempt when order is unpaid and within 15-min TTL")
        void retryPayment_freshAttemptWhenUnpaidAndWithinTtl() {
            order.setCreatedAt(Instant.now());
            when(orderLifecycleService.prepareOrderForRetry(customerId, orderId)).thenReturn(order);
            when(paymentService.reconcileAllPaymentAttempts(orderId)).thenReturn(Optional.empty());
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OnlinePaymentResult newResult = new OnlinePaymentResult(
                    UUID.randomUUID(), orderId, "TXN-NEW", PaymentMethod.UPI, PaymentStatus.PENDING,
                    order.getTotalAmount(), "order_new", "key_test"
            );
            when(paymentService.initiatePayment(order, PaymentMethod.UPI)).thenReturn(newResult);

            PaymentResult result = orderService.retryPayment(customerId, orderId, PaymentMethod.UPI);

            assertThat(result).isEqualTo(newResult);
            verify(orderLifecycleService).prepareOrderForRetry(customerId, orderId);
            verify(orderLifecycleService).resetOrderStatusForRetry(orderId);
            verify(paymentService).initiatePayment(order, PaymentMethod.UPI);
        }

        @Test
        @DisplayName("Resets order status for retry before initiating payment")
        void retryPayment_resetsStatusBeforeInitiation() {
            order.setCreatedAt(Instant.now());
            when(orderLifecycleService.prepareOrderForRetry(customerId, orderId)).thenReturn(order);
            when(paymentService.reconcileAllPaymentAttempts(orderId)).thenReturn(Optional.empty());
            when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

            OnlinePaymentResult newResult = new OnlinePaymentResult(
                    UUID.randomUUID(), orderId, "TXN-CARD", PaymentMethod.CARD, PaymentStatus.PENDING,
                    order.getTotalAmount(), "order_card", "key_test"
            );
            when(paymentService.initiatePayment(order, PaymentMethod.CARD)).thenReturn(newResult);

            PaymentResult result = orderService.retryPayment(customerId, orderId, PaymentMethod.CARD);

            assertThat(result).isEqualTo(newResult);
            verify(orderLifecycleService).resetOrderStatusForRetry(orderId);
            verify(paymentService).initiatePayment(order, PaymentMethod.CARD);
        }

        @Test
        @DisplayName("Throws BadRequestException when lock cannot be acquired (concurrent retry)")
        void retryPayment_lockNotAcquired_throwsBadRequestException() throws InterruptedException {
            when(rLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

            assertThatThrownBy(() -> orderService.retryPayment(customerId, orderId, PaymentMethod.UPI))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already in progress");

            verify(orderLifecycleService, never()).prepareOrderForRetry(any(), any());
            verify(paymentService, never()).initiatePayment(any(), any());
        }

        @Test
        @DisplayName("Abandons order and throws BadRequestException when order is unpaid and past 15-min TTL")
        void retryPayment_unpaidAndTtlExpired_abandonsOrder() {
            order.setCreatedAt(Instant.now().minus(Duration.ofMinutes(16)));
            when(orderLifecycleService.prepareOrderForRetry(customerId, orderId)).thenReturn(order);
            when(paymentService.reconcileAllPaymentAttempts(orderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.retryPayment(customerId, orderId, PaymentMethod.UPI))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("expired");

            verify(orderLifecycleService).abandonOrderById(orderId, "Payment window expired");
            verify(paymentService, never()).initiatePayment(any(), any());
        }

        @Test
        @DisplayName("Throws BadRequestException when order is in terminal state ABANDONED")
        void retryPayment_alreadyAbandoned_throwsBadRequestException() {
            when(orderLifecycleService.prepareOrderForRetry(customerId, orderId))
                    .thenThrow(new BadRequestException("Order is in terminal state ABANDONED and cannot be retried."));

            assertThatThrownBy(() -> orderService.retryPayment(customerId, orderId, PaymentMethod.UPI))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("cannot be retried");

            verify(paymentService, never()).initiatePayment(any(), any());
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrderTests {

        @Test
        @DisplayName("Delegates order cancellation to OrderLifecycleService")
        void cancelOrder_delegatesToLifecycleService() {
            when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
            when(orderRepository.findByIdAndCustomerId(orderId, customerId)).thenReturn(Optional.of(order));

            orderService.cancelOrder(customerId, orderId);

            verify(orderLifecycleService).cancelOrder(order);
        }
    }

    @Nested
    @DisplayName("Restaurant Operations")
    class RestaurantOperationsTests {

        private UUID restaurantId;
        private UUID ownerId;
        private User owner;

        @BeforeEach
        void init() {
            restaurantId = UUID.randomUUID();
            ownerId = UUID.randomUUID();
            owner = new User();
            owner.setId(ownerId);

            when(userRepository.findById(ownerId)).thenReturn(Optional.of(owner));
            when(restaurantRepository.findByIdAndOwner(restaurantId, owner)).thenReturn(Optional.of(restaurant));
            when(orderRepository.findByIdAndRestaurantId(orderId, restaurant.getId())).thenReturn(Optional.of(order));
        }

        @Test
        @DisplayName("acceptOrder delegates status transition to lifecycle service")
        void acceptOrder_success() {
            order.setCurrentStatus(OrderStatus.ACCEPTED);
            when(orderLifecycleService.transitionStatus(order, OrderStatus.PLACED, OrderStatus.ACCEPTED))
                    .thenReturn(order);

            OrderResponse response = orderService.acceptOrder(orderId, restaurantId, ownerId);

            assertThat(response.currentStatus()).isEqualTo(OrderStatus.ACCEPTED);
            verify(orderLifecycleService).transitionStatus(order, OrderStatus.PLACED, OrderStatus.ACCEPTED);
        }

        @Test
        @DisplayName("declineOrder delegates status transition to lifecycle service")
        void declineOrder_success() {
            order.setCurrentStatus(OrderStatus.DECLINED);
            when(orderLifecycleService.transitionStatus(order, OrderStatus.PLACED, OrderStatus.DECLINED))
                    .thenReturn(order);

            OrderResponse response = orderService.declineOrder(orderId, restaurantId, ownerId);

            assertThat(response.currentStatus()).isEqualTo(OrderStatus.DECLINED);
            verify(orderLifecycleService).transitionStatus(order, OrderStatus.PLACED, OrderStatus.DECLINED);
        }

        @Test
        @DisplayName("markPreparing delegates status transition to lifecycle service")
        void markPreparing_success() {
            order.setCurrentStatus(OrderStatus.PREPARING);
            when(orderLifecycleService.transitionStatus(order, OrderStatus.ACCEPTED, OrderStatus.PREPARING))
                    .thenReturn(order);

            OrderResponse response = orderService.markPreparing(orderId, restaurantId, ownerId);

            assertThat(response.currentStatus()).isEqualTo(OrderStatus.PREPARING);
            verify(orderLifecycleService).transitionStatus(order, OrderStatus.ACCEPTED, OrderStatus.PREPARING);
        }

        @Test
        @DisplayName("markReadyForPickup transitions status and triggers autoAssign")
        void markReadyForPickup_success() {
            order.setCurrentStatus(OrderStatus.READY_FOR_PICKUP);
            when(orderLifecycleService.transitionStatus(order, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP))
                    .thenReturn(order);

            OrderResponse response = orderService.markReadyForPickup(orderId, restaurantId, ownerId);

            assertThat(response.currentStatus()).isEqualTo(OrderStatus.READY_FOR_PICKUP);
            verify(orderLifecycleService).transitionStatus(order, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
            verify(deliveryAssignmentService).autoAssign(order);
        }
    }
}
