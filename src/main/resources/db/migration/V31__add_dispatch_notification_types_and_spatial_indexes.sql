-- V31: Add dispatch notification types and ensure spatial GiST index on delivery agents

ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'NO_AGENT_FOUND';
ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'RESTAURANT_TIMEOUT';
ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'DELIVERY_OFFER_RECEIVED';
ALTER TYPE order_notification_type ADD VALUE IF NOT EXISTS 'AGENT_ASSIGNED';

CREATE INDEX IF NOT EXISTS idx_delivery_agents_location_gist
    ON delivery_agents USING GIST (last_location);
