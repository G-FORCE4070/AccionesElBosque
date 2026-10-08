CREATE SCHEMA IF NOT EXISTS trading;

CREATE TABLE IF NOT EXISTS trading.accounts (
    user_id    UUID           PRIMARY KEY,
    balance    NUMERIC(15, 2) NOT NULL,
    updated_at TIMESTAMP      NOT NULL
);

CREATE TABLE IF NOT EXISTS trading.holdings (
    user_id    UUID           NOT NULL,
    symbol     VARCHAR(10)    NOT NULL,
    quantity   INTEGER        NOT NULL,
    avg_cost   NUMERIC(15, 2) NOT NULL,
    updated_at TIMESTAMP      NOT NULL,
    PRIMARY KEY (user_id, symbol)
);

CREATE TABLE IF NOT EXISTS trading.orders (
    id               UUID           PRIMARY KEY,
    user_id          UUID           NOT NULL,
    symbol           VARCHAR(10)    NOT NULL,
    side             VARCHAR(4)     NOT NULL,
    order_type       VARCHAR(20)    NOT NULL,
    quantity         INTEGER        NOT NULL,
    estimated_price  NUMERIC(15, 2),
    executed_price   NUMERIC(15, 2),
    commission       NUMERIC(15, 2),
    status           VARCHAR(20)    NOT NULL,
    alpaca_order_id  VARCHAR(100),
    created_at       TIMESTAMP,
    executed_at      TIMESTAMP
);
