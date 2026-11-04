ALTER TABLE order_status_history
    ADD COLUMN changed_by_id UUID;

ALTER TABLE order_status_history
    ADD CONSTRAINT fk_order_status_history_changed_by
    FOREIGN KEY (changed_by_id) REFERENCES users (id);

CREATE INDEX IF NOT EXISTS idx_order_status_history_changed_by
    ON order_status_history (changed_by_id);
