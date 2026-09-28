-- V20: Add routing fields to orders and delivery_agents
-- Also ensures spatial GIST indexes exist on location columns.

-- ── orders: store route data resolved at placement time ──────────────────────
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS delivery_distance_meters DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS estimated_delivery_seconds BIGINT;

-- ── delivery_agents: fair assignment tiebreaker ───────────────────────────────
ALTER TABLE delivery_agents
    ADD COLUMN IF NOT EXISTS last_assigned_at TIMESTAMPTZ;

-- ── GIST spatial indexes (safe to run even if they already exist) ─────────────
CREATE INDEX IF NOT EXISTS idx_addresses_location
    ON addresses USING GIST (location);

CREATE INDEX IF NOT EXISTS idx_delivery_agents_last_location
    ON delivery_agents USING GIST (last_location);
