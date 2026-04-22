-- QTS Schema - Part 4: qts_risk (Risk Control)
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

CREATE INDEX IF NOT EXISTS idx_risk_rules_type ON risk_rules(rule_type);
CREATE INDEX IF NOT EXISTS idx_risk_rules_enabled ON risk_rules(enabled);

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

CREATE INDEX IF NOT EXISTS idx_risk_audit_user ON risk_audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_risk_audit_order ON risk_audit_logs(order_id);
CREATE INDEX IF NOT EXISTS idx_risk_audit_time ON risk_audit_logs(decision_time DESC);

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

-- Account risk profiles
CREATE TABLE IF NOT EXISTS account_risk_profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id VARCHAR(32) UNIQUE NOT NULL,
    risk_level VARCHAR(16) DEFAULT 'MEDIUM',
    max_daily_loss DECIMAL(18, 6),
    max_position_limit DECIMAL(18, 6),
    trading_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_account_risk_account ON account_risk_profiles(account_id);

-- Blacklist/whitelist
CREATE TABLE IF NOT EXISTS risk_lists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    list_type VARCHAR(16) NOT NULL CHECK (list_type IN ('BLACKLIST', 'WHITELIST')),
    target_type VARCHAR(32) NOT NULL CHECK (target_type IN ('USER', 'ACCOUNT', 'SYMBOL', 'IP')),
    target_value VARCHAR(128) NOT NULL,
    reason TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_risk_lists_type ON risk_lists(list_type);
CREATE INDEX IF NOT EXISTS idx_risk_lists_target ON risk_lists(target_type, target_value);

-- Trigger
CREATE OR REPLACE FUNCTION update_updated_at() RETURNS TRIGGER AS $$
BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_risk_rules_time ON risk_rules;
CREATE TRIGGER update_risk_rules_time BEFORE UPDATE ON risk_rules FOR EACH ROW EXECUTE FUNCTION update_updated_at();

DROP TRIGGER IF EXISTS update_position_limits_time ON position_limits;
CREATE TRIGGER update_position_limits_time BEFORE UPDATE ON position_limits FOR EACH ROW EXECUTE FUNCTION update_updated_at();