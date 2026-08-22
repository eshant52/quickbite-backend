package com.quickbite.quickbite.notification.service;

import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.notification.dto.NotificationResponse;

import java.util.UUID;

/**
 * Read-side (query) service for notifications.
 *
 * Kept separate from {@link NotificationService} (write-side) following CQRS
 * and Interface Segregation — callers that only read do not depend on the write interface.
 *
 * All list methods use cursor-based pagination backed by UUIDv7 time-ordering.
 */
public interface NotificationQueryService {

    /**
     * Returns a cursor-based page of notifications for the given user.
     *
     * @param userId     The authenticated user's ID.
     * @param unreadOnly If {@code true}, only unread notifications are returned.
     * @param cursor     The last UUID seen by the client (exclusive lower bound).
     *                   {@code null} for the first page.
     * @param size       Maximum number of items to return (capped at 100).
     */
    CursorPage<NotificationResponse> getNotifications(UUID userId, boolean unreadOnly, UUID cursor, int size);

    /** Returns the number of unread notifications — used for badge counters in the UI. */
    long getUnreadCount(UUID userId);

    /**
     * Marks a single notification as read.
     * Throws {@link com.quickbite.quickbite.common.exception.ResourceNotFoundException}
     * if the notification does not exist or does not belong to {@code userId}.
     */
    void markAsRead(UUID notificationId, UUID userId);

    /** Marks all of {@code userId}'s notifications as read in a single bulk update. */
    void markAllAsRead(UUID userId);
}
