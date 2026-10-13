package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.notification.dto.NotificationResponse;
import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.model.OrderNotification;
import com.quickbite.quickbite.notification.model.OrderNotificationType;
import com.quickbite.quickbite.notification.repository.NotificationRepository;
import com.quickbite.quickbite.order.model.Order;
import com.quickbite.quickbite.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationQueryServiceImpl notificationQueryService;

    private UUID userId;
    private User recipient;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        recipient = new User();
        recipient.setId(userId);
    }

    private OrderNotification createNotification(UUID id, String title, Instant createdAt) {
        OrderNotification notification = new OrderNotification();
        notification.setId(id);
        notification.setTitle(title);
        notification.setMessage("Test message for " + title);
        notification.setRecipient(recipient);
        notification.setRead(false);
        notification.setType(OrderNotificationType.PLACED);
        notification.setCreatedAt(createdAt);

        Order order = new Order();
        order.setId(UUID.randomUUID());
        notification.setOrder(order);

        return notification;
    }

    @Test
    @DisplayName("getNotifications: first page returns newest notifications with nextCursor")
    void getNotifications_firstPage_descendingOrder() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        // Descending order (newest first)
        OrderNotification n1 = createNotification(id1, "Newest", Instant.now());
        OrderNotification n2 = createNotification(id2, "Middle", Instant.now().minusSeconds(60));
        OrderNotification n3 = createNotification(id3, "Oldest", Instant.now().minusSeconds(120));

        when(notificationRepository.findWithCursor(eq(userId), isNull(), eq(false), eq(Limit.of(3))))
                .thenReturn(List.of(n1, n2, n3));

        CursorPage<NotificationResponse> result = notificationQueryService.getNotifications(userId, false, null, 2);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).id()).isEqualTo(id1);
        assertThat(result.content().get(1).id()).isEqualTo(id2);
        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextCursor()).isEqualTo(id2);

        verify(notificationRepository).findWithCursor(eq(userId), isNull(), eq(false), eq(Limit.of(3)));
    }

    @Test
    @DisplayName("getNotifications: subsequent page passes cursor to repository")
    void getNotifications_subsequentPage_passesCursor() {
        UUID cursor = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        OrderNotification n3 = createNotification(id3, "Oldest", Instant.now().minusSeconds(120));

        when(notificationRepository.findWithCursor(eq(userId), eq(cursor), eq(false), eq(Limit.of(3))))
                .thenReturn(List.of(n3));

        CursorPage<NotificationResponse> result = notificationQueryService.getNotifications(userId, false, cursor, 2);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(id3);
        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextCursor()).isNull();

        verify(notificationRepository).findWithCursor(eq(userId), eq(cursor), eq(false), eq(Limit.of(3)));
    }

    @Test
    @DisplayName("getNotifications: unreadOnly flag is forwarded to repository")
    void getNotifications_unreadOnly_passesFlag() {
        when(notificationRepository.findWithCursor(eq(userId), isNull(), eq(true), eq(Limit.of(11))))
                .thenReturn(List.of());

        CursorPage<NotificationResponse> result = notificationQueryService.getNotifications(userId, true, null, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.hasMore()).isFalse();
        verify(notificationRepository).findWithCursor(eq(userId), isNull(), eq(true), eq(Limit.of(11)));
    }

    @Test
    @DisplayName("getUnreadCount: delegates to repository")
    void getUnreadCount_delegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(userId)).thenReturn(5L);

        long count = notificationQueryService.getUnreadCount(userId);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository).countByRecipientIdAndIsReadFalse(userId);
    }

    @Test
    @DisplayName("markAsRead: successfully updates read status")
    void markAsRead_success() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.markAsRead(notificationId, userId)).thenReturn(1);

        notificationQueryService.markAsRead(notificationId, userId);

        verify(notificationRepository).markAsRead(notificationId, userId);
    }

    @Test
    @DisplayName("markAsRead: throws ResourceNotFoundException when no rows updated")
    void markAsRead_notFound_throwsException() {
        UUID notificationId = UUID.randomUUID();
        when(notificationRepository.markAsRead(notificationId, userId)).thenReturn(0);

        assertThatThrownBy(() -> notificationQueryService.markAsRead(notificationId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository).markAsRead(notificationId, userId);
    }

    @Test
    @DisplayName("markAllAsRead: delegates to repository")
    void markAllAsRead_delegatesToRepository() {
        notificationQueryService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsRead(userId);
    }
}
