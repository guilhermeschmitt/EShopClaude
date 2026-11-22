CREATE TABLE product (
    id          BIGSERIAL      PRIMARY KEY,
    name        VARCHAR(100)   NOT NULL,
    description VARCHAR(500),
    price       NUMERIC(10, 2) NOT NULL,
    category    VARCHAR(50)    NOT NULL,
    CONSTRAINT ck_product_price_non_negative CHECK (price >= 0)
);

CREATE INDEX ix_product_category ON product (category);
