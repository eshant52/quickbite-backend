package com.quickbite.quickbite.notification.model;

/**
 * Notification types specific to the restaurant onboarding lifecycle.
 * <p>
 * Future application types (vehicle registration, delivery-agent onboarding, etc.)
 * should get their own enum, e.g.:
 *   - {@code VehicleRegistrationNotificationType}
 *   - {@code DeliveryAgentOnboardingNotificationType}
 */
public enum RestaurantApplicationNotificationType {
    /** Sent to all admins when an owner submits a new restaurant application. */
    APPLICATION_SUBMITTED,
    /** Sent to the restaurant owner when an admin approves their application. */
    APPLICATION_APPROVED,
    /** Sent to the restaurant owner when an admin rejects their application. */
    APPLICATION_REJECTED
}
