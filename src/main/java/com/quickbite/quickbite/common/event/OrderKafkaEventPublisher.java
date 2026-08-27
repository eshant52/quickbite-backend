package com.quickbite.quickbite.common.event;

import com.quickbite.quickbite.common.event.order.OrderCancelledEvent;
import com.quickbite.quickbite.common.event.order.OrderPlacedEvent;
import com.quickbite.quickbite.common.event.order.OrderStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes order domain events to the single {@link QuickBiteTopics#ORDER_EVENTS} topic,
 * keyed by orderId to guarantee strictly ordered partition-level delivery AFTER the DB transaction commits.
 */
@Component
public class OrderKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPlaced(OrderPlacedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.ORDER_EVENTS, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish OrderPlacedEvent for order={}: {}",
                                event.orderId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] OrderPlacedEvent published: order={} partition={} offset={}",
                                event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        kafkaTemplate.send(QuickBiteTopics.ORDER_EVENTS, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish OrderCancelledEvent for order={}: {}",
                                event.orderId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] OrderCancelledEvent published: order={}", event.orderId());
                    }
                });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.ORDER_EVENTS, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish OrderStatusChangedEvent for order={} status={}→{}: {}",
                                event.orderId(), event.previousStatus(), event.newStatus(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] OrderStatusChangedEvent published: order={} status={}→{}",
                                event.orderId(), event.previousStatus(), event.newStatus());
                    }
                });
    }
}
