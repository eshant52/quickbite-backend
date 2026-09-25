-- V32: Drop leftover unique constraint on vehicles.number_plate from V3.
-- Physical vehicles are uniquely identified by vin_number (uc_vehicles_vin_number in V18),
-- while number_plate can repeat across historical vehicles when a registration plate is transferred.

ALTER TABLE vehicles
    DROP CONSTRAINT IF EXISTS uc_vehicles_numberplate;

CREATE INDEX IF NOT EXISTS idx_vehicles_number_plate ON vehicles (number_plate);
