-- ============================================
-- QTS PostgreSQL Initialization Script
-- Version: 1.0.0
-- Description: Initialize QTS trading system databases
-- ============================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- ============================================
-- Database: qts_trade (Trading Core)
-- ============================================
CREATE DATABASE qts_trade;
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

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_symbol ON orders(symbol);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_order_time ON orders(order_time DESC);

-- Order events (audit log)
CREATE TABLE IF NOT EXISTS order_events (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    event_data JSONB,
    event_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_order_events_order_id ON order_events(order_id);
CREATE INDEX idx_order_events_event_time ON order_events(event_time DESC);

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

CREATE INDEX idx_trades_symbol ON trades(symbol);
CREATE INDEX idx_trades_trade_time ON trades(trade_time DESC);

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

CREATE INDEX idx_positions_account ON positions(account_id);

-- ============================================
-- Database: qts_auth (Authentication)
-- ============================================
CREATE DATABASE qts_auth;
\c qts_auth;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id VARCHAR(64) UNIQUE NOT NULL,
    username VARCHAR(128) UNIQUE NOT NULL,
    email VARCHAR(256) UNIQUE NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    status VARCHAR(16) DEFAULT 'ACTIVE',
    user_type VARCHAR(32),
    last_login TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- Sessions table
CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id VARCHAR(128) UNIQUE NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    ip_address VARCHAR(64),
    user_agent TEXT,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_expires ON sessions(expires_at);

-- API keys table
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    key_id VARCHAR(64) UNIQUE NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    key_hash VARCHAR(256) NOT NULL,
    description TEXT,
    permissions JSONB,
    rate_limit INTEGER DEFAULT 1000,
    expires_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_api_keys_user ON api_keys(user_id);

-- ============================================
-- Database: qts_risk (Risk Control)
-- ============================================
CREATE DATABASE qts_risk;
\c qts_risk;

-- Risk rules table
CREATE TABLE IF NOT EXISTS risk_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    rule_id VARCHAR(64) UNIQUE NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    condition JSONB NOT NULL,
    action VARCHAR(32) NOT NULL,
    priority INTEGER DEFAULT 0,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_risk_rules_type ON risk_rules(rule_type);
CREATE INDEX idx_risk_rules_enabled ON risk_rules(enabled);

-- Risk audit log
CREATE TABLE IF NOT EXISTS risk_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    order_id VARCHAR(64),
    rule_id VARCHAR(64),
    action_taken VARCHAR(32),
    risk_score DECIMAL(6, 2),
    details JSONB,
    decision VARCHAR(16),
    decision_time TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_risk_audit_user ON risk_audit_logs(user_id);
CREATE INDEX idx_risk_audit_order ON risk_audit_logs(order_id);
CREATE INDEX idx_risk_audit_time ON risk_audit_logs(decision_time DESC);

-- Position limits
CREATE TABLE IF NOT EXISTS position_limits (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id VARCHAR(32) NOT NULL,
    symbol VARCHAR(16),
    max_position DECIMAL(18, 6),
    max_order_size DECIMAL(18, 6),
    daily_trade_limit DECIMAL(18, 6),
    update_time TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(account_id, symbol)
);

-- ============================================
-- Function: Auto-update timestamps
-- ============================================
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers
\c qts_trade;
CREATE TRIGGER update_orders_time BEFORE UPDATE ON orders FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_positions_time BEFORE UPDATE ON positions FOR EACH ROW EXECUTE FUNCTION update_updated_at();

\c qts_auth;
CREATE TRIGGER update_users_time BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();

\c qts_risk;
CREATE TRIGGER update_risk_rules_time BEFORE UPDATE ON risk_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER update_position_limits_time BEFORE UPDATE ON position_limits FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================
-- Grant permissions
-- ============================================
-- Application user (read/write)
CREATE USER qts_app WITH PASSWORD 'CHANGE_ME_IN_PROD';
GRANT ALL PRIVILEGES ON DATABASE qts_trade TO qts_app;
GRANT ALL PRIVILEGES ON DATABASE qts_auth TO qts_app;
GRANT ALL PRIVILEGES ON DATABASE qts_risk TO qts_app;

-- Monitoring user (read-only)
CREATE USER qts_monitor WITH PASSWORD 'CHANGE_ME_IN_PROD';
GRANT CONNECT ON DATABASE qts_trade TO qts_monitor;
GRANT CONNECT ON DATABASE qts_auth TO qts_monitor;
GRANT CONNECT ON DATABASE qts_risk TO qts_monitor;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO qts_monitor;

-- ============================================
-- Initial data
-- ============================================
\c qts_risk;

-- Default risk rules
INSERT INTO risk_rules (rule_id, rule_name, rule_type, condition, action, priority, enabled)
VALUES 
    ('RULE_001', 'Max Order Size', 'ORDER_SIZE', '{"max_quantity": 1000000}', 'REJECT', 100, true),
    ('RULE_002', 'Max Position', 'POSITION', '{"max_position": 5000000}', 'REJECT', 90, true),
    ('RULE_003', 'Price Deviation', 'PRICE', '{"max_deviation_percent": 5}', 'ALERT', 80, true),
    ('RULE_004', 'Trading Hours', 'TIME', '{"start_hour": 9, "end_hour": 15}', 'REJECT', 95, true)
ON CONFLICT (rule_id) DO NOTHING;

-- ============================================
-- Done
-- ============================================
DO $$
BEGIN
    RAISE NOTICE 'QTS databases initialized successfully';
END $$;