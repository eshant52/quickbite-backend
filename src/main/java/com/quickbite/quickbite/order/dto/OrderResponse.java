package com.quickbite.quickbite.order.dto;

import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.order.model.OrderCancellationReason;
import com.quickbite.quickbite.order.model.OrderStatus;
import com.quickbite.quickbite.order.model.OrderStatusHistory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID restaurantId,
        String restaurantName,
        String deliveryAddress,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal deliveryFee,
        BigDecimal platformFee,
        BigDecimal taxAmount,
        BigDecimal tipAmount,
        BigDecimal totalAmount,
        OrderStatus currentStatus,
        OrderCancellationReason cancellationReason,
        Instant restaurantAcceptanceDeadline,
        AssignedDeliveryAgentResponse deliveryAgent,
        List<OrderStatusHistoryDto> statusHistory,
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
            OrderCancellationReason cancellationReason,
            Instant restaurantAcceptanceDeadline,
            Double deliveryDistanceMeters,
            Long estimatedDeliverySeconds,
            Instant createdAt
    ) {
        this(id, restaurantId, restaurantName, deliveryAddress, items,
                subtotal, BigDecimal.ZERO, deliveryFee, BigDecimal.ZERO, taxAmount, tipAmount, totalAmount,
                currentStatus, cancellationReason, restaurantAcceptanceDeadline, null, List.of(),
                deliveryDistanceMeters, estimatedDeliverySeconds, createdAt);
    }

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
            Double deliveryDistanceMeters,
            Long estimatedDeliverySeconds,
            Instant createdAt
    ) {
        this(id, restaurantId, restaurantName, deliveryAddress, items,
                subtotal, BigDecimal.ZERO, deliveryFee, BigDecimal.ZERO, taxAmount, tipAmount, totalAmount,
                currentStatus, null, null, null, List.of(),
                deliveryDistanceMeters, estimatedDeliverySeconds, createdAt);
    }

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
                subtotal, BigDecimal.ZERO, deliveryFee, BigDecimal.ZERO, taxAmount, tipAmount, totalAmount,
                currentStatus, null, null, null, List.of(),
                null, null, createdAt);
    }

    public static OrderResponse from(Order order) {
        List<OrderStatusHistory> rawHistory = order.getStatusHistory() != null
                ? order.getStatusHistory()
                : List.of();
        return from(order, rawHistory);
    }

    public static OrderResponse from(Order order, List<OrderStatusHistory> history) {
        List<OrderStatusHistoryDto> historyDtos = history == null ? List.of() : history.stream()
                .sorted(Comparator.comparing(
                        OrderStatusHistory::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(OrderStatusHistoryDto::from)
                .toList();

        List<OrderItemResponse> itemDtos = order.getItems() == null ? List.of() : order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getDeliveryAddress(),
                itemDtos,
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getDeliveryFee(),
                order.getPlatformFee(),
                order.getTaxAmount(),
                order.getTipAmount(),
                order.getTotalAmount(),
                order.getCurrentStatus(),
                order.getCancellationReason(),
                order.getRestaurantAcceptanceDeadline(),
                AssignedDeliveryAgentResponse.from(order.getDeliveryAgent()),
                historyDtos,
                order.getDeliveryDistanceMeters(),
                order.getEstimatedDeliverySeconds(),
                order.getCreatedAt()
        );
    }
}
