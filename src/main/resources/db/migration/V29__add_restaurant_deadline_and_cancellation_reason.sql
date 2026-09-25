-- V29: Add restaurant acceptance deadline and cancellation reason to orders and orders_aud

CREATE TYPE order_cancellation_reason AS ENUM (
    'RESTAURANT_UNRESPONSIVE',
    'NO_AGENT_FOUND',
    'CUSTOMER_CANCELLED'
);

ALTER TABLE orders
    ADD COLUMN restaurant_acceptance_deadline TIMESTAMPTZ,
    ADD COLUMN cancellation_reason order_cancellation_reason;

ALTER TABLE orders_aud
    ADD COLUMN restaurant_acceptance_deadline TIMESTAMPTZ,
    ADD COLUMN cancellation_reason order_cancellation_reason;

CREATE INDEX idx_orders_restaurant_acceptance_deadline
    ON orders(restaurant_acceptance_deadline)
    WHERE current_status = 'PLACED';
