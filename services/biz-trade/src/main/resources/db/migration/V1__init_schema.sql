-- V1__init_schema.sql
-- PostgreSQL Schema for biz-trade service
-- Created for Sprint 1

-- ============================================
-- Orders Table
-- ============================================
CREATE TABLE orders (
    order_id          BIGINT PRIMARY KEY,
    client_order_id   VARCHAR(64) NOT NULL UNIQUE,
    account_id        BIGINT NOT NULL,
    symbol            VARCHAR(32) NOT NULL,
    side              VARCHAR(8) NOT NULL,
    order_type        VARCHAR(16) NOT NULL,
    price             DECIMAL(18, 6),
    quantity          DECIMAL(18, 6) NOT NULL,
    filled_qty        DECIMAL(18, 6) NOT NULL DEFAULT 0,
    avg_price         DECIMAL(18, 6) NOT NULL DEFAULT 0,
    status            VARCHAR(16) NOT NULL,
    reject_code       VARCHAR(32),
    reject_reason     VARCHAR(256),
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at      TIMESTAMP,
    filled_at         TIMESTAMP,
    cancelled_at      TIMESTAMP,
    modify_version    BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_orders_account_id ON orders(account_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- ============================================
-- Positions Table
-- ============================================
CREATE TABLE positions (
    position_id       BIGINT PRIMARY KEY,
    account_id        BIGINT NOT NULL,
    symbol            VARCHAR(32) NOT NULL,
    quantity          DECIMAL(18, 6) NOT NULL DEFAULT 0,
    available_qty     DECIMAL(18, 6) NOT NULL DEFAULT 0,
    frozen_qty        DECIMAL(18, 6) NOT NULL DEFAULT 0,
    cost_price        DECIMAL(18, 6) NOT NULL DEFAULT 0,
    market_price      DECIMAL(18, 6) NOT NULL DEFAULT 0,
    market_value      DECIMAL(18, 6) NOT NULL DEFAULT 0,
    profit_loss       DECIMAL(18, 6) NOT NULL DEFAULT 0,
    today_buy_qty     DECIMAL(18, 6) NOT NULL DEFAULT 0,
    today_sell_qty    DECIMAL(18, 6) NOT NULL DEFAULT 0,
    UNIQUE(account_id, symbol)
);

CREATE INDEX idx_positions_account_id ON positions(account_id);

-- ============================================
-- Assets Table
-- ============================================
CREATE TABLE assets (
    account_id            BIGINT PRIMARY KEY,
    currency              VARCHAR(8) NOT NULL DEFAULT 'CNY',
    total_assets          DECIMAL(18, 6) NOT NULL DEFAULT 0,
    available_cash        DECIMAL(18, 6) NOT NULL DEFAULT 0,
    frozen_cash           DECIMAL(18, 6) NOT NULL DEFAULT 0,
    market_value          DECIMAL(18, 6) NOT NULL DEFAULT 0,
    margin                DECIMAL(18, 6) NOT NULL DEFAULT 0,
    maintenance_margin    DECIMAL(18, 6) NOT NULL DEFAULT 0
);

-- ============================================
-- Trade Events Table
-- ============================================
CREATE TABLE trade_events (
    trade_id     BIGINT PRIMARY KEY,
    order_id     BIGINT NOT NULL,
    account_id   BIGINT NOT NULL,
    symbol       VARCHAR(32) NOT NULL,
    side         VARCHAR(8) NOT NULL,
    price        DECIMAL(18, 6) NOT NULL,
    quantity     DECIMAL(18, 6) NOT NULL,
    trade_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trade_events_order_id ON trade_events(order_id);
CREATE INDEX idx_trade_events_account_id ON trade_events(account_id);
CREATE INDEX idx_trade_events_trade_time ON trade_events(trade_time);

-- ============================================
-- Risk Rules Table
-- ============================================
CREATE TABLE risk_rules (
    rule_id     BIGINT PRIMARY KEY,
    rule_name   VARCHAR(64) NOT NULL UNIQUE,
    rule_type   VARCHAR(32) NOT NULL,
    threshold   DECIMAL(18, 6) NOT NULL,
    action      VARCHAR(16) NOT NULL,
    enabled     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_risk_rules_rule_type ON risk_rules(rule_type);
CREATE INDEX idx_risk_rules_enabled ON risk_rules(enabled);
