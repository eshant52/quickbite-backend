package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.cuisine.CuisineApprovedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRejectedEvent;
import com.quickbite.quickbite.common.event.cuisine.CuisineRequestedEvent;
import com.quickbite.quickbite.notification.dto.CuisineNotificationPayload;
import com.quickbite.quickbite.notification.model.CuisineNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class CuisineEventListener {

    private static final Logger log = LoggerFactory.getLogger(CuisineEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public CuisineEventListener(
            NotificationService notificationService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = QuickBiteTopics.CUISINE_EVENTS,
            groupId = "quickbite-notification-group",
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onCuisineEvent(String rawEvent) {
        CuisineEvent event = deserialize(rawEvent, CuisineEvent.class);
        if (event == null) return;

        switch (event) {
            case CuisineRequestedEvent requested -> handleCuisineRequested(requested);
            case CuisineApprovedEvent approved -> handleCuisineApproved(approved);
            case CuisineRejectedEvent rejected -> handleCuisineRejected(rejected);
        }
    }

    private void handleCuisineRequested(CuisineRequestedEvent event) {
        List<User> admins = userRepository.findAllById(event.allottedAdminIds());
        if (admins.isEmpty()) {
            log.warn("[NOTIFICATION] No allotted admins found to notify for cuisine request: {}", event.requestId());
            return;
        }

        admins.forEach(admin ->
                notificationService.send(new CuisineNotificationPayload(
                        admin,
                        "New Cuisine Review Assigned",
                        "A new cuisine \"" + event.cuisineName() +
                                "\" has been requested. You have been allotted to review this request.",
                        event.requestId(),
                        event.cuisineName(),
                        CuisineNotificationType.PENDING
                )));

        log.info("[NOTIFICATION] Sent cuisine allotment notification to {} admins for request={}",
                admins.size(), event.requestId());
    }

    private void handleCuisineApproved(CuisineApprovedEvent event) {
        if (event.requesterId() == null) {
            log.info("[NOTIFICATION] Cuisine approved: id={} name={} (no requester to notify)", event.cuisineId(), event.cuisineName());
            return;
        }

        userRepository.findById(event.requesterId()).ifPresentOrElse(
                owner -> {
                    notificationService.send(new CuisineNotificationPayload(
                            owner,
                            "Cuisine Approved! \uD83C\uDF89",
                            "Your requested cuisine \"" + event.cuisineName() + "\" has been approved and is now available on QuickBite.",
                            event.requestId(),
                            event.cuisineName(),
                            CuisineNotificationType.APPROVED
                    ));
                    log.info("[NOTIFICATION] Notified owner={} — cuisine={} approved", event.requesterId(), event.cuisineName());
                },
                () -> log.warn("[NOTIFICATION] Requester not found for approved cuisine event, requesterId={}", event.requesterId())
        );
    }

    private void handleCuisineRejected(CuisineRejectedEvent event) {
        if (event.requesterId() == null) {
            log.info("[NOTIFICATION] Cuisine rejected: name={} remarks={} (no requester to notify)", event.cuisineName(), event.rejectionRemarks());
            return;
        }

        userRepository.findById(event.requesterId()).ifPresentOrElse(
                owner -> {
                    notificationService.send(new CuisineNotificationPayload(
                            owner,
                            "Cuisine Request Rejected",
                            "Your requested cuisine \"" + event.cuisineName() + "\" was not approved. Reason: " + event.rejectionRemarks(),
                            event.requestId(),
                            event.cuisineName(),
                            CuisineNotificationType.REJECTED
                    ));
                    log.info("[NOTIFICATION] Notified owner={} — cuisine request={} rejected", event.requesterId(), event.requestId());
                },
                () -> log.warn("[NOTIFICATION] Requester not found for rejected cuisine event, requesterId={}", event.requesterId())
        );
    }

    private <T> T deserialize(String rawEvent, Class<T> type) {
        try {
            return objectMapper.readValue(rawEvent, type);
        } catch (Exception e) {
            log.error("[NOTIFICATION] Failed to deserialize Kafka event: {}", rawEvent, e);
            throw new RuntimeException("Failed to deserialize event", e);
        }
    }
}
