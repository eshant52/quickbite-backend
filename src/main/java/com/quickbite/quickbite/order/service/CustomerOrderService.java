package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.payment.dto.PaymentResult;
import com.quickbite.quickbite.payment.model.PaymentMethod;

import java.util.UUID;

public interface CustomerOrderService {
    /**
     * Checkout orchestrator. NOT @Transactional — drives two independent transactions.
     *
     * <pre>
     * placeOrder() [no TX]
     *   │
     *   ├─ TX 1: orderCreationService.createOrderWithItems()  → COMMITS (pure DB, fast)
     *   │
     *   └─ TX 2: paymentService.initiatePayment()             → COMMITS (pure DB, fast)
     *              ├─ for COD: eventPublisher fires OrderPlacedEvent
     *              │           → Kafka send happens AFTER TX 2 commits (AFTER_COMMIT listener)
     *              │           → cart cleared
     *              │
     *              └─ for online: returns PaymentResult with gateway order ID
     * </pre>
     */
    PaymentResult placeOrder(UUID customerId, PlaceOrderRequest req);

    /**
     * Retries payment for an existing order in AWAITING_PAYMENT or PAYMENT_FAILED state.
     * Idempotently reuses active gateway orders for online payment or transitions to COD.
     *
     * @param customerId    the customer's UUID
     * @param orderId       the order's UUID
     * @param paymentMethod optional updated payment method (defaults to original method if null)
     * @return a {@link PaymentResult} with payment details or gateway checkout credentials
     */
    PaymentResult retryPayment(UUID customerId, UUID orderId, PaymentMethod paymentMethod);

    OrderResponse getMyOrder(UUID customerId, UUID orderId);
    CursorPage<OrderSummaryResponse> listMyOrders(UUID customerId, UUID cursor, int size);
    void cancelOrder(UUID customerId, UUID orderId);
}
