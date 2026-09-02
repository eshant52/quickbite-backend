package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.event.order.OrderCancelledEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.service.PaymentService;
import com.quickbite.quickbite.restaurant.model.Restaurant;
import com.quickbite.quickbite.restaurant.repository.RestaurantRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates customer and restaurant order operations.
 *
 * <h2>Transaction strategy</h2>
 * <p>
 * {@link #placeOrder} is deliberately <b>not</b> annotated with {@code @Transactional}.
 * It acts as an orchestrator that drives two independent, short-lived transactions:
 * <ol>
 *   <li><b>TX 1</b> — {@link OrderCreationService#createOrderWithItems}: validates the
 *       cart, builds the Order aggregate, and commits. The DB connection is released
 *       before any payment logic runs.</li>
 *   <li><b>TX 2</b> — {@link PaymentService#initiatePayment}: creates the Payment record
 *       (and for COD, clears the cart). Also commits before returning.</li>
 * </ol>
 * <p>
 * When real gateways (Razorpay, Stripe) are added, the HTTP call to the gateway will
 * happen <em>after</em> TX 2 commits — no DB connection will be held during the network
 * round-trip.
 *
 * <h2>Kafka publishing</h2>
 * <p>
 * All methods that need to publish Kafka events use {@link ApplicationEventPublisher}
 * rather than {@code KafkaTemplate} directly. Spring's
 * {@code @TransactionalEventListener(phase = AFTER_COMMIT)} in
 * {@code OrderKafkaEventPublisher} ensures the message reaches Kafka only after the
 * DB transaction has durably committed, eliminating the "consumer sees event before
 * data exists" race condition.
 */
@Service
public class OrderServiceImpl implements CustomerOrderService, RestaurantOrderService {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderCreationService orderCreationService;
    private final PaymentService paymentService;
    private final com.quickbite.quickbite.delivery.service.DeliveryService deliveryService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            RestaurantRepository restaurantRepository,
            OrderCreationService orderCreationService,
            PaymentService paymentService,
            com.quickbite.quickbite.delivery.service.DeliveryService deliveryService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderCreationService = orderCreationService;
        this.paymentService = paymentService;
        this.deliveryService = deliveryService;
        this.eventPublisher = eventPublisher;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Customer operations
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Checkout orchestrator. NOT @Transactional — drives two independent transactions.
     *
     * <pre>
     * placeOrder() [no TX]
     *   │
     *   ├─ TX 1: orderCreationService.createOrderWithItems()  → COMMITS (pure DB, fast)
     *   │
     *   └─ TX 2: paymentService.initiatePayment()             → COMMITS (pure DB, fast)
     *              └─ for COD: eventPublisher fires OrderPlacedEvent
     *                          → Kafka send happens AFTER TX 2 commits (AFTER_COMMIT listener)
     * </pre>
     */
    @Override
    public PaymentResult placeOrder(UUID customerId, PlaceOrderRequest req) {
        // TX 1 — validate cart, build order, persist order + items + status history
        Order savedOrder = orderCreationService.createOrderWithItems(customerId, req);

        // TX 2 — create payment record; COD also clears cart and registers OrderPlacedEvent
        return paymentService.initiatePayment(savedOrder, req.paymentMethod());
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

        OrderStatus current = order.getCurrentStatus();
        if (current == OrderStatus.CANCELLED) {
            throw new OrderStateException("Order is already cancelled");
        }
        if (current != OrderStatus.PLACED && current != OrderStatus.AWAITING_PAYMENT) {
            throw new OrderStateException(
                    "Order cannot be cancelled once the restaurant has accepted it");
        }

        order.setCurrentStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOrderStatus(OrderStatus.CANCELLED);
        orderStatusHistoryRepository.save(history);

        // Registered for AFTER_COMMIT — Kafka send happens only after this TX commits
        eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                customer.getId(),
                order.getRestaurant().getId(),
                Instant.now()
        ));
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
        return OrderResponse.from(transition(order, OrderStatus.PLACED, OrderStatus.ACCEPTED));
    }

    @Override
    @Transactional
    public OrderResponse declineOrder(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        return OrderResponse.from(transition(order, OrderStatus.PLACED, OrderStatus.DECLINED));
    }

    @Override
    @Transactional
    public OrderResponse markPreparing(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        return OrderResponse.from(transition(order, OrderStatus.ACCEPTED, OrderStatus.PREPARING));
    }

    @Override
    @Transactional
    public OrderResponse markReadyForPickup(UUID orderId, UUID restaurantId, UUID ownerId) {
        Order order = loadRestaurantOrder(orderId, restaurantId, ownerId);
        Order updated = transition(order, OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP);
        deliveryService.autoAssign(updated);
        return OrderResponse.from(updated);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Validates and performs a state-machine transition, records history, and
     * registers an {@code OrderStatusChangedEvent} that Kafka will receive only
     * after the enclosing {@code @Transactional} method commits.
     */
    private Order transition(Order order, OrderStatus expected, OrderStatus next) {
        if (order.getCurrentStatus() != expected) {
            throw new OrderStateException(
                    "Cannot transition order from " + order.getCurrentStatus() +
                    " to " + next + ". Expected status: " + expected);
        }

        order.setCurrentStatus(next);
        Order saved = orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(saved);
        history.setOrderStatus(next);
        orderStatusHistoryRepository.save(history);

        // Registered for AFTER_COMMIT — Kafka send happens only after the caller's TX commits
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                saved.getId(),
                saved.getCustomer().getId(),
                saved.getRestaurant().getId(),
                expected,
                next,
                Instant.now()
        ));

        return saved;
    }

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
