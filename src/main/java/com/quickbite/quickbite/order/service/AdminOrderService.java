package com.quickbite.quickbite.order.service;

import java.util.UUID;

public interface AdminOrderService {

    /**
     * Refunds the payment for an order that has already been {@code DELIVERED}
     * (supports both online gateway payments and Cash on Delivery orders).
     *
     * @param orderId the order's UUID
     * @param reason  the reason for issuing the post-delivery refund
     */
    void refundDeliveredOrder(UUID orderId, String reason);
}
