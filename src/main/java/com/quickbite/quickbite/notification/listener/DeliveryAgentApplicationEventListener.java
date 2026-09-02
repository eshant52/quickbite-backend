package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationSubmittedEvent;
import com.quickbite.quickbite.notification.dto.DeliveryAgentApplicationNotificationPayload;
import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Consumes delivery agent application lifecycle events from Kafka and delivers notifications.
 */
@Component
public class DeliveryAgentApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryAgentApplicationEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public DeliveryAgentApplicationEventListener(
            NotificationService notificationService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = QuickBiteTopics.DELIVERY_AGENT_APPLICATION_EVENTS,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onDeliveryAgentApplicationEvent(String rawEvent) {
        DeliveryAgentApplicationEvent event = deserialize(rawEvent, DeliveryAgentApplicationEvent.class);
        if (event == null) return;

        switch (event) {
            case DeliveryAgentApplicationSubmittedEvent submitted -> handleApplicationSubmitted(submitted);
            case DeliveryAgentApplicationApprovedEvent approved -> handleApplicationApproved(approved);
            case DeliveryAgentApplicationRejectedEvent rejected -> handleApplicationRejected(rejected);
        }
    }

    private void handleApplicationSubmitted(DeliveryAgentApplicationSubmittedEvent event) {
        List<User> admins = userRepository.findAllById(event.allottedAdminIds());
        if (admins.isEmpty()) {
            log.warn("[NOTIFICATION] No allotted admins found for delivery agent application={}", event.applicationId());
            return;
        }

        User agent = userRepository.findById(event.agentUserId()).orElse(null);
        String agentName = agent != null ? agent.getName() : "A delivery agent applicant";

        admins.forEach(admin -> notificationService.send(new DeliveryAgentApplicationNotificationPayload(
                admin,
                "Delivery Agent Application Assigned for Review",
                agentName + " submitted a delivery partner application. You have been allotted to review this request.",
                DeliveryAgentApplicationNotificationType.APPLICATION_SUBMITTED,
                event.applicationId(),
                agentName
        )));

        log.info("[NOTIFICATION] Notified {} admin(s) for delivery agent application={}", admins.size(), event.applicationId());
    }

    private void handleApplicationApproved(DeliveryAgentApplicationApprovedEvent event) {
        userRepository.findById(event.agentUserId()).ifPresentOrElse(
                agent -> {
                    notificationService.send(new DeliveryAgentApplicationNotificationPayload(
                            agent,
                            "Welcome to QuickBite Delivery! \uD83C\uDF89",
                            "Your delivery agent application has been approved! You are now an active delivery partner on QuickBite.",
                            DeliveryAgentApplicationNotificationType.APPLICATION_APPROVED,
                            event.applicationId(),
                            agent.getName()
                    ));
                    log.info("[NOTIFICATION] Notified agent={} — delivery agent application={} approved", event.agentUserId(), event.applicationId());
                },
                () -> log.warn("[NOTIFICATION] Agent user not found for approved event, agentUserId={}", event.agentUserId())
        );
    }

    private void handleApplicationRejected(DeliveryAgentApplicationRejectedEvent event) {
        userRepository.findById(event.agentUserId()).ifPresentOrElse(
                agent -> {
                    notificationService.send(new DeliveryAgentApplicationNotificationPayload(
                            agent,
                            "Delivery Agent Application Requires Changes",
                            "Your application was not approved. Reason: " + event.rejectionRemarks()
                                    + " You can reopen and update your application from the dashboard.",
                            DeliveryAgentApplicationNotificationType.APPLICATION_REJECTED,
                            event.applicationId(),
                            agent.getName()
                    ));
                    log.info("[NOTIFICATION] Notified agent={} — delivery agent application={} rejected", event.agentUserId(), event.applicationId());
                },
                () -> log.warn("[NOTIFICATION] Agent user not found for rejected event, agentUserId={}", event.agentUserId())
        );
    }

    private <T> T deserialize(String rawEvent, Class<T> type) {
        try {
            return objectMapper.readValue(rawEvent, type);
        } catch (Exception ex) {
            log.error("[NOTIFICATION] Failed to deserialize Kafka event as {}: {}", type.getSimpleName(), ex.getMessage(), ex);
            throw new RuntimeException("Failed to deserialize event", ex);
        }
    }
}
