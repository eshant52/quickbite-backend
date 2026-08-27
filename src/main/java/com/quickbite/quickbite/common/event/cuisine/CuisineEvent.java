package com.quickbite.quickbite.common.event.cuisine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CuisineRequestedEvent.class, name = "CUISINE_REQUESTED"),
        @JsonSubTypes.Type(value = CuisineApprovedEvent.class, name = "CUISINE_APPROVED"),
        @JsonSubTypes.Type(value = CuisineRejectedEvent.class, name = "CUISINE_REJECTED")
})
public sealed interface CuisineEvent
        permits CuisineRequestedEvent, CuisineApprovedEvent, CuisineRejectedEvent {

    UUID requestId();
}
