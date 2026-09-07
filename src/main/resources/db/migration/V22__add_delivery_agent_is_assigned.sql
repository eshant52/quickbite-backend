-- V22: Add is_assigned column to delivery_agents and audit table, with partial index for dispatch queries

ALTER TABLE delivery_agents
    ADD COLUMN is_assigned BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE delivery_agents_aud
    ADD COLUMN is_assigned BOOLEAN;

-- Partial index to accelerate dispatch queries filtering for active, on-duty, unassigned agents
CREATE INDEX IF NOT EXISTS idx_delivery_agents_dispatch_ready
    ON delivery_agents (is_available, is_assigned, current_status)
    WHERE is_available = TRUE AND is_assigned = FALSE AND current_status = 'APPROVED';
