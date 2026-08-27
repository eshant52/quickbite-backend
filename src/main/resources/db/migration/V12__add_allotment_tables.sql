-- V12: Admin allotment system & cuisine notification tables

-- 1. Cuisine Notification Type & Table (if missing)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'cuisine_notification_type') THEN
        CREATE TYPE cuisine_notification_type AS ENUM (
            'PENDING',
            'APPROVED',
            'REJECTED'
        );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS cuisine_notifications (
    id          UUID                      PRIMARY KEY,
    cuisine_id  UUID                      NOT NULL,
    type        cuisine_notification_type NOT NULL,
    CONSTRAINT fk_cuisine_notifications_on_id
        FOREIGN KEY (id) REFERENCES notifications (id),
    CONSTRAINT fk_cuisine_notifications_cuisine
        FOREIGN KEY (cuisine_id) REFERENCES cuisines (id)
);

-- 2. Admin Allotment Types
CREATE TYPE allotment_reference_type AS ENUM (
    'RESTAURANT_APPLICATION',
    'CUISINE',
    'VEHICLE_APPLICATION'
);

CREATE TYPE allotment_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'DECLINED'
);

-- 3. Admin Allotments Table
CREATE TABLE admin_allotments
(
    id             UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    admin_id       UUID                        NOT NULL,
    reference_id   UUID                        NOT NULL,
    reference_type allotment_reference_type    NOT NULL,
    status         allotment_status            NOT NULL,
    notified_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    responded_at   TIMESTAMP WITHOUT TIME ZONE,
    version        BIGINT                      NOT NULL DEFAULT 0,
    CONSTRAINT pk_admin_allotments PRIMARY KEY (id),
    CONSTRAINT uq_allotment_admin_reference UNIQUE (admin_id, reference_id),
    CONSTRAINT fk_allotment_admin FOREIGN KEY (admin_id) REFERENCES users (id)
);

-- 4. Partial unique index: only ONE admin can have status = 'ACCEPTED' per referenceId
CREATE UNIQUE INDEX uq_allotment_one_accepted_per_reference
    ON admin_allotments (reference_id)
    WHERE status = 'ACCEPTED';

-- 5. Query optimization indexes
CREATE INDEX idx_allotment_reference_status
    ON admin_allotments (reference_id, status);

CREATE INDEX idx_allotment_admin_status
    ON admin_allotments (admin_id, status);
