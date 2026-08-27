-- V13: Separate cuisine_requests from master cuisines catalog and decouple cuisine notifications

-- 1. Create cuisine_requests workflow table
CREATE TABLE cuisine_requests
(
    id              UUID                        NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name            VARCHAR(100)                NOT NULL,
    requested_by_id UUID                        NOT NULL,
    status          CUISINE_STATUS              NOT NULL DEFAULT 'PENDING',
    reviewed_by_id  UUID,
    reviewed_at     TIMESTAMP WITHOUT TIME ZONE,
    remarks         TEXT,
    cuisine_id      UUID,
    CONSTRAINT pk_cuisine_requests PRIMARY KEY (id),
    CONSTRAINT fk_cuisine_requests_requested_by FOREIGN KEY (requested_by_id) REFERENCES users (id),
    CONSTRAINT fk_cuisine_requests_reviewed_by FOREIGN KEY (reviewed_by_id) REFERENCES users (id),
    CONSTRAINT fk_cuisine_requests_cuisine FOREIGN KEY (cuisine_id) REFERENCES cuisines (id)
);

CREATE INDEX idx_cuisine_requests_status ON cuisine_requests (status);
CREATE INDEX idx_cuisine_requests_requested_by ON cuisine_requests (requested_by_id);
CREATE INDEX idx_cuisine_requests_name ON cuisine_requests (name);

-- 2. Strip workflow columns from cuisines table so it remains a pure master catalog
ALTER TABLE cuisines
    DROP COLUMN IF EXISTS status,
    DROP COLUMN IF EXISTS reviewed_by_id,
    DROP COLUMN IF EXISTS reviewed_at,
    DROP COLUMN IF EXISTS remarks;

-- 3. Decouple cuisine_notifications to store request id and snapshot cuisine name directly
ALTER TABLE cuisine_notifications
    DROP CONSTRAINT IF EXISTS fk_cuisine_notifications_cuisine,
    DROP COLUMN IF EXISTS cuisine_id,
    ADD COLUMN cuisine_request_id UUID NOT NULL,
    ADD COLUMN cuisine_name VARCHAR(100) NOT NULL;
