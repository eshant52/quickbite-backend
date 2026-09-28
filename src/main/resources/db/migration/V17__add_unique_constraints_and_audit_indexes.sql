ALTER TABLE cuisine_requests
    ADD CONSTRAINT uc_cuisine_requests_cuisine UNIQUE (cuisine_id);

ALTER TABLE restaurant_applications
    ADD CONSTRAINT uc_restaurant_applications_restaurant UNIQUE (restaurant_id);

CREATE UNIQUE INDEX IX_pk_delivery_agents_aud ON delivery_agents_aud (rev, id);

CREATE UNIQUE INDEX IX_pk_menu_items_aud ON menu_items_aud (rev, id);

CREATE UNIQUE INDEX IX_pk_orders_aud ON orders_aud (rev, id);

CREATE UNIQUE INDEX IX_pk_payments_aud ON payments_aud (rev, id);

CREATE UNIQUE INDEX IX_pk_restaurants_aud ON restaurants_aud (rev, id);

CREATE UNIQUE INDEX IX_pk_users_aud ON users_aud (rev, id);

ALTER TABLE payments_aud
    ALTER COLUMN amount TYPE DECIMAL USING (amount::DECIMAL);

ALTER TABLE restaurants_aud
    ALTER COLUMN avg_rating TYPE DECIMAL USING (avg_rating::DECIMAL);

ALTER TABLE orders_aud
    ALTER COLUMN delivery_fee TYPE DECIMAL USING (delivery_fee::DECIMAL);

ALTER TABLE orders_aud
    ALTER COLUMN discount_amount TYPE DECIMAL USING (discount_amount::DECIMAL);

ALTER TABLE orders_aud
    ALTER COLUMN platform_fee TYPE DECIMAL USING (platform_fee::DECIMAL);

ALTER TABLE menu_items_aud
    ALTER COLUMN price TYPE DECIMAL USING (price::DECIMAL);

ALTER TABLE delivery_agents_aud
    DROP COLUMN rev;

ALTER TABLE delivery_agents_aud
    ADD rev INTEGER NOT NULL PRIMARY KEY;

ALTER TABLE delivery_agents_aud
    ADD CONSTRAINT FK_DELIVERY_AGENTS_AUD_ON_REV FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE menu_items_aud
    DROP COLUMN rev;

ALTER TABLE menu_items_aud
    ADD rev INTEGER NOT NULL PRIMARY KEY;

ALTER TABLE menu_items_aud
    ADD CONSTRAINT FK_MENU_ITEMS_AUD_ON_REV FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE orders_aud
    DROP COLUMN rev;

ALTER TABLE orders_aud
    ADD rev INTEGER NOT NULL PRIMARY KEY;

ALTER TABLE orders_aud
    ADD CONSTRAINT FK_ORDERS_AUD_ON_REV FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE payments_aud
    DROP COLUMN rev;

ALTER TABLE payments_aud
    ADD rev INTEGER NOT NULL PRIMARY KEY;

ALTER TABLE payments_aud
    ADD CONSTRAINT FK_PAYMENTS_AUD_ON_REV FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE restaurants_aud
    DROP COLUMN rev;

ALTER TABLE restaurants_aud
    ADD rev INTEGER NOT NULL PRIMARY KEY;

ALTER TABLE restaurants_aud
    ADD CONSTRAINT FK_RESTAURANTS_AUD_ON_REV FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE users_aud
    DROP COLUMN rev;

ALTER TABLE users_aud
    ADD rev INTEGER NOT NULL PRIMARY KEY;

ALTER TABLE users_aud
    ADD CONSTRAINT FK_USERS_AUD_ON_REV FOREIGN KEY (rev) REFERENCES revinfo (rev);

ALTER TABLE orders_aud
    ALTER COLUMN subtotal TYPE DECIMAL USING (subtotal::DECIMAL);

ALTER TABLE orders_aud
    ALTER COLUMN tax_amount TYPE DECIMAL USING (tax_amount::DECIMAL);

ALTER TABLE orders_aud
    ALTER COLUMN tip_amount TYPE DECIMAL USING (tip_amount::DECIMAL);

ALTER TABLE orders_aud
    ALTER COLUMN total_amount TYPE DECIMAL USING (total_amount::DECIMAL);