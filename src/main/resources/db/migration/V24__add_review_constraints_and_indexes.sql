-- 1. Ensure an order can only ever be reviewed once (Storage layer guarantee)
ALTER TABLE reviews
    ADD CONSTRAINT uq_reviews_order_id UNIQUE (order_id);

-- 3. Fast cursor pagination for customer review history: WHERE customer_id = ? AND id < ? ORDER BY id DESC
CREATE INDEX idx_reviews_customer_id ON reviews (customer_id, id DESC);

-- 2. Fast cursor pagination for restaurant public reviews: WHERE restaurant_id = ? AND id < ? ORDER BY id DESC
CREATE INDEX idx_reviews_restaurant_id ON reviews (restaurant_id, id DESC);