package com.quickbite.quickbite.notification.repository;

import com.quickbite.quickbite.notification.model.Notification;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    /**
     * Cursor-based fetch of notifications for a recipient.
     *
     * Fetches at most {@code limit} items where {@code id > cursor} (or all items
     * when {@code cursor} is {@code null} — first page).
     *
     * Callers should pass {@code Limit.of(requestedSize + 1)} to enable the
     * {@code hasMore} detection in {@link com.quickbite.quickbite.common.dto.CursorPage#of}.
     *
     * The {@code :unreadOnly} flag filters to unread-only when {@code true};
     * when {@code false} all notifications are returned.
     *
     * UUIDv7 IDs are time-ordered, so {@code ORDER BY n.id ASC} is also
     * chronological — newest-first display is handled on the client.
     */
    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient.id = :recipientId
              AND (:cursor IS NULL OR n.id > :cursor)
              AND (:unreadOnly = false OR n.isRead = false)
            ORDER BY n.id ASC
            """)
    List<Notification> findWithCursor(
            @Param("recipientId") UUID recipientId,
            @Param("cursor") UUID cursor,
            @Param("unreadOnly") boolean unreadOnly,
            Limit limit);

    /** Fast unread badge count — no entity loading, no pagination. */
    long countByRecipientIdAndIsReadFalse(UUID recipientId);

    /**
     * Marks a single notification as read, only if it belongs to the given recipient.
     * Returns the number of rows updated (0 = not found or wrong owner).
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id AND n.recipient.id = :recipientId")
    int markAsRead(@Param("id") UUID id, @Param("recipientId") UUID recipientId);

    /** Bulk-marks all unread notifications for a recipient as read. */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    void markAllAsRead(@Param("recipientId") UUID recipientId);
}
