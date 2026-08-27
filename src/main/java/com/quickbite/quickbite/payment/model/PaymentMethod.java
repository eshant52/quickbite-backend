package com.quickbite.quickbite.payment.model;

public enum PaymentMethod {
    UPI,
    CARD,
    NET_BANKING,
    COD,
    WALLET;

    /**
     * Returns {@code true} for all methods that require an online payment gateway
     * (i.e. everything except Cash on Delivery).
     */
    public boolean isOnline() {
        return this != COD;
    }
}
