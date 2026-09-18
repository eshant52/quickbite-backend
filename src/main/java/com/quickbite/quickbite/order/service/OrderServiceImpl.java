package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.config.property.OrderProperties;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.delivery.service.DeliveryAssignmentService;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Orchestrates customer and restaurant order operations.
 *
 * <h2>Transaction strategy</h2>
 * <p>
 * {@link #placeOrder} and {@link #retryPayment} are deliberately <b>not</b> annotated with {@code @Transactional}.
 * They act as orchestrators that delegate database state mutations to {@link OrderCreationService}
 * and {@link OrderLifecycleService} in short, isolated transactions, ensuring that external
 * network calls to payment gateways and Kafka dispatches happen without holding open database connections.
 */
@Service
public class OrderServiceImpl implements CustomerOrderService, RestaurantOrderService {

    private static final String PLACE_ORDER_COOLDOWN_PREFIX = "quickbite:cooldown:place-order:";

    private static final String RETRY_LOCK_PREFIX = "quickbite:lock:retry-payment:";
    private static final long RETRY_LOCK_TTL_SECONDS = 30;

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderCreationService orderCreationService;
    private final OrderLifecycleService orderLifecycleService;
    private final PaymentProcessingService paymentService;
    private final DeliveryAssignmentService deliveryAssignmentService;
    private final RedissonClient redissonClient;
    private final OrderProperties orderProperties;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            OrderCreationService orderCreationService,
            OrderLifecycleService orderLifecycleService,
            PaymentProcessingService paymentService,
            DeliveryAssignmentService deliveryAssignmentService,
            RedissonClient redissonClient,
            OrderProperties orderProperties) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderCreationService = orderCreationService;
        this.orderLifecycleService = orderLifecycleService;
        this.paymentService = paymentService;
        this.deliveryAssignmentService = deliveryAssignmentService;
        this.redissonClient = redissonClient;
        this.orderProperties = orderProperties;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Customer operations
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    public PaymentResult placeOrder(UUID customerId, PlaceOrderRequest req) {
        // Prevent frequent rapid order submissions by blocking the customer for configured cooldown
        RBucket<String> cooldownBucket = redissonClient.getBucket(PLACE_ORDER_COOLDOWN_PREFIX + customerId);
        boolean acquired = cooldownBucket.setIfAbsent("LOCKED", Duration.ofSeconds(orderProperties.placeCooldownSeconds()));
        if (!acquired) {
            throw new BadRequestException("An order was recently submitted. Please wait " + orderProperties.placeCooldownSeconds() + " seconds before placing another order.");
        }

        try {
            // TX 1 — validate cart, build order, persist order + items + status history
            Order savedOrder = orderCreationService.createOrderWithItems(customerId, req);

            // TX 2 — create payment record; COD also clears cart and registers OrderPlacedEvent
            return paymentService.initiatePayment(savedOrder, req.paymentMethod());
        } catch (Exception e) {
            // If validation or order placement fails, release the cooldown so customer can correct and retry
            cooldownBucket.delete();
            throw e;
        }
    }

    @Override
    public PaymentResult retryPayment(UUID customerId, UUID orderId, PaymentMethod paymentMethod) {
        RLock lock = redissonClient.getLock(RETRY_LOCK_PREFIX + orderId);
        boolean locked;
        try {
            locked = lock.tryLock(0, RETRY_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Retry interrupted. Please try again.");
        }

        if (!locked) {
            throw new BadRequestException(
                    "A payment retry is already in progress for this order. Please wait.");
        }

        try {
            // Step 1: Validate customer ownership and non-terminal status (short isolated TX with lock)
            Order order = orderLifecycleService.prepareOrderForRetry(customerId, orderId);

            PaymentMethod methodToUse = paymentMethod != null ? paymentMethod : PaymentMethod.UPI;

            // Step 2: Comprehensive multi-attempt reconciliation against the gateway (createdAt DESC)
            // Reconciles all previous gateway orders, detects dual captures, and publishes auto-refunds
            Optional<PaymentResult> reconciledPaidResult = paymentService.reconcileAllPaymentAttempts(order.getId());
            if (reconciledPaidResult.isPresent()) {
                return reconciledPaidResult.get(); // Winning payment fulfilled the order!
            }

            // Step 3: Check 15-minute TTL only if the order was NOT paid at the gateway
            Instant cutoff = Instant.now().minus(Duration.ofMinutes(orderProperties.abandonTtlMinutes()));
            if (order.getCreatedAt().isBefore(cutoff)) {
                orderLifecycleService.abandonOrderById(order.getId(), "Payment window expired");
                throw new BadRequestException(
                        "Payment window for this order has expired. Please place a new order.");
            }

            // Step 5: Reset order status back to AWAITING_PAYMENT
            orderLifecycleService.resetOrderStatusForRetry(order.getId());

            Order refreshedOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() -> new OrderNotFoundException("Order not found after reset"));

            // Step 6: Initiate payment (HTTP call to gateway runs OUTSIDE DB transaction)
            return paymentService.initiatePayment(refreshedOrder, methodToUse);

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<OrderSummaryResponse> listMyOrders(UUID customerId, UUID cursor, int size) {
        User customer = loadUser(customerId);
        int pageSize = Math.clamp(size, 1, 100);

        List<Order> orders = orderRepository.findByCustomerWithCursor(
                customer.getId(), cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                orders.stream().map(OrderSummaryResponse::from).toList(),
                pageSize,
                OrderSummaryResponse::id);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getMyOrder(UUID customerId, UUID orderId) {
        User customer = loadUser(customerId);
        Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
        return OrderResponse.from(order);
    }

    /**
     * Cancels an order. Allowed from {@code PLACED} (COD, customer changed mind) and
     * {@code AWAITING_PAYMENT} (online, customer abandoned before paying).
     */
    @Override
    @Transactional
    public void cancelOrder(UUID customerId, UUID orderId) {
        User customer = loadUser(customerId);

        Order order = orderRepository.findByIdAndCustomerId(orderId, customer.getId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        orderLifecycleService.cancelOrder(order);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Restaurant operations
    // ──────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CursorPage<OrderSummaryResponse> listRestaurantOrders(
            UUID restaurantId, UUID ownerId, OrderStatus status, UUID cursor, int size) {
        User owner = loadUser(ownerId);
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, owner);
        int pageSize = Math.clamp(size, 1, 100);

        List<Order> orders = orderRepository.findByRestaurantWithCursor(
                restaurant.getId(), status, cursor, Limit.of(pageSize + 1));

        return CursorPage.of(
                orders.stream().map(OrderSummaryResponse::from).toList(),
                pageSize,
                OrderSummaryResponse::id);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getRestaurantOrder(UUID orderId, UUID restaurantId, UUID ownerId) {
        return OrderResponse.from(loadRestaurantOrder(orderId, restaurantId, ownerId));
    }

    @Override
    @Transactional
    public OrderResponse acceptOrder(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        return OrderResponse.from(orderLifecycleService.transitionStatus(order, OrderStatus.PLACED, OrderStatus.ACCEPTED));
    }

    @Override
    @Transactional
    public OrderResponse declineOrder(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        return OrderResponse.from(orderLifecycleService.transitionStatus(order, OrderStatus.PLACED, OrderStatus.DECLINED));
    }

    @Override
    @Transactional
    public OrderResponse markPreparing(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        return OrderResponse.from(orderLifecycleService.transitionStatus(order, OrderStatus.ACCEPTED, OrderStatus.PREPARING));
    }

    @Override
    @Transactional
    public OrderResponse markReadyForPickup(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        Order updated = orderLifecycleService.transitionStatus(order, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
        deliveryAssignmentService.autoAssign(updated);
        return OrderResponse.from(updated);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Restaurant loadOwnedRestaurant(UUID restaurantId, User owner) {
        return restaurantRepository.findByIdAndOwner(restaurantId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found"));
    }

    private Order loadRestaurantOrder(UUID orderId, UUID restaurantId, UUID ownerId) {
        User owner = loadUser(ownerId);
        Restaurant restaurant = loadOwnedRestaurant(restaurantId, owner);
        return orderRepository.findByIdAndRestaurantId(orderId, restaurant.getId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    }
}
