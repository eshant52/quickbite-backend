package com.quickbite.quickbite.common.event;

import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes restaurant application lifecycle events to the {@link QuickBiteTopics#RESTAURANT_APPLICATION_EVENTS} topic
 * only AFTER the originating database transaction has committed.
 */
@Component
public class RestaurantApplicationKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RestaurantApplicationKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RestaurantApplicationKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationSubmitted(RestaurantApplicationSubmittedEvent event) {
        kafkaTemplate.send(
                        QuickBiteTopics.RESTAURANT_APPLICATION_EVENTS,
                        event.applicationId().toString(),
                        event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish RestaurantApplicationSubmittedEvent for app={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] RestaurantApplicationSubmittedEvent published for app={}", event.applicationId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationApproved(RestaurantApplicationApprovedEvent event) {
        kafkaTemplate.send(
                        QuickBiteTopics.RESTAURANT_APPLICATION_EVENTS,
                        event.applicationId().toString(),
                        event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish RestaurantApplicationApprovedEvent for app={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] RestaurantApplicationApprovedEvent published for app={}", event.applicationId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationRejected(RestaurantApplicationRejectedEvent event) {
        kafkaTemplate.send(
                        QuickBiteTopics.RESTAURANT_APPLICATION_EVENTS,
                        event.applicationId().toString(),
                        event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish RestaurantApplicationRejectedEvent for app={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] RestaurantApplicationRejectedEvent published for app={}", event.applicationId());
                    }
                });
    }
}
