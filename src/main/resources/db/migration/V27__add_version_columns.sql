-- Optimistic locking version columns for Order and Payment entities.
-- Required by @Version annotation on Order.java and Payment.java.
ALTER TABLE orders   ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Propagate to Envers audit tables as well
ALTER TABLE orders_aud   ADD COLUMN IF NOT EXISTS version BIGINT;
ALTER TABLE payments_aud ADD COLUMN IF NOT EXISTS version BIGINT;
