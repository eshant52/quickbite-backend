-- Add REFUND_FAILED to the payment_status enum.
-- Used when a gateway refund attempt fails. The failure reason is recorded in
-- payment_status_history so the support team can investigate manually.
ALTER TYPE payment_status ADD VALUE IF NOT EXISTS 'REFUND_FAILED';
