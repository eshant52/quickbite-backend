-- V33: Add missing database performance, spatial geography, and pagination indexes

-- 1. PostGIS functional geography indexes
-- Resolves query planner bypassing geometry GiST when casting columns to ::geography in queries.
CREATE INDEX IF NOT EXISTS idx_addresses_location_geog
    ON addresses USING GIST (CAST(location AS geography));

CREATE INDEX IF NOT EXISTS idx_delivery_agents_location_geog
    ON delivery_agents USING GIST (CAST(last_location AS geography));

-- Partial index for active dispatch candidate search (indexes only online, unassigned, approved drivers)
CREATE INDEX IF NOT EXISTS idx_delivery_agents_dispatch_available
    ON delivery_agents USING GIST (CAST(last_location AS geography))
    WHERE is_available = true
      AND is_assigned = false
      AND current_status = 'APPROVED'
      AND current_vehicle_id IS NOT NULL;

-- 2. Orders aggregate indexes
-- Fast customer order history cursor pagination: WHERE customer_id = ? AND id < ? ORDER BY id DESC
CREATE INDEX IF NOT EXISTS idx_orders_customer_id
    ON orders (customer_id, id DESC);

-- Fast restaurant live kitchen incoming orders: WHERE restaurant_id = ? AND current_status = ? ORDER BY id DESC
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_status
    ON orders (restaurant_id, current_status, id DESC);

-- Fast restaurant order history cursor pagination: WHERE restaurant_id = ? AND id < ? ORDER BY id DESC
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_id
    ON orders (restaurant_id, id DESC);

-- Foreign key index for assigned delivery agent
CREATE INDEX IF NOT EXISTS idx_orders_delivery_agent
    ON orders (delivery_agent_id);

-- Foreign key index for order items (eliminates table scan on order.getItems())
CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items (order_id);

-- Foreign key index for order status history timeline
CREATE INDEX IF NOT EXISTS idx_order_status_history_order_created
    ON order_status_history (order_id, created_at ASC);

-- 3. Payments aggregate indexes
-- Foreign key & timeline lookup for payments by order
CREATE INDEX IF NOT EXISTS idx_payments_order_id_created
    ON payments (order_id, created_at ASC);

-- Foreign key index for payment status history
CREATE INDEX IF NOT EXISTS idx_payment_status_history_payment_id
    ON payment_status_history (payment_id);

-- 4. Menu Items indexes
-- Fast customer menu browsing: WHERE restaurant_id = ? AND is_available = true ORDER BY id ASC
CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant_available
    ON menu_items (restaurant_id, is_available, id ASC);

-- Fast all-items cursor pagination for restaurant owner
CREATE INDEX IF NOT EXISTS idx_menu_items_restaurant_id
    ON menu_items (restaurant_id, id ASC);

-- 5. Addresses index
-- Fast address book lookup for customer: WHERE user_id = ?
CREATE INDEX IF NOT EXISTS idx_addresses_user_id
    ON addresses (user_id);

-- 6. Notifications indexes
-- Fast unread notification inbox cursor pagination & badge counts (newest first)
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_unread_desc
    ON notifications (recipient_id, is_read, id DESC);

-- Fast all notification inbox cursor pagination (newest first)
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_desc
    ON notifications (recipient_id, id DESC);

-- 7. Sessions index
-- Fast active session count and revocation checks: WHERE user_id = ? AND revoked_at IS NULL AND expires_at > CURRENT_TIMESTAMP
CREATE INDEX IF NOT EXISTS idx_sessions_user_active
    ON sessions (user_id, revoked_at, expires_at);

-- 8. Reviews index
-- Covering index for restaurant rating aggregates (AVG, COUNT, GROUP BY rating)
CREATE INDEX IF NOT EXISTS idx_reviews_restaurant_rating
    ON reviews (restaurant_id, rating);

-- 9. Users index
-- Functional index for case-insensitive email lookup: WHERE LOWER(email) = LOWER(?)
CREATE INDEX IF NOT EXISTS idx_users_lower_email
    ON users (LOWER(email));
