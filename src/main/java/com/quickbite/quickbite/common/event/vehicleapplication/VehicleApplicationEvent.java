package com.quickbite.quickbite.common.event.vehicleapplication;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = VehicleApplicationSubmittedEvent.class, name = "SUBMITTED"),
        @JsonSubTypes.Type(value = VehicleApplicationApprovedEvent.class, name = "APPROVED"),
        @JsonSubTypes.Type(value = VehicleApplicationRejectedEvent.class, name = "REJECTED")
})
public sealed interface VehicleApplicationEvent permits
        VehicleApplicationSubmittedEvent,
        VehicleApplicationApprovedEvent,
        VehicleApplicationRejectedEvent {
    UUID applicationId();
}
