package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.common.event.order.OrderCancelledEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import com.quickbite.quickbite.common.exception.BadRequestException;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.order.exception.OrderNotFoundException;
import com.quickbite.quickbite.order.exception.OrderStateException;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderItem;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;
import com.quickbite.quickbite.order.repository.OrderItemRepository;
import com.quickbite.quickbite.order.repository.OrderRepository;
import com.quickbite.quickbite.order.repository.OrderStatusHistoryRepository;
import com.quickbite.quickbite.payment.service.PaymentProcessingService;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(OrderLifecycleServiceImpl.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final PaymentProcessingService paymentService;
    private final ApplicationEventPublisher eventPublisher;

    public OrderLifecycleServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            PaymentProcessingService paymentService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.paymentService = paymentService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Order prepareOrderForRetry(UUID customerId, UUID orderId) {
        Order lockedOrder = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (!lockedOrder.getCustomer().getId().equals(customerId)) {
            throw new OrderNotFoundException("Order not found");
        }

        OrderStatus current = lockedOrder.getCurrentStatus();

        if (isTerminal(current)) {
            throw new BadRequestException("Order is in terminal state " + current + " and cannot be retried.");
        }

        if (current != OrderStatus.AWAITING_PAYMENT
                && current != OrderStatus.PAYMENT_FAILED
                && !isIntermediate(current)) {
            throw new BadRequestException("Order is in " + current + " state and cannot be retried.");
        }

        return lockedOrder;
    }

    @Override
    @Transactional
    public void resetOrderStatusForRetry(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found"));

        if (order.getCurrentStatus() == OrderStatus.PAYMENT_FAILED) {
            order.setCurrentStatus(OrderStatus.AWAITING_PAYMENT);
            orderRepository.save(order);

            OrderStatusHistory history = new OrderStatusHistory();
            history.setOrder(order);
            history.setOrderStatus(OrderStatus.AWAITING_PAYMENT);
            orderStatusHistoryRepository.save(history);
        }
    }

    @Override
    @Transactional
    public void abandonOrder(Order order, String reason) {
        // Idempotency guard: skip if already in a terminal state.
        // Prevents double-abandonment from concurrent placeOrder calls or scheduler overlaps.
        if (isTerminal(order.getCurrentStatus())) {
            log.info("Order {} already in terminal state {}, skipping abandonment.",
                    order.getId(), order.getCurrentStatus());
            return;
        }

        OrderStatus previousStatus = order.getCurrentStatus();
        order.setCurrentStatus(OrderStatus.ABANDONED);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOrderStatus(OrderStatus.ABANDONED);
        orderStatusHistoryRepository.save(history);

        paymentService.cancelPendingPayments(order.getId(), reason);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomer().getId(),
                order.getRestaurant().getId(),
                previousStatus,
                OrderStatus.ABANDONED,
                Instant.now()
        ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processAbandonmentBatch(Instant cutoff, int batchSize) {
        List<Order> staleOrders = orderRepository.findByCurrentStatusAndCreatedAtBeforeForUpdateSkipLocked(
                OrderStatus.AWAITING_PAYMENT,
                cutoff,
                Limit.of(batchSize)
        );

        if (staleOrders.isEmpty()) {
            return 0;
        }

        for (Order order : staleOrders) {
            try {
                abandonOrder(order, "Payment window expired (auto-abandoned)");
            } catch (Exception e) {
                log.error("Failed to abandon order {}: {}", order.getId(), e.getMessage(), e);
            }
        }

        return staleOrders.size();
    }

    @Override
    @Transactional
    public void abandonOrderById(UUID orderId, String reason) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId));
        // Re-check terminal state under lock in case another TX committed between the
        // TTL check in prepareOrderForRetry and this call.
        abandonOrder(order, reason);
    }

    @Override
    @Transactional
    public void cancelOrder(Order order) {
        OrderStatus current = order.getCurrentStatus();
        if (current == OrderStatus.CANCELLED) {
            return; // Idempotent
        }
        if (current != OrderStatus.PLACED
                && current != OrderStatus.AWAITING_PAYMENT
                && current != OrderStatus.PAYMENT_FAILED) {
            throw new OrderStateException(
                    "Order cannot be cancelled once the restaurant has accepted it");
        }

        order.setCurrentStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOrderStatus(OrderStatus.CANCELLED);
        orderStatusHistoryRepository.save(history);

        paymentService.cancelPendingPayments(order.getId(), "Order cancelled by customer");

        eventPublisher.publishEvent(new OrderCancelledEvent(
                order.getId(),
                order.getCustomer().getId(),
                order.getRestaurant().getId(),
                Instant.now()
        ));
    }

    @Override
    @Transactional
    public Order transitionStatus(Order order, OrderStatus expected, OrderStatus next) {
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

    @Transactional
    public Order persistNewOrder(
            User customer,
            Address customerAddress,
            Cart cart,
            RouteResult route,
            BigDecimal deliveryFee,
            BigDecimal platformFee,
            BigDecimal subTotal,
            BigDecimal taxAmount,
            BigDecimal tip,
            BigDecimal total,
            OrderStatus initialStatus) {

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(cart.getRestaurant());
        order.setDeliveryAddress(formatAddress(customerAddress));
        order.setDeliveryLocation(customerAddress.getLocation());
        order.setSubtotal(subTotal);
        order.setDiscountAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        order.setDeliveryFee(deliveryFee);
        order.setPlatformFee(platformFee);
        order.setTaxAmount(taxAmount);
        order.setTipAmount(tip);
        order.setTotalAmount(total);
        order.setCurrentStatus(initialStatus);
        order.setDeliveryDistanceMeters(route.distanceMeters());
        order.setEstimatedDeliverySeconds(route.durationSeconds());

        Order savedOrder = orderRepository.save(order);


        // Snapshot cart items as OrderItems
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    if (!cartItem.getMenuItem().isAvailable()) {
                        throw new BadRequestException(
                                "Item '" + cartItem.getMenuItem().getName() + "' is no longer available");
                    }
                    OrderItem item = new OrderItem();
                    item.setOrder(savedOrder);
                    item.setMenuItem(cartItem.getMenuItem());
                    item.setQuantity(cartItem.getQuantity());
                    item.setUnitPrice(cartItem.getUnitPrice());
                    item.setSubTotal(cartItem.getSubTotal());
                    return item;
                }).toList();

        orderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);


        // Record initial status history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(savedOrder);
        history.setOrderStatus(initialStatus);
        orderStatusHistoryRepository.save(history);

        return savedOrder;
    }

    @Override
    public boolean isIntermediate(OrderStatus status) {
        return status == OrderStatus.PLACED
                || status == OrderStatus.ACCEPTED
                || status == OrderStatus.PREPARING
                || status == OrderStatus.READY_FOR_PICKUP
                || status == OrderStatus.OUT_FOR_DELIVERY;
    }

    @Override
    public boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.ABANDONED
                || status == OrderStatus.CANCELLED
                || status == OrderStatus.DELIVERED
                || status == OrderStatus.DECLINED;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String formatAddress(Address a) {
        StringBuilder sb = new StringBuilder();
        if (a.getHouseNumber() != null && !a.getHouseNumber().isBlank())
            sb.append(a.getHouseNumber()).append(", ");
        if (a.getBuildingName() != null && !a.getBuildingName().isBlank())
            sb.append(a.getBuildingName()).append(", ");
        sb.append(a.getStreet());
        if (a.getLandmark() != null && !a.getLandmark().isBlank())
            sb.append(", Near ").append(a.getLandmark());
        sb.append(", ").append(a.getCity()).append(", ").append(a.getState());
        if (a.getPostalCode() != null && !a.getPostalCode().isBlank())
            sb.append(" - ").append(a.getPostalCode());
        return sb.toString();
    }
}
