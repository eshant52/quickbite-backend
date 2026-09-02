package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.vehicleapplication.VehicleApplicationSubmittedEvent;
import com.quickbite.quickbite.notification.dto.VehicleApplicationNotificationPayload;
import com.quickbite.quickbite.notification.model.VehicleApplicationNotificationType;
import com.quickbite.quickbite.notification.service.NotificationService;
import com.quickbite.quickbite.user.model.User;
import com.quickbite.quickbite.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleApplicationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VehicleApplicationEventListener listener;

    private User admin;
    private User driver;
    private UUID adminId;
    private UUID driverUserId;
    private UUID appId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        driverUserId = UUID.randomUUID();
        appId = UUID.randomUUID();

        admin = new User();
        admin.setId(adminId);
        admin.setName("Admin Alice");

        driver = new User();
        driver.setId(driverUserId);
        driver.setName("Driver Dave");
    }

    @Test
    @DisplayName("onVehicleApplicationEvent - handles submitted event and notifies allotted admins")
    void handlesSubmittedEvent() throws Exception {
        var event = new VehicleApplicationSubmittedEvent(
                appId, driverUserId, UUID.randomUUID(), "Yamaha FZ", List.of(adminId), Instant.now());
        String raw = "{\"eventType\":\"SUBMITTED\"}";

        when(objectMapper.readValue(eq(raw), any(Class.class))).thenReturn(event);
        when(userRepository.findAllById(List.of(adminId))).thenReturn(List.of(admin));

        listener.onVehicleApplicationEvent(raw);

        ArgumentCaptor<VehicleApplicationNotificationPayload> captor =
                ArgumentCaptor.forClass(VehicleApplicationNotificationPayload.class);
        verify(notificationService).send(captor.capture());

        VehicleApplicationNotificationPayload payload = captor.getValue();
        assertThat(payload.recipient()).isEqualTo(admin);
        assertThat(payload.type()).isEqualTo(VehicleApplicationNotificationType.APPLICATION_SUBMITTED);
        assertThat(payload.applicationId()).isEqualTo(appId);
        assertThat(payload.vehicleName()).isEqualTo("Yamaha FZ");
    }

    @Test
    @DisplayName("onVehicleApplicationEvent - handles approved event and notifies driver")
    void handlesApprovedEvent() throws Exception {
        var event = new VehicleApplicationApprovedEvent(
                appId, driverUserId, UUID.randomUUID(), "Yamaha FZ", adminId, Instant.now());
        String raw = "{\"eventType\":\"APPROVED\"}";

        when(objectMapper.readValue(eq(raw), any(Class.class))).thenReturn(event);
        when(userRepository.findById(driverUserId)).thenReturn(Optional.of(driver));

        listener.onVehicleApplicationEvent(raw);

        ArgumentCaptor<VehicleApplicationNotificationPayload> captor =
                ArgumentCaptor.forClass(VehicleApplicationNotificationPayload.class);
        verify(notificationService).send(captor.capture());

        VehicleApplicationNotificationPayload payload = captor.getValue();
        assertThat(payload.recipient()).isEqualTo(driver);
        assertThat(payload.type()).isEqualTo(VehicleApplicationNotificationType.APPLICATION_APPROVED);
        assertThat(payload.applicationId()).isEqualTo(appId);
    }

    @Test
    @DisplayName("onVehicleApplicationEvent - handles rejected event and notifies driver with remarks")
    void handlesRejectedEvent() throws Exception {
        var event = new VehicleApplicationRejectedEvent(
                appId, driverUserId, UUID.randomUUID(), "Yamaha FZ", "RC document is blurry", adminId, Instant.now());
        String raw = "{\"eventType\":\"REJECTED\"}";

        when(objectMapper.readValue(eq(raw), any(Class.class))).thenReturn(event);
        when(userRepository.findById(driverUserId)).thenReturn(Optional.of(driver));

        listener.onVehicleApplicationEvent(raw);

        ArgumentCaptor<VehicleApplicationNotificationPayload> captor =
                ArgumentCaptor.forClass(VehicleApplicationNotificationPayload.class);
        verify(notificationService).send(captor.capture());

        VehicleApplicationNotificationPayload payload = captor.getValue();
        assertThat(payload.recipient()).isEqualTo(driver);
        assertThat(payload.type()).isEqualTo(VehicleApplicationNotificationType.APPLICATION_REJECTED);
        assertThat(payload.message()).contains("RC document is blurry");
    }
}
