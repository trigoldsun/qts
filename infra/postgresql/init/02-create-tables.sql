-- QTS Schema - Part 2: qts_trade (Trading Core)
\c qts_trade;

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id VARCHAR(64) UNIQUE NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    side VARCHAR(4) NOT NULL CHECK (side IN ('BUY', 'SELL')),
    order_type VARCHAR(16) NOT NULL CHECK (order_type IN ('MARKET', 'LIMIT', 'STOP')),
    price DECIMAL(18, 6),
    quantity DECIMAL(18, 6) NOT NULL,
    filled_quantity DECIMAL(18, 6) DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    user_id VARCHAR(64) NOT NULL,
    account_id VARCHAR(32),
    order_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    update_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_symbol ON orders(symbol);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_order_time ON orders(order_time DESC);

-- Order events (audit log)
CREATE TABLE IF NOT EXISTS order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_data JSONB,
    event_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_order_events_order_id ON order_events(order_id);
CREATE INDEX IF NOT EXISTS idx_order_events_event_time ON order_events(event_time DESC);

-- Trades table
CREATE TABLE IF NOT EXISTS trades (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    trade_id VARCHAR(64) UNIQUE NOT NULL,
    order_id VARCHAR(64) NOT NULL,
    counter_order_id VARCHAR(64),
    symbol VARCHAR(16) NOT NULL,
    side VARCHAR(4) NOT NULL,
    price DECIMAL(18, 6) NOT NULL,
    quantity DECIMAL(18, 6) NOT NULL,
    trade_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trades_symbol ON trades(symbol);
CREATE INDEX IF NOT EXISTS idx_trades_trade_time ON trades(trade_time DESC);

-- Positions table
CREATE TABLE IF NOT EXISTS positions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id VARCHAR(32) NOT NULL,
    symbol VARCHAR(16) NOT NULL,
    quantity DECIMAL(18, 6) DEFAULT 0,
    avg_price DECIMAL(18, 6) DEFAULT 0,
    unrealized_pnl DECIMAL(18, 6) DEFAULT 0,
    realized_pnl DECIMAL(18, 6) DEFAULT 0,
    update_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(account_id, symbol)
);

CREATE INDEX IF NOT EXISTS idx_positions_account ON positions(account_id);

-- Auto-update trigger function
CREATE OR REPLACE FUNCTION update_updated_at() RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

-- Apply triggers
DROP TRIGGER IF EXISTS update_orders_time ON orders;
CREATE TRIGGER update_orders_time BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at();

DROP TRIGGER IF EXISTS update_positions_time ON positions;
CREATE TRIGGER update_positions_time BEFORE UPDATE ON positions FOR EACH ROW EXECUTE FUNCTION update_updated_at();