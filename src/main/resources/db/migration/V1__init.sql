CREATE TABLE assets (
    id           UUID           NOT NULL,
    customer_id  VARCHAR(64)    NOT NULL,
    asset_name   VARCHAR(32)    NOT NULL,
    size         NUMERIC(38, 8) NOT NULL,
    usable_size  NUMERIC(38, 8) NOT NULL,
    version      BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT pk_assets PRIMARY KEY (id),
    CONSTRAINT uq_assets_customer_asset UNIQUE (customer_id, asset_name),
    CONSTRAINT ck_assets_size_non_negative CHECK (size >= 0),
    CONSTRAINT ck_assets_usable_non_negative CHECK (usable_size >= 0),
    CONSTRAINT ck_assets_usable_within_size CHECK (usable_size <= size)
);

CREATE INDEX idx_assets_customer ON assets (customer_id, asset_name);

CREATE TABLE orders (
    id          UUID                     NOT NULL,
    customer_id VARCHAR(64)              NOT NULL,
    asset_name  VARCHAR(32)              NOT NULL,
    order_side  VARCHAR(4)               NOT NULL,
    size        NUMERIC(38, 8)           NOT NULL,
    price       NUMERIC(38, 8)           NOT NULL,
    status      VARCHAR(16)              NOT NULL,
    create_date TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT pk_orders PRIMARY KEY (id),
    CONSTRAINT ck_orders_size_positive CHECK (size > 0),
    CONSTRAINT ck_orders_price_positive CHECK (price > 0),
    CONSTRAINT ck_orders_side CHECK (order_side IN ('BUY', 'SELL')),
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING', 'MATCHED', 'CANCELED')),
    CONSTRAINT ck_orders_asset_not_currency CHECK (asset_name <> 'TRY')
);

CREATE INDEX idx_orders_customer_create_date ON orders (customer_id, create_date DESC);

CREATE INDEX idx_orders_status ON orders (status);

CREATE TABLE app_users (
    id            UUID         NOT NULL,
    username      VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL,
    customer_id   VARCHAR(64),
    CONSTRAINT pk_app_users PRIMARY KEY (id),
    CONSTRAINT uq_app_users_username UNIQUE (username),
    CONSTRAINT ck_app_users_role CHECK (role IN ('ADMIN', 'CUSTOMER')),
    CONSTRAINT ck_app_users_customer_link CHECK (
        (role = 'ADMIN' AND customer_id IS NULL)
            OR (role = 'CUSTOMER' AND customer_id IS NOT NULL))
);
