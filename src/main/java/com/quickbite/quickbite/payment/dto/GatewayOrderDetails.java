package com.quickbite.quickbite.payment.dto;

public record GatewayOrderDetails(
        GatewayOrderStatus status,
        String gatewayPaymentId
) {
    public static GatewayOrderDetails of(GatewayOrderStatus status) {
        return new GatewayOrderDetails(status, null);
    }
}
