package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.payment.PaymentStatusChangedEvent;
import com.quickbite.quickbite.notification.dto.PaymentNotificationPayload;
import com.quickbite.quickbite.notification.model.PaymentNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.payment.model.Payment;
import com.quickbite.quickbite.payment.model.PaymentStatus;
import com.quickbite.quickbite.payment.repository.PaymentRepository;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class PaymentEventListener {
    private final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventListener(NotificationService notificationService,
                                UserRepository userRepository,
                                PaymentRepository paymentRepository,
                                ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    // --------------------------------------------------------------------------
    // PAYMENT_STATUS_CHANGED — notify customer
    // --------------------------------------------------------------------------

    @KafkaListener(
            topics = QuickBiteTopics.PAYMENT_EVENTS,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onPaymentStatusChanged(String rawEvent) {
        PaymentStatusChangedEvent event = deserialize(rawEvent, PaymentStatusChangedEvent.class);
        if (event == null) return;

        User customer = userRepository.findById(event.customerId()).orElse(null);
        if (customer == null) {
            log.error("[NOTIFICATION] Customer not found for payment: {}", event.paymentId());
            return;
        }

        Payment payment = paymentRepository.findById(event.paymentId()).orElse(null);
        if (payment == null) {
            log.error("[NOTIFICATION] Payment not found: {}", event.paymentId());
            return;
        }

        Map<String, String> titleAndMessage = getTitleAndMessage(event.newStatus());
        PaymentNotificationType notificationType = getPaymentNotificationType(event.newStatus());

        notificationService.send(new PaymentNotificationPayload(
                customer,
                titleAndMessage.get("title"),
                titleAndMessage.get("message"),
                notificationType,
                payment
        ));

        log.info("[NOTIFICATION] Payment sent for payment id: {}", event.paymentId());
    }

    private <T> T deserialize(String rawEvent, Class<T> type) {
        try {
            return objectMapper.readValue(rawEvent, type);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to deserialize event: {}", rawEvent, e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }

    private Map<String, String> getTitleAndMessage(PaymentStatus newStatus) {
        return switch (newStatus) {
            case PENDING -> Map.of(
                    "title", "Payment Pending",
                    "message", "Your payment is currently pending. Please wait for confirmation."
            );
            case SUCCESS -> Map.of(
                    "title", "Payment Successful",
                    "message", "Your payment has been successfully processed. Thank you!"
            );
            case FAILED -> Map.of(
                    "title", "Payment Failed",
                    "message", "Unfortunately, your payment could not be processed. Please try again."
            );
            case CANCELLED -> Map.of(
                    "title", "Payment Cancelled",
                    "message", "Your payment has been cancelled. If this was a mistake, please try again."
            );
            case REFUNDED -> Map.of(
                    "title", "Payment Refunded",
                    "message", "Your payment has been refunded. Please check your account for details."
            );
        };
    }

    private PaymentNotificationType getPaymentNotificationType(PaymentStatus newStatus) {
        return switch (newStatus) {
            case PENDING -> PaymentNotificationType.PENDING;
            case SUCCESS -> PaymentNotificationType.SUCCESS;
            case FAILED -> PaymentNotificationType.FAILED;
            case CANCELLED -> PaymentNotificationType.CANCELLED;
            case REFUNDED -> PaymentNotificationType.REFUNDED;
        };
    }
}
