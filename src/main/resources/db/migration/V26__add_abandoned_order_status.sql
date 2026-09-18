-- Add ABANDONED to the order_status and order_notification_type PostgreSQL enums.
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'ABANDONED';
ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'ABANDONED';

-- Partial index on orders: only indexes orders in AWAITING_PAYMENT status.
-- Enables O(log N) lookup of stale orders for the background abandonment scheduler
-- with zero overhead on fulfilled/terminal orders.
CREATE INDEX IF NOT EXISTS idx_orders_awaiting_payment_created
    ON orders (created_at)
    WHERE current_status = 'AWAITING_PAYMENT';
