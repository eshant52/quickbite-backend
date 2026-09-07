-- V23: Add SUSPENDED value to delivery_agent_verification_status enum
ALTER TYPE delivery_agent_verification_status ADD VALUE IF NOT EXISTS 'SUSPENDED';
