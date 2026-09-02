-- V15: Add DELIVERY_AGENT to allotment reference types and performance indexes for delivery

-- 1. Add DELIVERY_AGENT to allotment_reference_type enum
ALTER TYPE allotment_reference_type ADD VALUE IF NOT EXISTS 'DELIVERY_AGENT';

-- 2. Delivery Agents indexes
CREATE INDEX IF NOT EXISTS idx_delivery_agents_user_id ON delivery_agents(user_id);
CREATE INDEX IF NOT EXISTS idx_delivery_agents_status ON delivery_agents(current_status);
CREATE INDEX IF NOT EXISTS idx_delivery_agents_available_status
    ON delivery_agents(is_available, current_status)
    WHERE is_available = true AND current_status = 'APPROVED';

-- 3. Delivery Agent Documents indexes
CREATE INDEX IF NOT EXISTS idx_delivery_agent_documents_agent_id ON delivery_agent_documents(delivery_agent_id);
