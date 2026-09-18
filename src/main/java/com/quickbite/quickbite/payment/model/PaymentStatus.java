package com.quickbite.quickbite.payment.model;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED,
    REFUNDED,
    /**
     * Gateway refund failed. The failure reason is recorded in {@code PaymentStatusHistory}.
     * No automated retry — the support team investigates via payment history.
     */
    REFUND_FAILED
}
