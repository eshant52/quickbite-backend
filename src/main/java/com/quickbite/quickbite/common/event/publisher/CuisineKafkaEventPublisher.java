package com.quickbite.quickbite.common.event.publisher;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.cuisine.CuisineApprovedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRejectedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes cuisine domain events to the {@link QuickBiteTopics#CUISINE_EVENTS} topic
 * only AFTER the originating database transaction has committed.
 */
@Component
public class CuisineKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CuisineKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CuisineKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCuisineRequested(CuisineRequestedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.CUISINE_EVENTS, event.requestId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish CuisineRequestedEvent for request={}: {}",
                                event.requestId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] CuisineRequestedEvent published for request={}", event.requestId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCuisineApproved(CuisineApprovedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.CUISINE_EVENTS, event.requestId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish CuisineApprovedEvent for request={}: {}",
                                event.requestId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] CuisineApprovedEvent published for request={}", event.requestId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCuisineRejected(CuisineRejectedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.CUISINE_EVENTS, event.requestId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish CuisineRejectedEvent for request={}: {}",
                                event.requestId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] CuisineRejectedEvent published for request={}", event.requestId());
                    }
                });
    }
}
