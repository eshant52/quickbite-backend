package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.restaurantapplication.RestaurantApplicationSubmittedEvent;
import com.quickbite.quickbite.notification.dto.RestaurantApplicationNotificationPayload;
import com.quickbite.quickbite.notification.model.RestaurantApplicationNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes restaurant application lifecycle events from the {@link QuickBiteTopics#RESTAURANT_APPLICATION_EVENTS}
 * stream and converts them into notifications delivered through registered channels.
 */
@Component
public class RestaurantApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(RestaurantApplicationEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public RestaurantApplicationEventListener(NotificationService notificationService,
                                              UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    @KafkaListener(
            topics = QuickBiteTopics.RESTAURANT_APPLICATION_EVENTS,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onApplicationEvent(RestaurantApplicationEvent event) {
        if (event == null) return;

        switch (event) {
            case RestaurantApplicationSubmittedEvent submitted -> handleApplicationSubmitted(submitted);
            case RestaurantApplicationApprovedEvent approved -> handleApplicationApproved(approved);
            case RestaurantApplicationRejectedEvent rejected -> handleApplicationRejected(rejected);
        }
    }

    private void handleApplicationSubmitted(RestaurantApplicationSubmittedEvent event) {
        List<User> admins = userRepository.findAllById(event.allottedAdminIds());
        if (admins.isEmpty()) {
            log.warn("[NOTIFICATION] No allotted admins found for application={}", event.applicationId());
            return;
        }

        admins.forEach(admin -> notificationService.send(new RestaurantApplicationNotificationPayload(
                admin,
                "Restaurant Application Assigned for Review",
                event.ownerName() + " submitted an application for \"" + event.restaurantName() + "\". You have been allotted to review this request.",
                RestaurantApplicationNotificationType.APPLICATION_SUBMITTED,
                event.applicationId(),
                event.restaurantName()
        )));

        log.info("[NOTIFICATION] Notified {} admin(s) for application={}", admins.size(), event.applicationId());
    }

    private void handleApplicationApproved(RestaurantApplicationApprovedEvent event) {
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

    private void handleApplicationRejected(RestaurantApplicationRejectedEvent event) {
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
}
