package com.quickbite.quickbite.notification.controller;

import com.quickbite.quickbite.auth.util.AuthenticatedSessionResolver;
import com.quickbite.quickbite.common.dto.CursorPage;
import com.quickbite.quickbite.notification.dto.NotificationResponse;
import com.quickbite.quickbite.notification.service.NotificationQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST endpoints for the in-app notification inbox.
 * <p>
 * All endpoints require a valid access token (SCOPE_API), enforced by the
 * catch-all rule in {@link com.quickbite.quickbite.common.config.SecurityConfig}.
 * There is no role restriction — any authenticated user can access their own inbox.
 *
 * <h3>Pagination</h3>
 * List endpoints use cursor-based pagination backed by UUIDv7 time-ordering:
 * <pre>
 *   First page:       GET /api/v1/notifications?size=20
 *   Subsequent pages: GET /api/v1/notifications?cursor=&lt;nextCursor&gt;&amp;size=20
 * </pre>
 * Stop fetching when the response has {@code hasMore: false} or {@code nextCursor: null}.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationQueryService notificationQueryService;
    private final AuthenticatedSessionResolver sessionResolver;

    public NotificationController(NotificationQueryService notificationQueryService,
                                   AuthenticatedSessionResolver sessionResolver) {
        this.notificationQueryService = notificationQueryService;
        this.sessionResolver = sessionResolver;
    }

    /**
     * GET /api/v1/notifications?unread=false&cursor=&lt;uuid&gt;&size=20
     * <p>
     * Returns a cursor page of the caller's notifications ordered by creation time (oldest first
     * within a page — use {@code nextCursor} to walk forward in time).
     *
     * @param unreadOnly If {@code true}, only unread notifications are returned.
     * @param cursor     Exclusive lower-bound UUIDv7 cursor from the previous response.
     *                   Omit (or pass {@code null}) for the first page.
     * @param size       Page size. Clamped to [1, 100]. Defaults to 20.
     */
    @GetMapping
    public ResponseEntity<CursorPage<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "unread", defaultValue = "false") boolean unreadOnly,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "20") int size) {

        UUID userId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(notificationQueryService.getNotifications(userId, unreadOnly, cursor, size));
    }

    /**
     * GET /api/v1/notifications/unread-count
     * <p>
     * Returns the caller's unread notification count.
     * Lightweight endpoint — no entity loading, no pagination.
     * <p>
     * Response: {@code { "count": 3 }}
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = sessionResolver.userIdFromJwt(jwt);
        return ResponseEntity.ok(Map.of("count", notificationQueryService.getUnreadCount(userId)));
    }

    /**
     * PUT /api/v1/notifications/{id}/read
     * <p>
     * Marks a single notification as read.
     * Returns 404 if the notification does not exist or belongs to a different user.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        UUID userId = sessionResolver.userIdFromJwt(jwt);
        notificationQueryService.markAsRead(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/notifications/read-all
     * <p>
     * Bulk-marks all the caller's unread notifications as read.
     * Safe to call when there are no unread notifications.
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = sessionResolver.userIdFromJwt(jwt);
        notificationQueryService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
