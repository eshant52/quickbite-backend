CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE addresses
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id       UUID                        NOT NULL,
    label         VARCHAR(50)                 NOT NULL,
    house_number  VARCHAR(20),
    building_name VARCHAR(100),
    street        VARCHAR(150)                NOT NULL,
    landmark      VARCHAR(100),
    city          VARCHAR(50)                 NOT NULL,
    state         VARCHAR(50)                 NOT NULL,
    country       VARCHAR(50)                 NOT NULL,
    postal_code   VARCHAR(10),
    location      GEOMETRY(POINT, 4326),
    is_default    BOOLEAN,
    CONSTRAINT pk_addresses PRIMARY KEY (id)
);

CREATE TABLE cart_items
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    cart_id      UUID                        NOT NULL,
    menu_item_id UUID                        NOT NULL,
    quantity     INTEGER                     NOT NULL,
    unit_price   DECIMAL(10, 2)              NOT NULL,
    sub_total    DECIMAL(10, 2)              NOT NULL,
    CONSTRAINT pk_cart_items PRIMARY KEY (id)
);

CREATE TABLE carts
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    customer_id   UUID                        NOT NULL,
    restaurant_id UUID                        NOT NULL,
    total_price   DECIMAL(10, 2)              NOT NULL,
    expires_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_carts PRIMARY KEY (id)
);

CREATE TABLE cuisines
(
    id             UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name           VARCHAR(100)                NOT NULL,
    status         CUISINE_STATUS              NOT NULL,
    reviewed_by_id UUID,
    reviewed_at    TIMESTAMP WITHOUT TIME ZONE,
    remarks        TEXT,
    CONSTRAINT pk_cuisines PRIMARY KEY (id)
);

CREATE TABLE delivery_agent_documents
(
    id                UUID                         NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    delivery_agent_id UUID                         NOT NULL,
    type              DELIVERY_AGENT_DOCUMENT_TYPE NOT NULL,
    description       TEXT,
    url               TEXT                         NOT NULL,
    status            DOCUMENT_VERIFICATION_STATUS NOT NULL,
    remarks           TEXT,
    reviewed_at       TIMESTAMP WITHOUT TIME ZONE,
    reviewed_by_id    UUID,
    CONSTRAINT pk_delivery_agent_documents PRIMARY KEY (id)
);

CREATE TABLE delivery_agent_verification_history
(
    id                UUID                               NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
    delivery_agent_id UUID                               NOT NULL,
    status            DELIVERY_AGENT_VERIFICATION_STATUS NOT NULL,
    reviewed_by_id    UUID,
    remarks           TEXT,
    CONSTRAINT pk_delivery_agent_verification_history PRIMARY KEY (id)
);

CREATE TABLE delivery_agents
(
    id                 UUID                               NOT NULL,
    created_at         TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE        NOT NULL,
    user_id            UUID                               NOT NULL,
    current_vehicle_id UUID,
    is_available       BOOLEAN                            NOT NULL,
    last_location      GEOMETRY(POINT, 4326),
    current_status     DELIVERY_AGENT_VERIFICATION_STATUS NOT NULL,
    CONSTRAINT pk_delivery_agents PRIMARY KEY (id)
);

CREATE TABLE menu_item_images
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    menu_item_id  UUID                        NOT NULL,
    image_url     TEXT                        NOT NULL,
    display_order INTEGER                     NOT NULL,
    CONSTRAINT pk_menu_item_images PRIMARY KEY (id)
);

CREATE TABLE menu_items
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    restaurant_id UUID                        NOT NULL,
    name          VARCHAR(100)                NOT NULL,
    description   TEXT                        NOT NULL,
    cuisine_id    UUID                        NOT NULL,
    price         DECIMAL(10, 2)              NOT NULL,
    category      VARCHAR(50)                 NOT NULL,
    is_available  BOOLEAN                     NOT NULL,
    CONSTRAINT pk_menu_items PRIMARY KEY (id)
);

CREATE TABLE notifications
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    title        VARCHAR(200)                NOT NULL,
    message      TEXT                        NOT NULL,
    recipient_id UUID                        NOT NULL,
    is_read      BOOLEAN                     NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
);

CREATE TABLE order_items
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    menu_item_id UUID                        NOT NULL,
    order_id     UUID                        NOT NULL,
    quantity     INTEGER                     NOT NULL,
    unit_price   DECIMAL(10, 2)              NOT NULL,
    sub_total    DECIMAL(10, 2)              NOT NULL,
    CONSTRAINT pk_order_items PRIMARY KEY (id)
);

CREATE TABLE order_notifications
(
    id       UUID                    NOT NULL,
    order_id UUID                    NOT NULL,
    type     ORDER_NOTIFICATION_TYPE NOT NULL,
    CONSTRAINT pk_order_notifications PRIMARY KEY (id)
);

CREATE TABLE order_status_history
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    order_id     UUID                        NOT NULL,
    order_status ORDER_STATUS                NOT NULL,
    CONSTRAINT pk_order_status_history PRIMARY KEY (id)
);

CREATE TABLE orders
(
    id                UUID                        NOT NULL,
    created_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    restaurant_id     UUID                        NOT NULL,
    customer_id       UUID                        NOT NULL,
    delivery_agent_id UUID,
    delivery_address  TEXT                        NOT NULL,
    delivery_location GEOMETRY(POINT, 4326),
    subtotal          DECIMAL(10, 2)              NOT NULL,
    discount_amount   DECIMAL(10, 2)              NOT NULL,
    delivery_fee      DECIMAL(10, 2)              NOT NULL,
    platform_fee      DECIMAL(10, 2)              NOT NULL,
    tax_amount        DECIMAL(10, 2)              NOT NULL,
    tip_amount        DECIMAL(10, 2)              NOT NULL,
    total_amount      DECIMAL(10, 2)              NOT NULL,
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE payment_notifications
(
    id         UUID                      NOT NULL,
    payment_id UUID                      NOT NULL,
    type       PAYMENT_NOTIFICATION_TYPE NOT NULL,
    CONSTRAINT pk_payment_notifications PRIMARY KEY (id)
);

CREATE TABLE payment_status_history
(
    id         UUID                        NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    payment_id UUID                        NOT NULL,
    status     PAYMENT_STATUS              NOT NULL,
    CONSTRAINT pk_payment_status_history PRIMARY KEY (id)
);

CREATE TABLE payments
(
    id             UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    order_id       UUID                        NOT NULL,
    transaction_id VARCHAR(255)                NOT NULL,
    payment_method PAYMENT_METHOD              NOT NULL,
    amount         DECIMAL(10, 2)              NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id)
);

CREATE TABLE refresh_tokens
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    user_id      UUID                        NOT NULL,
    family_id    UUID,
    token_hash   TEXT                        NOT NULL,
    device_name  VARCHAR(100)                NOT NULL,
    os           VARCHAR(50)                 NOT NULL,
    client_type  CLIENT_TYPE                 NOT NULL,
    revoked      BOOLEAN                     NOT NULL,
    expires_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

CREATE TABLE restaurant_documents
(
    id             UUID                         NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE  NOT NULL,
    restaurant_id  UUID                         NOT NULL,
    type           RESTAURANT_DOCUMENT_TYPE     NOT NULL,
    url            TEXT                         NOT NULL,
    status         DOCUMENT_VERIFICATION_STATUS NOT NULL,
    reviewed_by_id UUID,
    reviewed_at    TIMESTAMP WITHOUT TIME ZONE,
    remarks        TEXT,
    CONSTRAINT pk_restaurant_documents PRIMARY KEY (id)
);

CREATE TABLE restaurant_hours
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    restaurant_id UUID                        NOT NULL,
    day_of_week   DAY_OF_WEEK                 NOT NULL,
    open_time     time WITHOUT TIME ZONE      NOT NULL,
    close_time    time WITHOUT TIME ZONE      NOT NULL,
    CONSTRAINT pk_restaurant_hours PRIMARY KEY (id)
);

CREATE TABLE restaurant_images
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    restaurant_id UUID                        NOT NULL,
    image_url     TEXT                        NOT NULL,
    display_order INTEGER                     NOT NULL,
    CONSTRAINT pk_restaurant_images PRIMARY KEY (id)
);

CREATE TABLE restaurant_verification_status_history
(
    id             UUID                           NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    restaurant_id  UUID                           NOT NULL,
    reviewed_by_id UUID,
    status         RESTAURANT_VERIFICATION_STATUS NOT NULL,
    remarks        TEXT,
    CONSTRAINT pk_restaurant_verification_status_history PRIMARY KEY (id)
);

CREATE TABLE restaurants
(
    id             UUID                           NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE    NOT NULL,
    owner_id       UUID                           NOT NULL,
    name           VARCHAR(200)                   NOT NULL,
    description    TEXT                           NOT NULL,
    address_id     UUID                           NOT NULL,
    avg_rating     DECIMAL(3, 2),
    total_rating   BIGINT                         NOT NULL,
    is_closed      BOOLEAN                        NOT NULL,
    current_status RESTAURANT_VERIFICATION_STATUS NOT NULL,
    CONSTRAINT pk_restaurants PRIMARY KEY (id)
);

CREATE TABLE revchanges
(
    rev        BIGINT NOT NULL,
    entityname VARCHAR(255)
);

CREATE TABLE reviews
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    restaurant_id UUID                        NOT NULL,
    customer_id   UUID                        NOT NULL,
    order_id      UUID                        NOT NULL,
    rating        INTEGER                     NOT NULL,
    comment       TEXT,
    CONSTRAINT pk_reviews PRIMARY KEY (id)
);

CREATE TABLE revinfo
(
    rev      BIGINT NOT NULL,
    revtstmp BIGINT,
    CONSTRAINT pk_revinfo PRIMARY KEY (rev)
);

CREATE TABLE users
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    name          VARCHAR(100)                NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    phone_number  VARCHAR(20)                 NOT NULL,
    password_hash TEXT                        NOT NULL,
    role          USER_ROLE                   NOT NULL,
    is_active     BOOLEAN                     NOT NULL,
    last_login_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE vehicle_ownership_documents
(
    id                   UUID                            NOT NULL,
    created_at           TIMESTAMP WITHOUT TIME ZONE     NOT NULL,
    updated_at           TIMESTAMP WITHOUT TIME ZONE     NOT NULL,
    vehicle_ownership_id UUID                            NOT NULL,
    description          TEXT,
    url                  TEXT                            NOT NULL,
    type                 VEHICLE_OWNERSHIP_DOCUMENT_TYPE NOT NULL,
    status               DOCUMENT_VERIFICATION_STATUS    NOT NULL,
    reviewed_by_id       UUID,
    reviewed_at          TIMESTAMP WITHOUT TIME ZONE,
    remarks              TEXT,
    CONSTRAINT pk_vehicle_ownership_documents PRIMARY KEY (id)
);

CREATE TABLE vehicle_ownership_status_history
(
    id                   UUID                        NOT NULL,
    created_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vehicle_ownership_id UUID                        NOT NULL,
    status               OWNERSHIP_STATUS            NOT NULL,
    reviewed_by_id       UUID,
    remarks              TEXT,
    CONSTRAINT pk_vehicle_ownership_status_history PRIMARY KEY (id)
);

CREATE TABLE vehicle_ownerships
(
    id             UUID                        NOT NULL,
    created_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vehicle_id     UUID                        NOT NULL,
    owner_id       UUID                        NOT NULL,
    current_status OWNERSHIP_STATUS            NOT NULL,
    CONSTRAINT pk_vehicle_ownerships PRIMARY KEY (id)
);

CREATE TABLE vehicles
(
    id           UUID                        NOT NULL,
    created_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vehicle_type VEHICLE_TYPE                NOT NULL,
    number_plate VARCHAR(20)                 NOT NULL,
    brand        VARCHAR(50)                 NOT NULL,
    model        VARCHAR(50)                 NOT NULL,
    CONSTRAINT pk_vehicles PRIMARY KEY (id)
);

ALTER TABLE carts
    ADD CONSTRAINT uc_carts_customer UNIQUE (customer_id);

ALTER TABLE cuisines
    ADD CONSTRAINT uc_cuisines_name UNIQUE (name);

ALTER TABLE delivery_agents
    ADD CONSTRAINT uc_delivery_agents_user UNIQUE (user_id);

ALTER TABLE payments
    ADD CONSTRAINT uc_payments_transactionid UNIQUE (transaction_id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uc_refresh_tokens_tokenhash UNIQUE (token_hash);

ALTER TABLE restaurants
    ADD CONSTRAINT uc_restaurants_address UNIQUE (address_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_phonenumber UNIQUE (phone_number);

ALTER TABLE vehicles
    ADD CONSTRAINT uc_vehicles_numberplate UNIQUE (number_plate);

ALTER TABLE addresses
    ADD CONSTRAINT FK_ADDRESSES_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE carts
    ADD CONSTRAINT FK_CARTS_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES users (id);

ALTER TABLE carts
    ADD CONSTRAINT FK_CARTS_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE cart_items
    ADD CONSTRAINT FK_CART_ITEMS_ON_CART FOREIGN KEY (cart_id) REFERENCES carts (id);

ALTER TABLE cart_items
    ADD CONSTRAINT FK_CART_ITEMS_ON_MENUITEM FOREIGN KEY (menu_item_id) REFERENCES menu_items (id);

ALTER TABLE cuisines
    ADD CONSTRAINT FK_CUISINES_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE delivery_agents
    ADD CONSTRAINT FK_DELIVERY_AGENTS_ON_CURRENTVEHICLE FOREIGN KEY (current_vehicle_id) REFERENCES vehicles (id);

ALTER TABLE delivery_agents
    ADD CONSTRAINT FK_DELIVERY_AGENTS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE delivery_agent_documents
    ADD CONSTRAINT FK_DELIVERY_AGENT_DOCUMENTS_ON_DELIVERYAGENT FOREIGN KEY (delivery_agent_id) REFERENCES delivery_agents (id);

ALTER TABLE delivery_agent_documents
    ADD CONSTRAINT FK_DELIVERY_AGENT_DOCUMENTS_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE delivery_agent_verification_history
    ADD CONSTRAINT FK_DELIVERY_AGENT_VERIFICATION_HISTORY_ON_DELIVERYAGENT FOREIGN KEY (delivery_agent_id) REFERENCES delivery_agents (id);

ALTER TABLE delivery_agent_verification_history
    ADD CONSTRAINT FK_DELIVERY_AGENT_VERIFICATION_HISTORY_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE menu_items
    ADD CONSTRAINT FK_MENU_ITEMS_ON_CUISINE FOREIGN KEY (cuisine_id) REFERENCES cuisines (id);

ALTER TABLE menu_items
    ADD CONSTRAINT FK_MENU_ITEMS_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE menu_item_images
    ADD CONSTRAINT FK_MENU_ITEM_IMAGES_ON_MENUITEM FOREIGN KEY (menu_item_id) REFERENCES menu_items (id);

ALTER TABLE notifications
    ADD CONSTRAINT FK_NOTIFICATIONS_ON_RECIPIENT FOREIGN KEY (recipient_id) REFERENCES users (id);

ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES users (id);

ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_DELIVERYAGENT FOREIGN KEY (delivery_agent_id) REFERENCES delivery_agents (id);

ALTER TABLE orders
    ADD CONSTRAINT FK_ORDERS_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE order_items
    ADD CONSTRAINT FK_ORDER_ITEMS_ON_MENUITEM FOREIGN KEY (menu_item_id) REFERENCES menu_items (id);

ALTER TABLE order_items
    ADD CONSTRAINT FK_ORDER_ITEMS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE order_notifications
    ADD CONSTRAINT FK_ORDER_NOTIFICATIONS_ON_ID FOREIGN KEY (id) REFERENCES notifications (id);

ALTER TABLE order_notifications
    ADD CONSTRAINT FK_ORDER_NOTIFICATIONS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE order_status_history
    ADD CONSTRAINT FK_ORDER_STATUS_HISTORY_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE payments
    ADD CONSTRAINT FK_PAYMENTS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE payment_notifications
    ADD CONSTRAINT FK_PAYMENT_NOTIFICATIONS_ON_ID FOREIGN KEY (id) REFERENCES notifications (id);

ALTER TABLE payment_notifications
    ADD CONSTRAINT FK_PAYMENT_NOTIFICATIONS_ON_PAYMENT FOREIGN KEY (payment_id) REFERENCES payments (id);

ALTER TABLE payment_status_history
    ADD CONSTRAINT FK_PAYMENT_STATUS_HISTORY_ON_PAYMENT FOREIGN KEY (payment_id) REFERENCES payments (id);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT FK_REFRESH_TOKENS_ON_USER FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE restaurants
    ADD CONSTRAINT FK_RESTAURANTS_ON_ADDRESS FOREIGN KEY (address_id) REFERENCES addresses (id);

ALTER TABLE restaurants
    ADD CONSTRAINT FK_RESTAURANTS_ON_OWNER FOREIGN KEY (owner_id) REFERENCES users (id);

ALTER TABLE restaurant_documents
    ADD CONSTRAINT FK_RESTAURANT_DOCUMENTS_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE restaurant_documents
    ADD CONSTRAINT FK_RESTAURANT_DOCUMENTS_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE restaurant_hours
    ADD CONSTRAINT FK_RESTAURANT_HOURS_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE restaurant_images
    ADD CONSTRAINT FK_RESTAURANT_IMAGES_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE restaurant_verification_status_history
    ADD CONSTRAINT FK_RESTAURANT_VERIFICATION_STATUS_HISTORY_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE restaurant_verification_status_history
    ADD CONSTRAINT FK_RESTAURANT_VERIFICATION_STATUS_HISTORY_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE reviews
    ADD CONSTRAINT FK_REVIEWS_ON_CUSTOMER FOREIGN KEY (customer_id) REFERENCES users (id);

ALTER TABLE reviews
    ADD CONSTRAINT FK_REVIEWS_ON_ORDER FOREIGN KEY (order_id) REFERENCES orders (id);

ALTER TABLE reviews
    ADD CONSTRAINT FK_REVIEWS_ON_RESTAURANT FOREIGN KEY (restaurant_id) REFERENCES restaurants (id);

ALTER TABLE vehicle_ownerships
    ADD CONSTRAINT FK_VEHICLE_OWNERSHIPS_ON_OWNER FOREIGN KEY (owner_id) REFERENCES delivery_agents (id);

ALTER TABLE vehicle_ownerships
    ADD CONSTRAINT FK_VEHICLE_OWNERSHIPS_ON_VEHICLE FOREIGN KEY (vehicle_id) REFERENCES vehicles (id);

ALTER TABLE vehicle_ownership_documents
    ADD CONSTRAINT FK_VEHICLE_OWNERSHIP_DOCUMENTS_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE vehicle_ownership_documents
    ADD CONSTRAINT FK_VEHICLE_OWNERSHIP_DOCUMENTS_ON_VEHICLEOWNERSHIP FOREIGN KEY (vehicle_ownership_id) REFERENCES vehicle_ownerships (id);

ALTER TABLE vehicle_ownership_status_history
    ADD CONSTRAINT FK_VEHICLE_OWNERSHIP_STATUS_HISTORY_ON_REVIEWEDBY FOREIGN KEY (reviewed_by_id) REFERENCES users (id);

ALTER TABLE vehicle_ownership_status_history
    ADD CONSTRAINT FK_VEHICLE_OWNERSHIP_STATUS_HISTORY_ON_VEHICLEOWNERSHIP FOREIGN KEY (vehicle_ownership_id) REFERENCES vehicle_ownerships (id);

ALTER TABLE revchanges
    ADD CONSTRAINT fk_revchanges_on_default_tracking_modified_entities_changelog FOREIGN KEY (rev) REFERENCES revinfo (rev);

DROP TABLE spatial_ref_sys CASCADE;