package com.quickbite.quickbite.common.event.publisher;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.payment.PaymentStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentKafkaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentKafkaEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentEvent(PaymentStatusChangedEvent event) {
        kafkaTemplate.send(QuickBiteTopics.PAYMENT_EVENTS, event.paymentId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Kafka] Failed to publish PaymentStatusChangedEvent for payment={}: {}",
                                event.paymentId(), ex.getMessage(), ex);
                    } else {
                        log.debug("[Kafka] Successfully published PaymentStatusChangedEvent for payment={} status={}->{}",
                                event.paymentId(), event.previousStatus(), event.newStatus());
                    }
                });
    }
}
