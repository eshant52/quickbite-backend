package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.QuickBiteTopics;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationSubmittedEvent;
import com.quickbite.quickbite.notification.dto.VehicleApplicationNotificationPayload;
import com.quickbite.quickbite.notification.model.VehicleApplicationNotificationType;
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
 * Consumes vehicle application lifecycle events from Kafka and delivers notifications.
 */
@Component
public class VehicleApplicationEventListener {

    private static final Logger log = LoggerFactory.getLogger(VehicleApplicationEventListener.class);

    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public VehicleApplicationEventListener(
            NotificationService notificationService,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            groupId = "quickbite-notification-group",
            topics = QuickBiteTopics.VEHICLE_APPLICATION_EVENTS,
            containerFactory = "stringKafkaListenerContainerFactory"
    )
    public void onVehicleApplicationEvent(String rawEvent) {
        VehicleApplicationEvent event = deserialize(rawEvent, VehicleApplicationEvent.class);
        if (event == null) return;

        switch (event) {
            case VehicleApplicationSubmittedEvent submitted -> handleVehicleApplicationSubmitted(submitted);
            case VehicleApplicationApprovedEvent approved -> handleVehicleApplicationApproved(approved);
            case VehicleApplicationRejectedEvent rejected -> handleVehicleApplicationRejected(rejected);
        }
    }

    private void handleVehicleApplicationSubmitted(VehicleApplicationSubmittedEvent event) {
        log.info("[NOTIFICATION] Handling vehicle application submitted event: {}", event.applicationId());

        List<User> admins = userRepository.findAllById(event.allottedAdminIds());
        if (admins.isEmpty()) {
            log.warn("[NOTIFICATION] No admins found for vehicle application submitted event: {}", event.applicationId());
            return;
        }

        admins.forEach(admin -> notificationService.send(new VehicleApplicationNotificationPayload(
                admin,
                "Vehicle Application Assigned for Review",
                "A vehicle application for \"" + event.vehicleName() + "\" has been submitted. You have been allotted to review this request.",
                VehicleApplicationNotificationType.APPLICATION_SUBMITTED,
                event.applicationId(),
                event.vehicleName()
        )));

        log.info("[NOTIFICATION] Notified {} admin(s) for vehicle application={}", admins.size(), event.applicationId());
    }

    private void handleVehicleApplicationApproved(VehicleApplicationApprovedEvent event) {
        userRepository.findById(event.agentUserId()).ifPresentOrElse(
                driver -> {
                    notificationService.send(new VehicleApplicationNotificationPayload(
                            driver,
                            "Vehicle Approved! \uD83C\uDF89",
                            "Your vehicle \"" + event.vehicleName() + "\" has been approved and added to your profile.",
                            VehicleApplicationNotificationType.APPLICATION_APPROVED,
                            event.applicationId(),
                            event.vehicleName()
                    ));
                    log.info("[NOTIFICATION] Notified driver={} — vehicle application={} approved", event.agentUserId(), event.applicationId());
                },
                () -> log.warn("[NOTIFICATION] Driver not found for approved event, agentUserId={}", event.agentUserId())
        );
    }

    private void handleVehicleApplicationRejected(VehicleApplicationRejectedEvent event) {
        userRepository.findById(event.agentUserId()).ifPresentOrElse(
                driver -> {
                    notificationService.send(new VehicleApplicationNotificationPayload(
                            driver,
                            "Vehicle Application Requires Changes",
                            "Your vehicle application for \"" + event.vehicleName() + "\" was not approved. Reason: " + event.rejectionReason(),
                            VehicleApplicationNotificationType.APPLICATION_REJECTED,
                            event.applicationId(),
                            event.vehicleName()
                    ));
                    log.info("[NOTIFICATION] Notified driver={} — vehicle application={} rejected", event.agentUserId(), event.applicationId());
                },
                () -> log.warn("[NOTIFICATION] Driver not found for rejected event, agentUserId={}", event.agentUserId())
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
