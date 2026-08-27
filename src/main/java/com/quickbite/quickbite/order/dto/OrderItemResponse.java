package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.order.model.OrderItem;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        String menuItemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {
    public static OrderItemResponse from(OrderItem orderItem) {
        return new OrderItemResponse(
                orderItem.getId(),
                orderItem.getMenuItem().getName(),
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getSubTotal()
        );
    }
}
