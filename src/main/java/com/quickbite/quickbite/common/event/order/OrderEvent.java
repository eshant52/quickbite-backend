package com.quickbite.quickbite.common.event.order;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderPlacedEvent.class, name = "ORDER_PLACED"),
        @JsonSubTypes.Type(value = OrderStatusChangedEvent.class, name = "ORDER_STATUS_CHANGED"),
        @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "ORDER_CANCELLED")
})
public sealed interface OrderEvent
        permits OrderPlacedEvent, OrderStatusChangedEvent, OrderCancelledEvent {

    UUID orderId();
}
