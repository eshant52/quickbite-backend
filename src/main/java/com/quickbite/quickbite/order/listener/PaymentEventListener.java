package com.quickbite.quickbite.order.listener;

import com.quickbite.quickbite.common.config.property.OrderProperties;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles internal payment domain events to synchronize {@link Order} state.
 *
 * <h2>Why this exists</h2>
 * <p>{@code PaymentLifecycleServiceImpl} must not import order-domain repositories
 * directly (AGENTS.md: "Never import from one domain into another domain's internal classes").
 * Instead, the payment domain publishes internal Spring application events, and this
 * listener handles the order-side state transitions atomically.
 *
 * <h2>Transaction Phase: BEFORE_COMMIT</h2>
 * <p>All handlers run in the {@code BEFORE_COMMIT} phase so that order and payment state
 * changes are written in the <em>same</em> DB transaction. If the transaction rolls back,
 * both the payment update and the order update roll back together, preserving consistency.
 */
@Component
public class PaymentEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OrderProperties orderProperties;

    public PaymentEventListener(
            OrderRepository orderRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            ApplicationEventPublisher eventPublisher,
            OrderProperties orderProperties) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.eventPublisher = eventPublisher;
        this.orderProperties = orderProperties;
    }

    /**
     * Handles payment SUCCESS.
     *
     * <ul>
     *   <li>{@code AWAITING_PAYMENT → PLACED}: normal checkout flow — publishes
     *       {@link OrderPlacedEvent} (AFTER_COMMIT) for downstream notifications.</li>
     *   <li>{@code ABANDONED / CANCELLED}: late capture — requests auto-refund via
     *       {@link PaymentRefundRequestedEvent} (AFTER_COMMIT, handled by
     *       {@code PaymentRefundListener}).</li>
     * </ul>
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePaymentSucceeded(PaymentSucceededEvent event) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found for payment sync: " + event.orderId()));

        OrderStatus current = order.getCurrentStatus();

        if (current == OrderStatus.AWAITING_PAYMENT) {
            order.setCurrentStatus(OrderStatus.PLACED);
            order.setRestaurantAcceptanceDeadline(Instant.now().plus(orderProperties.restaurantAcceptanceWindow()));
            orderRepository.save(order);
            recordOrderHistory(order, OrderStatus.PLACED);

            eventPublisher.publishEvent(new OrderPlacedEvent(
                    order.getId(),
                    order.getCustomer().getId(),
                    order.getCustomer().getName(),
                    order.getCustomer().getEmail(),
                    order.getRestaurant().getId(),
                    order.getRestaurant().getName(),
                    order.getTotalAmount(),
                    order.getCreatedAt()
            ));

        } else if (current == OrderStatus.ABANDONED || current == OrderStatus.CANCELLED) {
            log.warn("Payment {} succeeded for {} order {}. Evaluating auto-refund eligibility.",
                    event.paymentId(), current, order.getId());

            boolean hasCapturedPayment = event.paymentMethod() != null
                    && event.paymentMethod().isOnline()
                    && event.gatewayPaymentId() != null
                    && !event.gatewayPaymentId().isBlank();

            if (hasCapturedPayment) {
                String refundReason = "Auto-refund: Order was " + current + " before payment confirmation";
                eventPublisher.publishEvent(new PaymentRefundRequestedEvent(
                        event.paymentId(),
                        event.gatewayPaymentId(),
                        event.amount(),
                        refundReason
                ));
            } else {
                log.warn("No gateway capture found for payment {} on {} order {} — nothing to refund.",
                        event.paymentId(), current, order.getId());
            }
        }
    }

    /**
     * Handles payment FAILED.
     * Transitions {@code AWAITING_PAYMENT → PAYMENT_FAILED} so the customer can retry.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePaymentFailed(PaymentFailedEvent event) {
        Order order = loadOrder(event.orderId());

        if (order.getCurrentStatus() == OrderStatus.AWAITING_PAYMENT) {
            order.setCurrentStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            recordOrderHistory(order, OrderStatus.PAYMENT_FAILED);
        }
    }

    /**
     * Handles payment CANCELLED.
     *
     * <p>Fixes issue #14: previously {@code reconcileExpiredPayment} set the payment to
     * {@code CANCELLED} but had no order-sync branch, leaving the order permanently stuck
     * in {@code AWAITING_PAYMENT}. Now it transitions to {@code PAYMENT_FAILED} so the
     * customer can retry or cancel the order.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        Order order = loadOrder(event.orderId());

        if (order.getCurrentStatus() == OrderStatus.AWAITING_PAYMENT) {
            order.setCurrentStatus(OrderStatus.PAYMENT_FAILED);
            orderRepository.save(order);
            recordOrderHistory(order, OrderStatus.PAYMENT_FAILED);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void recordOrderHistory(Order order, OrderStatus status) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOrderStatus(status);
        orderStatusHistoryRepository.save(history);
    }

    private Order loadOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found for payment sync: " + orderId));
    }
}
