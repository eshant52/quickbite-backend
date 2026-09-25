-- V32: Drop leftover unique constraint on vehicles.number_plate from V3
-- and enforce at most one ACTIVE ownership per physical vehicle.

ALTER TABLE vehicles
    DROP CONSTRAINT IF EXISTS uc_vehicles_numberplate;

CREATE INDEX IF NOT EXISTS idx_vehicles_number_plate ON vehicles (number_plate);

CREATE UNIQUE INDEX IF NOT EXISTS idx_vehicle_ownerships_one_active_per_vehicle
    ON vehicle_ownerships (vehicle_id)
    WHERE current_status = 'ACTIVE';
