package com.quickbite.quickbite.common.event.publisher;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VehicleApplicationKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(VehicleApplicationKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public VehicleApplicationKafkaEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleApplicationSubmittedEvent(VehicleApplicationSubmittedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.VEHICLE_APPLICATION_EVENTS, event.applicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish VehicleApplicationSubmittedEvent for application={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published VehicleApplicationSubmittedEvent for application={} type={}",
                                event.applicationId(), event.getClass().getSimpleName());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleApplicationApprovedEvent(VehicleApplicationApprovedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.VEHICLE_APPLICATION_EVENTS, event.applicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish VehicleApplicationApprovedEvent for application={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published VehicleApplicationApprovedEvent for application={} type={}",
                                event.applicationId(), event.getClass().getSimpleName());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleVehicleApplicationRejectedEvent(VehicleApplicationRejectedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.VEHICLE_APPLICATION_EVENTS, event.applicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish VehicleApplicationRejectedEvent for application={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published VehicleApplicationRejectedEvent for application={} type={}",
                                event.applicationId(), event.getClass().getSimpleName());
                    }
                });
    }
}
