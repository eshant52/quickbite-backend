package com.quickbite.quickbite.order.model;

public enum OrderStatus {
    // Initial statuses
    AWAITING_PAYMENT, // Order created but payment not yet completed

    // Intermediate statuses
    PLACED, // Payment completed
    ACCEPTED, // Restaurant accepted the order
    PREPARING, // Restaurant is preparing the order
    READY_FOR_PICKUP, // Order is ready for pickup
    OUT_FOR_DELIVERY, // Order is out for delivery to customer

    // Terminal statuses
    PAYMENT_FAILED, // Payment failed and customer did not retry
    CANCELLED, // Order canceled by customer
    DECLINED, // Restaurant declined the order
    DELIVERED, // Order successfully delivered to customer
    ABANDONED // Order abandoned due to timeout or other reasons
}
