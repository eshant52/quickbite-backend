-- Add AWAITING_PAYMENT and PAYMENT_FAILED to the order_status PostgreSQL enum.
-- These are required for the two-phase order+payment flow:
--   AWAITING_PAYMENT → order created but online gateway not yet confirmed
--   PAYMENT_FAILED   → gateway reported failure / timeout
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'AWAITING_PAYMENT';
ALTER TYPE order_status ADD VALUE IF NOT EXISTS 'PAYMENT_FAILED';

-- Also add AWAITING_PAYMENT to order_notification_type so notification
-- listeners can reference it when publishing payment-pending notifications.
ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'AWAITING_PAYMENT';
ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'PAYMENT_FAILED';
