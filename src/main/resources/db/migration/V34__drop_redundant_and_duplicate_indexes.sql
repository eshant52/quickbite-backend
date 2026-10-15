-- V34: Drop redundant, duplicate, and bypassed indexes to reduce write overhead and storage

-- 1. Drop duplicate GiST spatial index on delivery_agents(last_location)
-- V31 introduced idx_delivery_agents_location_gist which is a 100% duplicate of
-- idx_delivery_agents_last_location introduced in V20. Keeping idx_delivery_agents_last_location.
DROP INDEX IF EXISTS idx_delivery_agents_location_gist;

-- 2. Drop redundant B-tree index on delivery_agents(user_id)
-- uc_delivery_agents_user UNIQUE (user_id) from V3 already provides an implicit unique
-- B-tree index on (user_id). The explicit index created in V15 is entirely redundant.
DROP INDEX IF EXISTS idx_delivery_agents_user_id;

-- 3. Drop bypassed geometry GiST index on addresses(location)
-- All spatial queries in the application (RestaurantRepository) cast location to ::geography.
-- The query planner bypasses geometry GiST in favor of idx_addresses_location_geog (introduced in V33).
DROP INDEX IF EXISTS idx_addresses_location;

-- 4. Drop superseded delivery agent availability index
-- idx_delivery_agents_available_status (is_available, current_status) from V15 is superseded by
-- idx_delivery_agents_dispatch_ready from V22 (which adds is_assigned) and
-- idx_delivery_agents_dispatch_available from V33 (spatial GiST candidate index).
DROP INDEX IF EXISTS idx_delivery_agents_available_status;
