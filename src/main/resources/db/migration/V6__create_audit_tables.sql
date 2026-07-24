-- ============================================================
-- V6: Hibernate Envers audit tables for @Audited entities:
--     orders, payments, restaurants, delivery_agents, users, menu_items
--
-- Envers audit table rules:
--   - PK is (id, rev) — composite
--   - rev  → FK to revinfo(rev)
--   - revtype: 0=INSERT, 1=UPDATE, 2=DELETE
--   - All entity columns are nullable (snapshot may be partial on DELETE)
--   - @OneToMany collections are NOT stored here (owned by the other side)
--   - @ManyToOne / @OneToOne are stored as the FK column value only (no FK constraint)
-- ============================================================

-- ── orders_aud ───────────────────────────────────────────────────────────────
CREATE TABLE orders_aud
(
    -- revision metadata
    id                UUID,
    rev               BIGINT   NOT NULL,
    revtype           SMALLINT,

    -- Base fields
    created_at        TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ,

    -- Order-specific fields
    restaurant_id     UUID,
    customer_id       UUID,
    delivery_agent_id UUID,
    delivery_address  TEXT,
    delivery_location GEOMETRY(POINT, 4326),
    subtotal          DECIMAL(10, 2),
    discount_amount   DECIMAL(10, 2),
    delivery_fee      DECIMAL(10, 2),
    platform_fee      DECIMAL(10, 2),
    tax_amount        DECIMAL(10, 2),
    tip_amount        DECIMAL(10, 2),
    total_amount      DECIMAL(10, 2),
    current_status    order_status,

    CONSTRAINT pk_orders_aud PRIMARY KEY (id, rev)
);
ALTER TABLE orders_aud
    ADD CONSTRAINT fk_orders_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);

-- ── payments_aud ─────────────────────────────────────────────────────────────
CREATE TABLE payments_aud
(
    -- revision metadata
    id             UUID,
    rev            BIGINT   NOT NULL,
    revtype        SMALLINT,

    -- Base fields
    created_at     TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ,

    -- Payment-specific fields
    order_id       UUID,
    transaction_id VARCHAR(255),
    payment_method payment_method,
    amount         DECIMAL(10, 2),
    current_status payment_status,

    CONSTRAINT pk_payments_aud PRIMARY KEY (id, rev)
);
ALTER TABLE payments_aud
    ADD CONSTRAINT fk_payments_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);

-- ── restaurants_aud ──────────────────────────────────────────────────────────
CREATE TABLE restaurants_aud
(
    -- revision metadata
    id             UUID,
    rev            BIGINT   NOT NULL,
    revtype        SMALLINT,

    -- Base fields
    created_at     TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ,

    -- Restaurant-specific fields
    owner_id       UUID,
    name           VARCHAR(200),
    description    TEXT,
    address_id     UUID,
    avg_rating     DECIMAL(3, 2),
    total_rating   BIGINT,
    is_closed      BOOLEAN,
    current_status restaurant_verification_status,

    CONSTRAINT pk_restaurants_aud PRIMARY KEY (id, rev)
);
ALTER TABLE restaurants_aud
    ADD CONSTRAINT fk_restaurants_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);

-- ── delivery_agents_aud ──────────────────────────────────────────────────────
CREATE TABLE delivery_agents_aud
(
    -- revision metadata
    id                 UUID,
    rev                BIGINT   NOT NULL,
    revtype            SMALLINT,

    -- Base fields
    created_at         TIMESTAMPTZ,
    updated_at         TIMESTAMPTZ,

    -- DeliveryAgent-specific fields
    user_id            UUID,
    current_vehicle_id UUID,
    is_available       BOOLEAN,
    last_location      GEOMETRY(POINT, 4326),
    current_status     delivery_agent_verification_status,

    CONSTRAINT pk_delivery_agents_aud PRIMARY KEY (id, rev)
);
ALTER TABLE delivery_agents_aud
    ADD CONSTRAINT fk_delivery_agents_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);

-- ── users_aud ────────────────────────────────────────────────────────────────
CREATE TABLE users_aud
(
    -- revision metadata
    id            UUID,
    rev           BIGINT   NOT NULL,
    revtype       SMALLINT,

    -- Base fields
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,

    -- User-specific fields
    name          VARCHAR(100),
    email         VARCHAR(255),
    phone_number  VARCHAR(20),
    password_hash TEXT,
    role          user_role,
    is_active     BOOLEAN,
    last_login_at TIMESTAMPTZ,

    CONSTRAINT pk_users_aud PRIMARY KEY (id, rev)
);
ALTER TABLE users_aud
    ADD CONSTRAINT fk_users_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);

-- ── menu_items_aud ───────────────────────────────────────────────────────────
CREATE TABLE menu_items_aud
(
    -- revision metadata
    id            UUID,
    rev           BIGINT   NOT NULL,
    revtype       SMALLINT,

    -- Base fields
    created_at    TIMESTAMPTZ,
    updated_at    TIMESTAMPTZ,

    -- MenuItem-specific fields
    restaurant_id UUID,
    name          VARCHAR(100),
    description   TEXT,
    cuisine_id    UUID,
    price         DECIMAL(10, 2),
    category      VARCHAR(50),
    is_available  BOOLEAN,

    CONSTRAINT pk_menu_items_aud PRIMARY KEY (id, rev)
);
ALTER TABLE menu_items_aud
    ADD CONSTRAINT fk_menu_items_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev);
