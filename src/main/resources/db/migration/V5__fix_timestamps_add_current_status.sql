-- ============================================================
-- V5: Fix timestamps (TIMESTAMP -> TIMESTAMPTZ) and add
--     current_status to orders & payments tables
-- ============================================================

-- -------- addresses --------
ALTER TABLE addresses
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- cart_items --------
ALTER TABLE cart_items
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- carts --------
ALTER TABLE carts
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at  TYPE TIMESTAMPTZ USING updated_at  AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at  TYPE TIMESTAMPTZ USING expires_at  AT TIME ZONE 'UTC';

-- -------- cuisines --------
ALTER TABLE cuisines
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN reviewed_at  TYPE TIMESTAMPTZ USING reviewed_at  AT TIME ZONE 'UTC';

-- -------- delivery_agent_documents --------
ALTER TABLE delivery_agent_documents
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN reviewed_at  TYPE TIMESTAMPTZ USING reviewed_at  AT TIME ZONE 'UTC';

-- -------- delivery_agent_verification_history --------
ALTER TABLE delivery_agent_verification_history
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- delivery_agents --------
ALTER TABLE delivery_agents
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- menu_item_images --------
ALTER TABLE menu_item_images
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- menu_items --------
ALTER TABLE menu_items
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- notifications --------
ALTER TABLE notifications
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- order_items --------
ALTER TABLE order_items
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- order_notifications (joined table, inherits notifications timestamps) --------
-- No extra timestamp columns beyond the PK FK.

-- -------- order_status_history --------
ALTER TABLE order_status_history
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- orders --------
ALTER TABLE orders
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- payment_notifications (joined table) --------
-- No extra timestamp columns.

-- -------- payment_status_history --------
ALTER TABLE payment_status_history
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- payments --------
ALTER TABLE payments
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- refresh_tokens --------
ALTER TABLE refresh_tokens
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at  TYPE TIMESTAMPTZ USING updated_at  AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at  TYPE TIMESTAMPTZ USING expires_at  AT TIME ZONE 'UTC',
    ALTER COLUMN revoked_at  TYPE TIMESTAMPTZ USING revoked_at  AT TIME ZONE 'UTC',
    ALTER COLUMN used_at     TYPE TIMESTAMPTZ USING used_at     AT TIME ZONE 'UTC';

-- -------- refresh_token_families --------
ALTER TABLE refresh_token_families
    ALTER COLUMN created_at          TYPE TIMESTAMPTZ USING created_at          AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at          TYPE TIMESTAMPTZ USING updated_at          AT TIME ZONE 'UTC',
    ALTER COLUMN revoked_at          TYPE TIMESTAMPTZ USING revoked_at          AT TIME ZONE 'UTC',
    ALTER COLUMN reuse_detected_at   TYPE TIMESTAMPTZ USING reuse_detected_at   AT TIME ZONE 'UTC';

-- -------- restaurant_documents --------
ALTER TABLE restaurant_documents
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN reviewed_at  TYPE TIMESTAMPTZ USING reviewed_at  AT TIME ZONE 'UTC';

-- -------- restaurant_hours (open_time / close_time are TIME, not timestamps — leave them) --------
ALTER TABLE restaurant_hours
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- restaurant_images --------
ALTER TABLE restaurant_images
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- restaurant_verification_status_history --------
ALTER TABLE restaurant_verification_status_history
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- restaurants --------
ALTER TABLE restaurants
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- reviews --------
ALTER TABLE reviews
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- sessions --------
ALTER TABLE sessions
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN login_at     TYPE TIMESTAMPTZ USING login_at     AT TIME ZONE 'UTC',
    ALTER COLUMN last_used_at TYPE TIMESTAMPTZ USING last_used_at AT TIME ZONE 'UTC',
    ALTER COLUMN expires_at   TYPE TIMESTAMPTZ USING expires_at   AT TIME ZONE 'UTC',
    ALTER COLUMN revoked_at   TYPE TIMESTAMPTZ USING revoked_at   AT TIME ZONE 'UTC';

-- -------- users --------
ALTER TABLE users
    ALTER COLUMN created_at    TYPE TIMESTAMPTZ USING created_at    AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at    TYPE TIMESTAMPTZ USING updated_at    AT TIME ZONE 'UTC',
    ALTER COLUMN last_login_at TYPE TIMESTAMPTZ USING last_login_at AT TIME ZONE 'UTC';

-- -------- vehicle_ownership_documents --------
ALTER TABLE vehicle_ownership_documents
    ALTER COLUMN created_at   TYPE TIMESTAMPTZ USING created_at   AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at   TYPE TIMESTAMPTZ USING updated_at   AT TIME ZONE 'UTC',
    ALTER COLUMN reviewed_at  TYPE TIMESTAMPTZ USING reviewed_at  AT TIME ZONE 'UTC';

-- -------- vehicle_ownership_status_history --------
ALTER TABLE vehicle_ownership_status_history
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- vehicle_ownerships --------
ALTER TABLE vehicle_ownerships
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- -------- vehicles --------
ALTER TABLE vehicles
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

-- ============================================================
-- Add current_status to orders and payments for fast reads
-- (avoids joining history tables just to get the latest status)
-- ============================================================

ALTER TABLE orders
    ADD COLUMN current_status order_status NOT NULL DEFAULT 'PLACED';

ALTER TABLE payments
    ADD COLUMN current_status payment_status NOT NULL DEFAULT 'PENDING';
