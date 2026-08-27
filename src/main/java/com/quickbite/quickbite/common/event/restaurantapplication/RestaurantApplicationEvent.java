package com.quickbite.quickbite.common.event.restaurantapplication;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = RestaurantApplicationSubmittedEvent.class, name = "APPLICATION_SUBMITTED"),
        @JsonSubTypes.Type(value = RestaurantApplicationApprovedEvent.class, name = "APPLICATION_APPROVED"),
        @JsonSubTypes.Type(value = RestaurantApplicationRejectedEvent.class, name = "APPLICATION_REJECTED")
})
public sealed interface RestaurantApplicationEvent
        permits RestaurantApplicationSubmittedEvent, RestaurantApplicationApprovedEvent, RestaurantApplicationRejectedEvent {

    UUID applicationId();
}
