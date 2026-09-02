-- V18: Delivery agent onboarding and standalone vehicle application tables

-- 1. Add vin_number to vehicles table
ALTER TABLE vehicles
    ADD COLUMN IF NOT EXISTS vin_number VARCHAR(30);

-- Make vin_number unique if not already
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uc_vehicles_vin_number'
    ) THEN
        ALTER TABLE vehicles ADD CONSTRAINT uc_vehicles_vin_number UNIQUE (vin_number);
    END IF;
END $$;

-- 2. Delivery agent applications table
CREATE TABLE IF NOT EXISTS delivery_agent_applications
(
    id                 UUID                        NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    agent_id           UUID                        NOT NULL,
    documents_complete BOOLEAN                     NOT NULL DEFAULT false,
    vehicle_complete   BOOLEAN                     NOT NULL DEFAULT false,
    status             application_status          NOT NULL DEFAULT 'DRAFT',
    reviewed_by_id     UUID,
    reviewed_at        TIMESTAMP WITHOUT TIME ZONE,
    rejection_remarks  TEXT,
    delivery_agent_id  UUID,
    CONSTRAINT pk_delivery_agent_applications PRIMARY KEY (id),
    CONSTRAINT fk_delivery_agent_app_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT fk_delivery_agent_app_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT fk_delivery_agent_app_promoted_agent FOREIGN KEY (delivery_agent_id) REFERENCES delivery_agents (id)
);

-- 3. Delivery agent application documents table
CREATE TABLE IF NOT EXISTS delivery_agent_application_documents
(
    id             UUID                         NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    application_id UUID                         NOT NULL,
    type           delivery_agent_document_type NOT NULL,
    url            TEXT                         NOT NULL,
    CONSTRAINT pk_delivery_agent_app_documents PRIMARY KEY (id),
    CONSTRAINT fk_agent_app_documents_application FOREIGN KEY (application_id) REFERENCES delivery_agent_applications (id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_app_doc_type UNIQUE (application_id, type)
);

-- 4. Vehicle applications table (supports both initial onboarding and standalone vehicle addition)
CREATE TABLE IF NOT EXISTS vehicle_applications
(
    id                       UUID                        NOT NULL,
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    application_id           UUID,
    delivery_agent_id        UUID,
    status                   application_status          NOT NULL DEFAULT 'DRAFT',
    reviewed_by_id           UUID,
    reviewed_at              TIMESTAMP WITHOUT TIME ZONE,
    rejection_remarks        TEXT,
    existing_vehicle_id      UUID,
    is_ownership_transferred BOOLEAN                     NOT NULL DEFAULT false,
    vin_number               VARCHAR(30)                 NOT NULL,
    number_plate             VARCHAR(20)                 NOT NULL,
    vehicle_type             vehicle_type                NOT NULL,
    brand                    VARCHAR(50)                 NOT NULL,
    model                    VARCHAR(50)                 NOT NULL,
    CONSTRAINT pk_vehicle_applications PRIMARY KEY (id),
    CONSTRAINT fk_vehicle_apps_application FOREIGN KEY (application_id) REFERENCES delivery_agent_applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_apps_delivery_agent FOREIGN KEY (delivery_agent_id) REFERENCES delivery_agents (id),
    CONSTRAINT fk_vehicle_apps_reviewer FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT fk_vehicle_apps_existing_vehicle FOREIGN KEY (existing_vehicle_id) REFERENCES vehicles (id)
);

-- 5. Vehicle application documents table
CREATE TABLE IF NOT EXISTS vehicle_application_documents
(
    id                     UUID                            NOT NULL,
    created_at             TIMESTAMP WITHOUT TIME ZONE     NOT NULL,
    updated_at             TIMESTAMP WITHOUT TIME ZONE     NOT NULL,
    application_vehicle_id UUID                            NOT NULL,
    type                   vehicle_ownership_document_type NOT NULL,
    url                    TEXT                            NOT NULL,
    CONSTRAINT pk_vehicle_application_documents PRIMARY KEY (id),
    CONSTRAINT fk_vehicle_app_docs_vehicle FOREIGN KEY (application_vehicle_id) REFERENCES vehicle_applications (id) ON DELETE CASCADE,
    CONSTRAINT uq_vehicle_app_doc_type UNIQUE (application_vehicle_id, type)
);

-- 6. Performance indexes
CREATE INDEX IF NOT EXISTS idx_delivery_agent_apps_agent_id ON delivery_agent_applications(agent_id);
CREATE INDEX IF NOT EXISTS idx_delivery_agent_apps_status ON delivery_agent_applications(status);
CREATE INDEX IF NOT EXISTS idx_vehicle_apps_app_id ON vehicle_applications(application_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_apps_agent_id ON vehicle_applications(delivery_agent_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_apps_status ON vehicle_applications(status);
CREATE INDEX IF NOT EXISTS idx_vehicle_apps_vin ON vehicle_applications(vin_number);

-- 7. Delivery agent application notifications
CREATE TYPE delivery_agent_application_notification_type AS ENUM (
    'APPLICATION_SUBMITTED',
    'APPLICATION_APPROVED',
    'APPLICATION_REJECTED'
);

CREATE TABLE IF NOT EXISTS delivery_agent_application_notifications
(
    id             UUID                                         PRIMARY KEY,
    application_id UUID                                         NOT NULL,
    type           delivery_agent_application_notification_type NOT NULL,
    agent_name     VARCHAR(150),
    CONSTRAINT fk_deliv_agent_app_notif_on_id FOREIGN KEY (id) REFERENCES notifications (id)
);

-- 8. Vehicle application notifications
CREATE TYPE vehicle_application_notification_type AS ENUM (
    'APPLICATION_SUBMITTED',
    'APPLICATION_APPROVED',
    'APPLICATION_REJECTED'
);

CREATE TABLE IF NOT EXISTS vehicle_application_notifications
(
    id             UUID                                  PRIMARY KEY,
    application_id UUID                                  NOT NULL,
    type           vehicle_application_notification_type NOT NULL,
    vehicle_name   VARCHAR(100),
    CONSTRAINT fk_vehicle_app_notif_on_id FOREIGN KEY (id) REFERENCES notifications (id)
);
