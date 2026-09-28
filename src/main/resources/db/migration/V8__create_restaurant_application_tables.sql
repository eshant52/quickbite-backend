-- V8: Restaurant application tables for multi-step onboarding
-- All application fields are nullable to support incremental draft saves.
-- The Restaurant row is created only on admin approval (promotion step).

CREATE TYPE application_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'UNDER_REVIEW',
    'APPROVED',
    'REJECTED'
);

CREATE TABLE restaurant_applications (
    id                    UUID               PRIMARY KEY,
    owner_id              UUID               NOT NULL REFERENCES users(id),

    -- Step 1: Basic details (nullable while in draft)
    name                  VARCHAR(200),
    description           TEXT,

    -- Step 2: Address fields embedded.
    -- A proper Address row is created only when the application is APPROVED.
    address_street        VARCHAR(150),
    address_city          VARCHAR(50),
    address_state         VARCHAR(50),
    address_country       VARCHAR(50),
    address_postal_code   VARCHAR(10),
    address_house_number  VARCHAR(20),
    address_building_name VARCHAR(100),
    address_landmark      VARCHAR(100),
    address_location      GEOMETRY(POINT, 4326),

    -- Step completion flags used for frontend progress bar and submit validation
    details_complete      BOOLEAN            NOT NULL DEFAULT FALSE,
    address_complete      BOOLEAN            NOT NULL DEFAULT FALSE,
    hours_complete        BOOLEAN            NOT NULL DEFAULT FALSE,
    images_complete       BOOLEAN            NOT NULL DEFAULT FALSE,
    documents_complete    BOOLEAN            NOT NULL DEFAULT FALSE,

    -- Lifecycle
    status                application_status NOT NULL DEFAULT 'DRAFT',
    reviewed_by_id        UUID               REFERENCES users(id),
    reviewed_at           TIMESTAMPTZ,
    rejection_remarks     TEXT,

    -- Populated on APPROVED: points to the promoted Restaurant row
    restaurant_id         UUID               REFERENCES restaurants(id),

    created_at            TIMESTAMPTZ        NOT NULL,
    updated_at            TIMESTAMPTZ        NOT NULL
);

-- Step 3: Operating hours for the draft application
CREATE TABLE application_hours (
    id             UUID        PRIMARY KEY,
    application_id UUID        NOT NULL REFERENCES restaurant_applications(id) ON DELETE CASCADE,
    day_of_week    day_of_week NOT NULL,
    open_time      TIME        NOT NULL,
    close_time     TIME        NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    UNIQUE (application_id, day_of_week)
);

-- Step 4: Images for the draft application
CREATE TABLE application_images (
    id             UUID        PRIMARY KEY,
    application_id UUID        NOT NULL REFERENCES restaurant_applications(id) ON DELETE CASCADE,
    image_url      TEXT        NOT NULL,
    display_order  INT         NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

-- Step 5: Documents for the draft application.
-- One entry per document type per application (enforced by unique constraint).
CREATE TABLE application_documents (
    id             UUID                     PRIMARY KEY,
    application_id UUID                     NOT NULL REFERENCES restaurant_applications(id) ON DELETE CASCADE,
    type           restaurant_document_type NOT NULL,
    url            TEXT                     NOT NULL,
    created_at     TIMESTAMPTZ              NOT NULL,
    updated_at     TIMESTAMPTZ              NOT NULL,
    UNIQUE (application_id, type)
);
