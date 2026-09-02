package com.quickbite.quickbite.notification.listener;

import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationApprovedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationRejectedEvent;
import com.quickbite.quickbite.common.event.deliveryagentapplication.DeliveryAgentApplicationSubmittedEvent;
import com.quickbite.quickbite.notification.dto.DeliveryAgentApplicationNotificationPayload;
import com.quickbite.quickbite.notification.model.DeliveryAgentApplicationNotificationType;
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
class DeliveryAgentApplicationEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DeliveryAgentApplicationEventListener listener;

    private User admin;
    private User agent;
    private UUID adminId;
    private UUID agentUserId;
    private UUID appId;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        agentUserId = UUID.randomUUID();
        appId = UUID.randomUUID();

        admin = new User();
        admin.setId(adminId);
        admin.setName("Admin Alice");

        agent = new User();
        agent.setId(agentUserId);
        agent.setName("Driver Dave");
    }

    @Test
    @DisplayName("onDeliveryAgentApplicationEvent - handles submitted event and notifies allotted admins")
    void handlesSubmittedEvent() throws Exception {
        var event = new DeliveryAgentApplicationSubmittedEvent(
                appId, agentUserId, List.of(adminId), Instant.now());
        String raw = "{\"eventType\":\"SUBMITTED\"}";

        when(objectMapper.readValue(eq(raw), any(Class.class))).thenReturn(event);
        when(userRepository.findAllById(List.of(adminId))).thenReturn(List.of(admin));
        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(agent));

        listener.onDeliveryAgentApplicationEvent(raw);

        ArgumentCaptor<DeliveryAgentApplicationNotificationPayload> captor =
                ArgumentCaptor.forClass(DeliveryAgentApplicationNotificationPayload.class);
        verify(notificationService).send(captor.capture());

        DeliveryAgentApplicationNotificationPayload payload = captor.getValue();
        assertThat(payload.recipient()).isEqualTo(admin);
        assertThat(payload.type()).isEqualTo(DeliveryAgentApplicationNotificationType.APPLICATION_SUBMITTED);
        assertThat(payload.applicationId()).isEqualTo(appId);
        assertThat(payload.agentName()).isEqualTo("Driver Dave");
    }

    @Test
    @DisplayName("onDeliveryAgentApplicationEvent - handles approved event and notifies driver")
    void handlesApprovedEvent() throws Exception {
        var event = new DeliveryAgentApplicationApprovedEvent(
                appId, UUID.randomUUID(), agentUserId, adminId, Instant.now());
        String raw = "{\"eventType\":\"APPROVED\"}";

        when(objectMapper.readValue(eq(raw), any(Class.class))).thenReturn(event);
        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(agent));

        listener.onDeliveryAgentApplicationEvent(raw);

        ArgumentCaptor<DeliveryAgentApplicationNotificationPayload> captor =
                ArgumentCaptor.forClass(DeliveryAgentApplicationNotificationPayload.class);
        verify(notificationService).send(captor.capture());

        DeliveryAgentApplicationNotificationPayload payload = captor.getValue();
        assertThat(payload.recipient()).isEqualTo(agent);
        assertThat(payload.type()).isEqualTo(DeliveryAgentApplicationNotificationType.APPLICATION_APPROVED);
        assertThat(payload.applicationId()).isEqualTo(appId);
    }

    @Test
    @DisplayName("onDeliveryAgentApplicationEvent - handles rejected event and notifies driver with remarks")
    void handlesRejectedEvent() throws Exception {
        var event = new DeliveryAgentApplicationRejectedEvent(
                appId, agentUserId, adminId, "Driving license expired", Instant.now());
        String raw = "{\"eventType\":\"REJECTED\"}";

        when(objectMapper.readValue(eq(raw), any(Class.class))).thenReturn(event);
        when(userRepository.findById(agentUserId)).thenReturn(Optional.of(agent));

        listener.onDeliveryAgentApplicationEvent(raw);

        ArgumentCaptor<DeliveryAgentApplicationNotificationPayload> captor =
                ArgumentCaptor.forClass(DeliveryAgentApplicationNotificationPayload.class);
        verify(notificationService).send(captor.capture());

        DeliveryAgentApplicationNotificationPayload payload = captor.getValue();
        assertThat(payload.recipient()).isEqualTo(agent);
        assertThat(payload.type()).isEqualTo(DeliveryAgentApplicationNotificationType.APPLICATION_REJECTED);
        assertThat(payload.message()).contains("Driving license expired");
    }
}
