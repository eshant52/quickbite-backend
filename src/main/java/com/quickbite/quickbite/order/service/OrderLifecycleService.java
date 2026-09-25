package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.cart.model.Cart;
import com.quickbite.quickbite.common.routing.RouteResult;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.user.model.Address;
import com.quickbite.quickbite.user.model.User;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain service managing lifecycle state transitions, pessimistic locking,
 * payment retry validation, and order abandonment.
 *
 * <p>All operations execute within fast, isolated database transactions,
 * ensuring database connections are never held open across external network calls.
 */
public interface OrderLifecycleService {

    /**
     * Loads an order by ID with a pessimistic write lock and checks it is not in a terminal state.
     *
     * @param orderId the order ID
     * @return the order entity if not in a terminal state or else null
     */
    Optional<Order> getOrderIfNotInTerminalState(UUID orderId);

    /**
     * Prepares an order for a payment retry by loading it with a pessimistic write lock,
     * checking it is not in a terminal state, and returning it for further processing.
     *
     * @param customerId the customer ID
     * @param orderId the order ID
     * @return the order entity ready for retry
     * @throws com.quickbite.quickbite.common.exception.BadRequestException if the order is in a terminal state or does not belong to the customer
     */
    Order prepareOrderForRetry(UUID customerId, UUID orderId);

    /**
     * Resets order status prior to initiating a new payment attempt within a short transaction.
     * If the order was in {@code PAYMENT_FAILED}, transitions to {@code AWAITING_PAYMENT}.
     *
     * @param orderId the order ID
     */
    void resetOrderStatusForRetry(UUID orderId);

    /**
     * Marks an order as {@code ABANDONED}, appends to status history, cancels any pending
     * payments, and publishes an {@code OrderStatusChangedEvent}.
     *
     * @param order  the order to abandon
     * @param reason the reason for abandonment
     */
    void abandonOrder(Order order, String reason);

    /**
     * Acquires up to {@code batchSize} stale {@code AWAITING_PAYMENT} orders created before {@code cutoff}
     * using {@code FOR UPDATE SKIP LOCKED} in an isolated transaction, marks them {@code ABANDONED},
     * cancels pending payments, and publishes lifecycle events.
     *
     * <p>Because {@code SKIP LOCKED} is used, multiple pods can execute this method concurrently
     * without stepping on each other or waiting for row locks.
     *
     * @param cutoff    the maximum creation timestamp for stale orders
     * @param batchSize the maximum number of orders to process in this batch
     * @return the number of orders successfully processed in this batch
     */
    int processAbandonmentBatch(Instant cutoff, int batchSize);

    /**
     * Loads an order by ID with a pessimistic write lock, checks it is not already
     * in a terminal state, and abandons it in a short isolated transaction.
     *
     * <p>Safe to call from a non-transactional context — runs its own transaction.
     * Designed to be called from {@code OrderServiceImpl.retryPayment} after catching
     * an {@link com.quickbite.quickbite.order.exception.OrderExpiredException}, ensuring
     * the abandonment commits independently from the exception-causing transaction.
     *
     * @param orderId the order to abandon
     * @param reason  the reason for abandonment
     */
    void abandonOrderById(UUID orderId, String reason);

    /**
     * Cancels an order, updates status to {@code CANCELLED}, records status history,
     * cancels pending payments, and publishes an {@code OrderCancelledEvent}.
     *
     * @param order the order to cancel
     */
    void cancelOrder(Order order);

    /**
     * Transitions an order from an expected status to a next status, records history,
     * and publishes an {@code OrderStatusChangedEvent}.
     *
     * @param order    the order entity
     * @param expected expected current status
     * @param next     next status
     * @return the saved order
     */
    Order transitionStatus(Order order, OrderStatus expected, OrderStatus next);

    /**
     * Persists a new order to the database.
     *
     * @param customer the customer placing the order
     * @param customerAddress the customer's address
     * @param cart the customer's cart
     * @param route the route for the order
     * @param deliveryFee the delivery fee
     * @param platformFee the platform fee
     * @param subTotal the subtotal
     * @param taxAmount the tax amount
     * @param tip the tip
     * @param total the total amount
     * @param initialStatus the initial status of the order
     * @return the saved order
     */
    Order persistNewOrder(User customer,
                          Address customerAddress,
                          Cart cart,
                          RouteResult route,
                          BigDecimal deliveryFee,
                          BigDecimal platformFee,
                          BigDecimal subTotal,
                          BigDecimal taxAmount,
                          BigDecimal tip,
                          BigDecimal total,
                          OrderStatus initialStatus);

    /**
     * Determines if the given order status is an intermediate state (i.e., not terminal).
     *
     * @param status the order status to check
     * @return true if the status is intermediate, false if terminal
     */
    boolean isIntermediate(OrderStatus status);

    /**
     * Determines if the given order status is a terminal state (i.e., not intermediate).
     *
     * @param status the order status to check
     * @return true if the status is terminal, false if intermediate
     */
    boolean isTerminal(OrderStatus status);

    /**
     * Acquires up to {@code batchSize} stale {@code PLACED} orders past their restaurant acceptance deadline
     * using {@code FOR UPDATE SKIP LOCKED}, marks them {@code CANCELLED} with reason
     * {@code RESTAURANT_UNRESPONSIVE}, cancels pending payments or refunds successful online payments,
     * and publishes cancellation events.
     *
     * @param now       current timestamp
     * @param batchSize maximum number of orders to process
     * @return number of orders processed
     */
    int processRestaurantAcceptanceTimeoutBatch(Instant now, int batchSize);

    /**
     * Cancels a single unaccepted PLACED order in its own isolated transaction.
     *
     * @param order the unaccepted order to cancel
     */
    void cancelUnacceptedOrder(Order order);

    /**
     * Cancels an order when proactive delivery dispatch fails to assign an agent within the max window.
     * Sets status to {@code CANCELLED} with reason {@code NO_AGENT_FOUND}, refunds customer if paid online,
     * and publishes cancellation events.
     *
     * @param orderId the order ID
     */
    void cancelDueToNoDeliveryAgent(UUID orderId);
}
