-- V25: Add gateway tracking fields to payments table
-- Supports Razorpay (and future gateways) by storing gateway-assigned identifiers
-- on the payment record for webhook correlation and refund support.

-- Add JSONB column for storing gateway metadata
ALTER TABLE payments
    ADD gateway_meta_data JSONB;

-- Add column for storing the name of the payment gateway (e.g., "Razorpay", "Stripe", etc.)
ALTER TABLE payments
    ADD gateway_name VARCHAR(50);

-- Gateway order ID (e.g. Razorpay "order_xxx") — set at payment initiation.
-- Used to correlate webhook / client-side verification back to our Payment row.
ALTER TABLE payments
    ADD COLUMN gateway_order_id VARCHAR(255);

-- Gateway payment ID (e.g. Razorpay "pay_xxx") — set after successful verification.
ALTER TABLE payments
    ADD COLUMN gateway_payment_id VARCHAR(255);

-- Index on gateway_order_id for fast webhook and verify lookups
CREATE INDEX idx_payments_gateway_order_id ON payments (gateway_order_id);

ALTER TABLE payments_aud
    ADD gateway_meta_data JSONB;

ALTER TABLE payments_aud
    ADD gateway_name VARCHAR(50);

ALTER TABLE payments_aud
    ADD gateway_order_id VARCHAR(255);

ALTER TABLE payments_aud
    ADD gateway_payment_id VARCHAR(255);

ALTER TABLE payment_status_history
    ADD reason TEXT;