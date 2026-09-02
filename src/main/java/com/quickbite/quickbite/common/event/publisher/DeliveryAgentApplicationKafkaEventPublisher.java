package com.quickbite.quickbite.common.event.publisher;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DeliveryAgentApplicationKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAgentApplicationKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public DeliveryAgentApplicationKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryAgentApplicationSubmittedEvent(DeliveryAgentApplicationSubmittedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.DELIVERY_AGENT_APPLICATION_EVENTS, event.applicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish DeliveryAgentApplicationEvent for application={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published DeliveryAgentApplicationEvent for application={} type={}",
                                event.applicationId(), event.getClass().getSimpleName());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryAgentApplicationApprovedEvent(DeliveryAgentApplicationApprovedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.DELIVERY_AGENT_APPLICATION_EVENTS, event.applicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish DeliveryAgentApplicationApprovedEvent for application={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published DeliveryAgentApplicationApprovedEvent for application={} type={}",
                                event.applicationId(), event.getClass().getSimpleName());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDeliveryAgentApplicationRejectedEvent(DeliveryAgentApplicationRejectedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.DELIVERY_AGENT_APPLICATION_EVENTS, event.applicationId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish DeliveryAgentApplicationRejectedEvent for application={}: {}",
                                event.applicationId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published DeliveryAgentApplicationRejectedEvent for application={} type={}",
                                event.applicationId(), event.getClass().getSimpleName());
                    }
                });
    }
}
