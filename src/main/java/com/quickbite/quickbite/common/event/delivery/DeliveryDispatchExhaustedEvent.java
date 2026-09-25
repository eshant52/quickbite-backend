package com.quickbite.quickbite.common.event.delivery;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;
import java.util.UUID;

@JsonTypeName("DELIVERY_DISPATCH_EXHAUSTED")
public record DeliveryDispatchExhaustedEvent(
        UUID orderId,
        UUID restaurantId,
        UUID customerId,
        Instant exhaustedAt
) implements DeliveryEvent {}
