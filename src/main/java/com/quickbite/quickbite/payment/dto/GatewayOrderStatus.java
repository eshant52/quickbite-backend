package com.quickbite.quickbite.payment.dto;

public enum GatewayOrderStatus {
    /**
     * Order is created or attempted and remains unpaid. Active for new payment attempts.
     */
    OPEN,

    /**
     * Order has been successfully paid and captured.
     */
    PAID,

    /**
     * Order has expired, closed, or is no longer payable at the gateway.
     */
    EXPIRED,

    /**
     * Transient gateway communication error or status unknown.
     */
    UNKNOWN
}
