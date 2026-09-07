package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.dto.UpdateLocationRequest;
import com.quickbite.quickbite.delivery.model.DeliveryAgentVerificationStatus;
import com.quickbite.quickbite.order.dto.OrderResponse;
import com.quickbite.quickbite.order.model.Order;

import java.util.UUID;

public interface DeliveryService {

    // Agent Self-Service
    DeliveryAgentResponse getMyProfile(UUID userId);
    DeliveryAgentResponse updateLocation(UUID userId, UpdateLocationRequest req);
    DeliveryAgentResponse updateAvailability(UUID userId, boolean available);

    // System Assignment
    void autoAssign(Order order);

    // Agent Order Lifecycle
    OrderResponse markOutForDelivery(UUID orderId, UUID agentUserId);
    OrderResponse markDelivered(UUID orderId, UUID agentUserId);
}
