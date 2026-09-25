package com.quickbite.quickbite.common.event.delivery;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeliveryOfferCreatedEvent.class, name = "DELIVERY_OFFER_CREATED"),
        @JsonSubTypes.Type(value = DeliveryAgentAssignedEvent.class, name = "DELIVERY_AGENT_ASSIGNED"),
        @JsonSubTypes.Type(value = DeliveryDispatchExhaustedEvent.class, name = "DELIVERY_DISPATCH_EXHAUSTED")
})
public sealed interface DeliveryEvent
        permits DeliveryOfferCreatedEvent, DeliveryAgentAssignedEvent, DeliveryDispatchExhaustedEvent {

    UUID orderId();
}
