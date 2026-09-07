-- Keep Envers audit tables aligned with the routing fields added in V20.
ALTER TABLE orders_aud
    ADD COLUMN IF NOT EXISTS delivery_distance_meters DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS estimated_delivery_seconds BIGINT;

ALTER TABLE delivery_agents_aud
    ADD COLUMN IF NOT EXISTS last_assigned_at TIMESTAMPTZ;
