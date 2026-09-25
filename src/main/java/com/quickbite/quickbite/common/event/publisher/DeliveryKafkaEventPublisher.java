package com.quickbite.quickbite.common.event.publisher;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.delivery.DeliveryAgentAssignedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryDispatchExhaustedEvent;
import com.quickbite.quickbite.common.event.delivery.DeliveryOfferCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes delivery domain events to the {@link QuickBiteTopics#DELIVERY_EVENTS} topic,
 * keyed by orderId to guarantee strictly ordered partition-level delivery AFTER the DB transaction commits.
 */
@Component
public class DeliveryKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DeliveryKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryOfferCreated(DeliveryOfferCreatedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.DELIVERY_EVENTS, event.orderId().toString(), event)
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish DeliveryOfferCreatedEvent for order={}: {}",
                                event.orderId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] DeliveryOfferCreatedEvent published: order={} offer={}",
                                event.orderId(), event.offerId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryAgentAssigned(DeliveryAgentAssignedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.DELIVERY_EVENTS, event.orderId().toString(), event)
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish DeliveryAgentAssignedEvent for order={}: {}",
                                event.orderId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] DeliveryAgentAssignedEvent published: order={} agent={}",
                                event.orderId(), event.agentId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveryDispatchExhausted(DeliveryDispatchExhaustedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.DELIVERY_EVENTS, event.orderId().toString(), event)
                .whenComplete((_, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish DeliveryDispatchExhaustedEvent for order={}: {}",
                                event.orderId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] DeliveryDispatchExhaustedEvent published: order={}", event.orderId());
                    }
                });
    }
}
