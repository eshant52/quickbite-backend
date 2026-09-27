package com.quickbite.quickbite.order.model;

public enum OrderStatus {
    // Initial / pre-payment statuses (retryable within payment window)
    AWAITING_PAYMENT, // Order created, awaiting payment confirmation
    PAYMENT_FAILED, // Payment attempt failed or cancelled; customer can retry within TTL

    // Intermediate statuses
    PLACED, // Payment confirmed (or COD placed), awaiting restaurant acceptance
    ACCEPTED, // Restaurant accepted the order
    PREPARING, // Restaurant is preparing the order
    READY_FOR_PICKUP, // Order is ready for pickup by delivery partner
    OUT_FOR_DELIVERY, // Order is out for delivery to customer

    // Terminal statuses
    CANCELLED, // Order cancelled (customer, restaurant timeout, or no agent found)
    DECLINED, // Restaurant declined the order
    DELIVERED, // Order successfully delivered to customer
    ABANDONED // Order abandoned after payment window expired
}
