package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.common.exception.ResourceNotFoundException;
import com.quickbite.quickbite.notification.dto.NotificationResponse;
import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.notification.repository.NotificationRepository;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationQueryServiceImpl implements NotificationQueryService {

    /** Hard cap on page size to prevent abusive requests. */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final NotificationRepository notificationRepository;

    public NotificationQueryServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, UUID cursor, int size) {
        int pageSize = Math.clamp(size, 1, MAX_PAGE_SIZE);

        // Fetch one extra to detect whether there's a next page
        List<Notification> fetched = notificationRepository.findWithCursor(
                userId, cursor, unreadOnly, Limit.of(pageSize + 1));

        List<NotificationResponse> mapped = fetched.stream()
                .map(NotificationResponse::from)
                .toList();

        return CursorPage.of(mapped, pageSize, NotificationResponse::id);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException(
                    "Notification not found or does not belong to this user: " + notificationId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }
}
