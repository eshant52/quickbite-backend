package com.quickbite.quickbite.order.model;

public enum OrderStatus {
    AWAITING_PAYMENT,
    PAYMENT_FAILED,
    ACCEPTED,
    CANCELLED,
    DECLINED,
    DELIVERED,
    OUT_FOR_DELIVERY,
    PLACED,
    PREPARING,
    READY_FOR_PICKUP
}
