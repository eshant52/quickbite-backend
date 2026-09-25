-- V30: Create order_dispatches and delivery_offers tables for proactive delivery dispatch

CREATE TYPE delivery_dispatch_status AS ENUM (
    'NOT_STARTED',
    'FINDING_AGENT',
    'AGENT_ASSIGNED',
    'EXHAUSTED'
);

CREATE TYPE delivery_offer_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED',
    'EXPIRED',
    'WITHDRAWN'
);

CREATE TABLE order_dispatches (
    id                UUID                     NOT NULL,
    order_id          UUID                     NOT NULL,
    status            delivery_dispatch_status NOT NULL DEFAULT 'NOT_STARTED',
    started_at        TIMESTAMPTZ,
    current_round     INT                      NOT NULL DEFAULT 0,
    next_attempt_at   TIMESTAMPTZ,
    dispatch_deadline TIMESTAMPTZ,
    created_at        TIMESTAMPTZ              NOT NULL,
    updated_at        TIMESTAMPTZ              NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,

    CONSTRAINT pk_order_dispatches PRIMARY KEY (id),
    CONSTRAINT uc_order_dispatches_orderid UNIQUE (order_id),
    CONSTRAINT fk_order_dispatches_order FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE INDEX idx_order_dispatches_next_attempt
    ON order_dispatches(next_attempt_at)
    WHERE status = 'FINDING_AGENT';

CREATE TABLE delivery_offers (
    id           UUID                  NOT NULL,
    order_id     UUID                  NOT NULL,
    agent_id     UUID                  NOT NULL,
    round_number INT                   NOT NULL,
    radius_km    DECIMAL(5,2)          NOT NULL,
    status       delivery_offer_status NOT NULL DEFAULT 'PENDING',
    offered_at   TIMESTAMPTZ           NOT NULL,
    expires_at   TIMESTAMPTZ           NOT NULL,
    responded_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ           NOT NULL,
    updated_at   TIMESTAMPTZ           NOT NULL,

    CONSTRAINT pk_delivery_offers PRIMARY KEY (id),
    CONSTRAINT uq_delivery_offers_order_agent UNIQUE (order_id, agent_id),
    CONSTRAINT fk_delivery_offers_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_delivery_offers_agent FOREIGN KEY (agent_id) REFERENCES delivery_agents(id)
);

-- Crucial: Engine-level guarantee that only 1 offer is PENDING per order
CREATE UNIQUE INDEX idx_delivery_offers_unique_pending
    ON delivery_offers(order_id)
    WHERE status = 'PENDING';

CREATE INDEX idx_delivery_offers_agent_status
    ON delivery_offers(agent_id, status);

CREATE INDEX idx_delivery_offers_expires_pending
    ON delivery_offers(expires_at)
    WHERE status = 'PENDING';
