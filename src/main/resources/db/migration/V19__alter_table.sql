-- V19: Align verification history references, make vehicle application draft fields nullable,
-- enforce vehicle vin_number not-null constraint, and restore composite primary keys on Envers audit tables.

-- 1. Delivery agent verification history: drop old FK & column, link to delivery_agent_applications
ALTER TABLE delivery_agent_verification_history
    DROP CONSTRAINT IF EXISTS fk_delivery_agent_verification_history_on_delivery_agent;

ALTER TABLE delivery_agent_verification_history
    ADD COLUMN IF NOT EXISTS application_id UUID;

ALTER TABLE delivery_agent_verification_history
    ALTER COLUMN application_id SET NOT NULL;

ALTER TABLE delivery_agent_verification_history
    ADD CONSTRAINT fk_delivery_agent_verification_history_on_application
        FOREIGN KEY (application_id) REFERENCES delivery_agent_applications (id);

ALTER TABLE delivery_agent_verification_history
    DROP COLUMN IF EXISTS delivery_agent_id;

-- 2. Restaurant verification status history: drop old FK & column, link to restaurant_applications
ALTER TABLE restaurant_verification_status_history
    DROP CONSTRAINT IF EXISTS fk_restaurant_verification_status_history_on_restaurant;

ALTER TABLE restaurant_verification_status_history
    ADD COLUMN IF NOT EXISTS application_id UUID;

ALTER TABLE restaurant_verification_status_history
    ALTER COLUMN application_id SET NOT NULL;

ALTER TABLE restaurant_verification_status_history
    ADD CONSTRAINT fk_restaurant_verification_status_history_on_application
        FOREIGN KEY (application_id) REFERENCES restaurant_applications (id);

ALTER TABLE restaurant_verification_status_history
    DROP COLUMN IF EXISTS restaurant_id;

-- 3. Delivery agent applications: unique constraint on promoted delivery_agent_id (@OneToOne)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uc_delivery_agent_applications_delivery_agent'
    ) THEN
        ALTER TABLE delivery_agent_applications
            ADD CONSTRAINT uc_delivery_agent_applications_delivery_agent UNIQUE (delivery_agent_id);
    END IF;
END $$;

-- 4. Vehicle applications: make draft step fields nullable
ALTER TABLE vehicle_applications
    ALTER COLUMN brand DROP NOT NULL,
    ALTER COLUMN model DROP NOT NULL,
    ALTER COLUMN number_plate DROP NOT NULL,
    ALTER COLUMN vehicle_type DROP NOT NULL,
    ALTER COLUMN vin_number DROP NOT NULL;

-- 5. Vehicles: ensure vin_number is NOT NULL
ALTER TABLE vehicles
    ALTER COLUMN vin_number SET NOT NULL;

-- 6. Envers audit tables: drop single-column PK on rev and duplicate index if present, add composite PRIMARY KEY (rev, id)
DROP INDEX IF EXISTS ix_pk_delivery_agents_aud;
ALTER TABLE delivery_agents_aud
    DROP CONSTRAINT IF EXISTS delivery_agents_aud_pkey,
    DROP CONSTRAINT IF EXISTS pk_delivery_agents_aud;
ALTER TABLE delivery_agents_aud
    ADD CONSTRAINT pk_delivery_agents_aud PRIMARY KEY (rev, id);

DROP INDEX IF EXISTS ix_pk_menu_items_aud;
ALTER TABLE menu_items_aud
    DROP CONSTRAINT IF EXISTS menu_items_aud_pkey,
    DROP CONSTRAINT IF EXISTS pk_menu_items_aud;
ALTER TABLE menu_items_aud
    ADD CONSTRAINT pk_menu_items_aud PRIMARY KEY (rev, id);

DROP INDEX IF EXISTS ix_pk_orders_aud;
ALTER TABLE orders_aud
    DROP CONSTRAINT IF EXISTS orders_aud_pkey,
    DROP CONSTRAINT IF EXISTS pk_orders_aud;
ALTER TABLE orders_aud
    ADD CONSTRAINT pk_orders_aud PRIMARY KEY (rev, id);

DROP INDEX IF EXISTS ix_pk_payments_aud;
ALTER TABLE payments_aud
    DROP CONSTRAINT IF EXISTS payments_aud_pkey,
    DROP CONSTRAINT IF EXISTS pk_payments_aud;
ALTER TABLE payments_aud
    ADD CONSTRAINT pk_payments_aud PRIMARY KEY (rev, id);

DROP INDEX IF EXISTS ix_pk_restaurants_aud;
ALTER TABLE restaurants_aud
    DROP CONSTRAINT IF EXISTS restaurants_aud_pkey,
    DROP CONSTRAINT IF EXISTS pk_restaurants_aud;
ALTER TABLE restaurants_aud
    ADD CONSTRAINT pk_restaurants_aud PRIMARY KEY (rev, id);

DROP INDEX IF EXISTS ix_pk_users_aud;
ALTER TABLE users_aud
    DROP CONSTRAINT IF EXISTS users_aud_pkey,
    DROP CONSTRAINT IF EXISTS pk_users_aud;
ALTER TABLE users_aud
    ADD CONSTRAINT pk_users_aud PRIMARY KEY (rev, id);