CREATE TABLE orders (
    id         BIGSERIAL      PRIMARY KEY,
    buyer_id   UUID           NOT NULL,
    created_at TIMESTAMPTZ    NOT NULL,
    total      NUMERIC(10, 2) NOT NULL,
    CONSTRAINT ck_orders_total_non_negative CHECK (total >= 0)
);

CREATE INDEX ix_orders_buyer_id ON orders (buyer_id);

CREATE TABLE order_item (
    id           BIGSERIAL      PRIMARY KEY,
    order_id     BIGINT         NOT NULL,
    product_id   BIGINT         NOT NULL,
    product_name VARCHAR(100)   NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INT            NOT NULL,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT ck_order_item_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_order_item_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX ix_order_item_order_id ON order_item (order_id);
