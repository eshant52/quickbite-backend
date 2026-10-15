package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.notification.dto.CuisineNotificationPayload;
import com.quickbite.quickbite.notification.dto.NotificationPayload;
import com.quickbite.quickbite.notification.dto.OrderNotificationPayload;
import com.quickbite.quickbite.notification.model.CuisineNotificationType;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.service.strategy.NotificationChannel;
import com.quickbite.quickbite.notification.service.strategy.NotificationDeliveryStrategy;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationDeliveryStrategy inAppStrategy;

    @Mock
    private NotificationDeliveryStrategy emailStrategy;

    @Mock
    private NotificationDeliveryStrategy pushStrategy;

    private NotificationServiceImpl notificationService;
    private NotificationPayload testPayload;
    private User recipient;

    @BeforeEach
    void setUp() {
        lenient().when(inAppStrategy.channel()).thenReturn(NotificationChannel.IN_APP);
        lenient().when(emailStrategy.channel()).thenReturn(NotificationChannel.EMAIL);
        lenient().when(pushStrategy.channel()).thenReturn(NotificationChannel.PUSH);

        notificationService = new NotificationServiceImpl(List.of(inAppStrategy, emailStrategy, pushStrategy));

        recipient = new User();
        recipient.setId(UUID.randomUUID());
        recipient.setEmail("user@example.com");

        testPayload = new CuisineNotificationPayload(
                recipient,
                "New Cuisine Added",
                "Italian cuisine is now live!",
                UUID.randomUUID(),
                "Italian",
                CuisineNotificationType.APPROVED
        );
    }

    @Test
    @DisplayName("send() broadcasts to all channels by default")
    void send_broadcastsToAllChannels() {
        notificationService.send(testPayload);

        verify(inAppStrategy, times(1)).deliver(testPayload);
        verify(emailStrategy, times(1)).deliver(testPayload);
        verify(pushStrategy, times(1)).deliver(testPayload);
    }

    @Test
    @DisplayName("sendToChannel() delivers only to the specified channel")
    void sendToChannel_deliversOnlyToSpecifiedChannel() {
        notificationService.sendToChannel(testPayload, NotificationChannel.EMAIL);

        verify(emailStrategy, times(1)).deliver(testPayload);
        verify(inAppStrategy, never()).deliver(any());
        verify(pushStrategy, never()).deliver(any());
    }

    @Test
    @DisplayName("sendToChannels() delivers only to the requested subset of channels")
    void sendToChannels_deliversOnlyToTargetChannels() {
        notificationService.sendToChannels(testPayload, Set.of(NotificationChannel.IN_APP, NotificationChannel.PUSH));

        verify(inAppStrategy, times(1)).deliver(testPayload);
        verify(pushStrategy, times(1)).deliver(testPayload);
        verify(emailStrategy, never()).deliver(any());
    }

    @Test
    @DisplayName("sendToChannel() safely handles unmapped channel without throwing")
    void sendToChannel_handlesUnmappedChannelGracefully() {
        NotificationServiceImpl partialService = new NotificationServiceImpl(List.of(inAppStrategy));

        // Attempting to send to EMAIL when only IN_APP is registered
        partialService.sendToChannel(testPayload, NotificationChannel.EMAIL);

        verify(inAppStrategy, never()).deliver(any());
    }

    @Test
    @DisplayName("deliverSafely catches strategy failure and does not block other channels")
    void send_handlesStrategyExceptionGracefully() {
        doThrow(new RuntimeException("Email gateway timeout")).when(emailStrategy).deliver(any());

        notificationService.send(testPayload);

        verify(emailStrategy, times(1)).deliver(testPayload);
        verify(inAppStrategy, times(1)).deliver(testPayload);
        verify(pushStrategy, times(1)).deliver(testPayload);
    }

    @Test
    @DisplayName("send() honors custom targetChannels defined on payload")
    void send_honorsCustomPayloadTargetChannels() {
        OrderNotificationPayload inAppOnlyPayload = new OrderNotificationPayload(
                recipient,
                "Order Status",
                "Your food is being prepared",
                OrderNotificationType.PREPARING,
                null,
                Set.of(NotificationChannel.IN_APP)
        );

        notificationService.send(inAppOnlyPayload);

        verify(inAppStrategy, times(1)).deliver(inAppOnlyPayload);
        verify(emailStrategy, never()).deliver(any());
        verify(pushStrategy, never()).deliver(any());
    }

    @Test
    @DisplayName("sendToChannels() with empty set does nothing")
    void sendToChannels_withEmptySet_doesNothing() {
        notificationService.sendToChannels(testPayload, Set.of());

        verify(inAppStrategy, never()).deliver(any());
        verify(emailStrategy, never()).deliver(any());
        verify(pushStrategy, never()).deliver(any());
    }

    @Test
    @DisplayName("sendToChannel() with null parameters does not throw exception")
    void send_withNullParameters_handledSafely() {
        notificationService.send(null);
        notificationService.sendToChannel(null, NotificationChannel.EMAIL);
        notificationService.sendToChannel(testPayload, null);
        notificationService.sendToChannels(null, Set.of(NotificationChannel.EMAIL));
        notificationService.sendToChannels(testPayload, null);

        verify(inAppStrategy, never()).deliver(any());
        verify(emailStrategy, never()).deliver(any());
        verify(pushStrategy, never()).deliver(any());
    }
}
