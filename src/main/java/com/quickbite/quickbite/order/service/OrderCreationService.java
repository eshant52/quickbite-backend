package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.order.model.Order;

import java.util.UUID;

/**
 * Responsible only for the first transactional unit of order placement:
 * validating the cart, snapshotting prices into {@link Order} and its items,
 * and persisting them — all in one fast, DB-only transaction with no external
 * calls. The transaction commits before {@link PaymentService} is invoked.
 *
 * <p>Separating order creation from payment initiation keeps each DB transaction
 * short and prevents holding a connection open while calling an external gateway.
 */
public interface OrderCreationService {

    /**
     * Creates and persists a new {@link Order} (with all {@code OrderItem} snapshots
     * and initial {@code OrderStatusHistory}) within a single {@code @Transactional}
     * boundary that commits immediately upon return.
     *
     * <p>The cart is <b>not</b> cleared here; that is the payment strategy's
     * responsibility (COD clears it, online payments leave it until webhook confirms).
     *
     * @param customerId the authenticated customer's ID
     * @param req        the validated checkout request
     * @return the committed {@link Order} entity, ready to be passed to
     *         {@link PaymentService#initiatePayment}
     */
    Order createOrderWithItems(UUID customerId, PlaceOrderRequest req);
}
