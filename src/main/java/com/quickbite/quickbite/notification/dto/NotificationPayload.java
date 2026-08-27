package com.quickbite.quickbite.notification.dto;

import com.quickbite.quickbite.notification.model.Notification;
import com.quickbite.quickbite.user.model.User;

/**
 * Sealed interface representing everything a notification delivery strategy needs.
 *
 * <h3>Why sealed?</h3>
 * Each notification type carries different domain-specific data.
 * Making this a sealed interface forces every implementation to provide:
 * <ul>
 *   <li>The three common fields every channel needs ({@code recipient}, {@code title}, {@code message})</li>
 *   <li>A {@link #toNotification()} factory method so {@code InAppNotificationStrategy}
 *       never needs to know about domain specifics — it just calls {@code save(payload.toNotification())}.</li>
 * </ul>
 *
 * <h3>Adding a new notification type</h3>
 * <ol>
 *   <li>Create a new record implementing this interface, e.g. {@code OrderNotificationPayload}.</li>
 *   <li>Add the new type to the {@code permits} clause below.</li>
 *   <li>No changes required in {@code InAppNotificationStrategy}, {@code EmailNotificationStrategy},
 *       or any other existing strategy — they all use only the interface methods.</li>
 * </ol>
 *
 * <h3>Current permitted implementations</h3>
 * <ul>
 *   <li>{@link RestaurantApplicationNotificationPayload}</li>
 *   <li>{@link OrderNotificationPayload}</li>
 *   <li>{@link PaymentNotificationPayload}</li>
 * </ul>
 */
public sealed interface NotificationPayload permits RestaurantApplicationNotificationPayload,
        OrderNotificationPayload, PaymentNotificationPayload, CuisineNotificationPayload {

    /**
     * The user who should receive this notification.
     */
    User recipient();

    /**
     * Short subject line — max 200 chars (matches the DB column length).
     */
    String title();

    /**
     * Full body text — max 1000 chars.
     */
    String message();

    /**
     * Factory method: constructs the correct {@link Notification} subtype populated
     * with all domain-specific fields.
     * <p>
     * Called exclusively by {@code InAppNotificationStrategy} to persist the notification.
     * Email and push strategies only use {@link #recipient()}, {@link #title()}, and
     * {@link #message()} and never call this method.
     */
    Notification toNotification();
}
