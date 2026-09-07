package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        String deliveryAddress,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        OrderStatus currentStatus,
        /** Road-network distance in metres from restaurant to customer. */
        Double deliveryDistanceMeters,
        /** Estimated driving seconds from restaurant to customer. */
        Long estimatedDeliverySeconds,
        Instant createdAt
) {
    public OrderResponse(
            UUID id,
            UUID restaurantId,
            String restaurantName,
            String deliveryAddress,
            List<OrderItemResponse> items,
            BigDecimal subtotal,
            BigDecimal deliveryFee,
            BigDecimal taxAmount,
            BigDecimal tipAmount,
            BigDecimal totalAmount,
            OrderStatus currentStatus,
            Instant createdAt
    ) {
        this(id, restaurantId, restaurantName, deliveryAddress, items,
                subtotal, deliveryFee, taxAmount, tipAmount, totalAmount,
                currentStatus, null, null, createdAt);
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getDeliveryAddress(),
                order.getItems().stream()
                        .map(OrderItemResponse::from)
                        .toList(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTaxAmount(),
                order.getTipAmount(),
                order.getTotalAmount(),
                order.getCurrentStatus(),
                order.getDeliveryDistanceMeters(),
                order.getEstimatedDeliverySeconds(),
                order.getCreatedAt()
        );
    }
}
