package com.quickbite.quickbite.common.event.delivery;

import com.fasterxml.jackson.annotation.JsonTypeName;

import java.time.Instant;
import java.util.UUID;

@JsonTypeName("DELIVERY_OFFER_CREATED")
public record DeliveryOfferCreatedEvent(
        UUID offerId,
        UUID orderId,
        UUID agentId,
        int roundNumber,
        double radiusKm,
        Instant expiresAt,
        Instant createdAt
) implements DeliveryEvent {}
