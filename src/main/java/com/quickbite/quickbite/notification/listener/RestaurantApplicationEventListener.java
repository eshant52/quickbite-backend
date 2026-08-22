package com.quickbite.quickbite.notification.listener;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationSubmittedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationRejectedEvent;
import com.quickbite.quickbite.notification.dto.RestaurantApplicationNotificationPayload;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.model.UserRole;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes restaurant application lifecycle events from Kafka and converts
 * them into notifications delivered through all registered channels
 * (in-app, email, push — see {@link com.quickbite.quickbite.notification.service.strategy}).
 *
 * Uses {@code containerFactory = "stringKafkaListenerContainerFactory"} to receive
 * raw JSON strings, which are then explicitly deserialized with {@link ObjectMapper}.
 * This keeps listeners decoupled from type-header configuration and makes the
 * deserialization contract visible at the call site.
 */
@Component
public class RestaurantApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(RestaurantApplicationEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public RestaurantApplicationEventListener(NotificationService notificationService,
                                              UserRepository userRepository,
                                              ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // APPLICATION_SUBMITTED — notify all admins
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = QuickBiteTopics.RESTAURANT_APPLICATION_SUBMITTED,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onApplicationSubmitted(String rawEvent) {
        RestaurantApplicationSubmittedEvent event = deserialize(rawEvent, RestaurantApplicationSubmittedEvent.class);
        if (event == null) return;

        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        if (admins.isEmpty()) {
            log.warn("[NOTIFICATION] No admins found to notify for application={}", event.applicationId());
            return;
        }

        admins.forEach(admin -> notificationService.send(new RestaurantApplicationNotificationPayload(
                admin,
                "New Restaurant Application",
                event.ownerName() + " submitted an application for \"" + event.restaurantName() + "\". Review it now.",
                RestaurantApplicationNotificationType.APPLICATION_SUBMITTED,
                event.applicationId(),
                event.restaurantName()
        )));

        log.info("[NOTIFICATION] Notified {} admin(s) for application={}", admins.size(), event.applicationId());
    }

    // -------------------------------------------------------------------------
    // APPLICATION_APPROVED — notify the restaurant owner
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = QuickBiteTopics.RESTAURANT_APPLICATION_APPROVED,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onRestaurantApproved(String rawEvent) {
        RestaurantApplicationApprovedEvent event = deserialize(rawEvent, RestaurantApplicationApprovedEvent.class);
        if (event == null) return;

        userRepository.findById(event.ownerId()).ifPresentOrElse(
                owner -> {
                    notificationService.send(new RestaurantApplicationNotificationPayload(
                            owner,
                            "Your Restaurant is Live! \uD83C\uDF89",
                            "\"" + event.restaurantName() + "\" has been approved and is now visible to customers on QuickBite.",
                            RestaurantApplicationNotificationType.APPLICATION_APPROVED,
                            event.applicationId(),
                            event.restaurantName()
                    ));
                    log.info("[NOTIFICATION] Notified owner={} — restaurant={} approved", event.ownerId(), event.restaurantId());
                },
                () -> log.warn("[NOTIFICATION] Owner not found for approved event, ownerId={}", event.ownerId())
        );
    }

    // -------------------------------------------------------------------------
    // APPLICATION_REJECTED — notify the restaurant owner
    // -------------------------------------------------------------------------

    @KafkaListener(
            topics = QuickBiteTopics.RESTAURANT_APPLICATION_REJECTED,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onRestaurantRejected(String rawEvent) {
        RestaurantApplicationRejectedEvent event = deserialize(rawEvent, RestaurantApplicationRejectedEvent.class);
        if (event == null) return;

        userRepository.findById(event.ownerId()).ifPresentOrElse(
                owner -> {
                    notificationService.send(new RestaurantApplicationNotificationPayload(
                            owner,
                            "Application Requires Changes",
                            "Your application for \"" + event.restaurantName() + "\" needs updates. "
                                    + "Reason: " + event.rejectionRemarks()
                                    + " You can reopen and resubmit from the dashboard.",
                            RestaurantApplicationNotificationType.APPLICATION_REJECTED,
                            event.applicationId(),
                            event.restaurantName()
                    ));
                    log.info("[NOTIFICATION] Notified owner={} — application={} rejected", event.ownerId(), event.applicationId());
                },
                () -> log.warn("[NOTIFICATION] Owner not found for rejected event, ownerId={}", event.ownerId())
        );
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private <T> T deserialize(String rawEvent, Class<T> type) {
        try {
            return objectMapper.readValue(rawEvent, type);
        } catch (JacksonException ex) {
            log.error("[NOTIFICATION] Failed to deserialize Kafka event as {}: {}", type.getSimpleName(), ex.getMessage(), ex);
            return null;
        }
    }
}
