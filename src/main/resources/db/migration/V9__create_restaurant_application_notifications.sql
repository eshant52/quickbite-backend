-- V9: Restaurant application notification table.
--
-- Specifically named "restaurant_application_notifications" (not the generic
-- "application_notifications") to be consistent with OrderNotification and
-- PaymentNotification, and to leave room for future application types:
--   - vehicle_registration_notifications
--   - delivery_agent_onboarding_notifications
--
-- applicationId is stored without a FK constraint by design:
-- notifications are historical records and must survive even if the source
-- RestaurantApplication row is deleted or archived.

CREATE TYPE restaurant_application_notification_type AS ENUM (
    'APPLICATION_SUBMITTED',
    'APPLICATION_APPROVED',
    'APPLICATION_REJECTED'
);

CREATE TABLE restaurant_application_notifications (
    id               UUID                                    PRIMARY KEY,
    application_id   UUID                                    NOT NULL,
    type             restaurant_application_notification_type NOT NULL,
    restaurant_name  VARCHAR(200),
    CONSTRAINT fk_restaurant_application_notifications_on_id
        FOREIGN KEY (id) REFERENCES notifications (id)
);
