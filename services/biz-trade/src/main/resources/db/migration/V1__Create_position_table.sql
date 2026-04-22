-- V1__Create_position_table.sql
-- Position management table for tracking user holdings

CREATE TABLE IF NOT EXISTS position (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL,
    symbol VARCHAR(50) NOT NULL,
    symbol_name VARCHAR(100),
    quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    available_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    frozen_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    cost_price DECIMAL(20, 8) NOT NULL DEFAULT 0,
    market_price DECIMAL(20, 8) NOT NULL DEFAULT 0,
    today_buy_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    today_sell_quantity DECIMAL(20, 8) NOT NULL DEFAULT 0,
    today_buy_amount DECIMAL(24, 8) NOT NULL DEFAULT 0,
    today_sell_amount DECIMAL(24, 8) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_position_account_symbol UNIQUE (account_id, symbol)
);

-- Index for account queries
CREATE INDEX idx_position_account_id ON position(account_id);

-- Index for symbol searches
CREATE INDEX idx_position_symbol ON position(symbol);

COMMENT ON TABLE position IS 'User position/holding tracking table';
COMMENT ON COLUMN position.account_id IS 'User account ID';
COMMENT ON COLUMN position.symbol IS 'Trading symbol, e.g., BTC-USDT';
COMMENT ON COLUMN position.quantity IS 'Total position quantity';
COMMENT ON COLUMN position.available_quantity IS 'Available quantity (total - frozen)';
COMMENT ON COLUMN position.frozen_quantity IS 'Quantity frozen for pending orders';
COMMENT ON COLUMN position.cost_price IS 'Average cost price';
COMMENT ON COLUMN position.market_price IS 'Current market price';
COMMENT ON COLUMN position.today_buy_quantity IS 'Today buy total quantity';
COMMENT ON COLUMN position.today_sell_quantity IS 'Today sell total quantity';