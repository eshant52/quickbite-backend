package com.quickbite.quickbite.common.event.deliveryagentapplication;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeliveryAgentApplicationSubmittedEvent.class, name = "SUBMITTED"),
        @JsonSubTypes.Type(value = DeliveryAgentApplicationApprovedEvent.class, name = "APPROVED"),
        @JsonSubTypes.Type(value = DeliveryAgentApplicationRejectedEvent.class, name = "REJECTED")
})
public sealed interface DeliveryAgentApplicationEvent permits DeliveryAgentApplicationSubmittedEvent, DeliveryAgentApplicationApprovedEvent, DeliveryAgentApplicationRejectedEvent {

    UUID applicationId();
}
