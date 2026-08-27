package com.quickbite.quickbite.order.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.dto.OrderSummaryResponse;
import com.quickbite.quickbite.order.dto.PlaceOrderRequest;
import com.quickbite.quickbite.payment.dto.PaymentResult;

import java.util.UUID;

public interface CustomerOrderService {
    /**
     * Creates the order, snapshots cart items, and delegates to PaymentService.
     *
     * @return a {@link PaymentResult} subtype: {@code CodPaymentResult} for COD orders,
     *         or a gateway-specific result for online payments.
     */
    PaymentResult placeOrder(UUID customerId, PlaceOrderRequest req);

    OrderResponse getMyOrder(UUID customerId, UUID orderId);
    CursorPage<OrderSummaryResponse> listMyOrders(UUID customerId, UUID cursor, int size);
    void cancelOrder(UUID customerId, UUID orderId);
}
