package com.quickbite.quickbite.delivery.service;

import com.quickbite.quickbite.delivery.dto.DeliveryAgentResponse;
import com.quickbite.quickbite.delivery.dto.UpdateLocationRequest;
import com.quickbite.quickbite.order.dto.OrderResponse;

import java.util.UUID;

/**
 * Service dedicated to delivery driver self-service, availability, GPS telemetry, and order trip execution.
 */
public interface DeliveryAgentService {

    DeliveryAgentResponse getMyProfile(UUID userId);

    DeliveryAgentResponse updateLocation(UUID userId, UpdateLocationRequest req);

    DeliveryAgentResponse updateAvailability(UUID userId, boolean available);

    OrderResponse markOutForDelivery(UUID orderId, UUID agentUserId);

    OrderResponse markDelivered(UUID orderId, UUID agentUserId);
}
