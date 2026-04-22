-- QTS Schema - Part 5: Default Data
\c qts_risk;

-- Default risk rules
INSERT INTO risk_rules (rule_id, rule_name, rule_type, condition, action, priority, enabled)
VALUES 
    ('RULE_001', 'Max Order Size', 'ORDER_SIZE', '{"max_quantity": 1000000}', 'REJECT', 100, true),
    ('RULE_002', 'Max Position', 'POSITION', '{"max_position": 5000000}', 'REJECT', 90, true),
    ('RULE_003', 'Price Deviation', 'PRICE', '{"max_deviation_percent": 5}', 'ALERT', 80, true),
    ('RULE_004', 'Trading Hours', 'TIME', '{"start_hour": 9, "end_hour": 15}', 'REJECT', 95, true),
    ('RULE_005', 'Daily Loss Limit', 'LOSS', '{"max_daily_loss": 100000}', 'REJECT', 99, true),
    ('RULE_006', 'Min Order Price', 'PRICE', '{"min_price": 0.01}', 'REJECT', 85, true),
    ('RULE_007', 'Rate Limit', 'FREQUENCY', '{"max_orders_per_minute": 100}', 'REJECT', 95, true),
    ('RULE_008', 'Concentration', 'CONCENTRATION', '{"max_position_percent": 20}', 'ALERT', 70, true)
ON CONFLICT (rule_id) DO NOTHING;

-- Default position limits
INSERT INTO position_limits (account_id, symbol, max_position, max_order_size, daily_trade_limit)
VALUES 
    ('DEFAULT', NULL, 10000000, 1000000, 50000000),
    ('VIP001', NULL, 100000000, 10000000, 500000000),
    ('MARGIN', NULL, 5000000, 500000, 25000000)
ON CONFLICT (account_id, symbol) DO NOTHING;

-- Default risk profiles
INSERT INTO account_risk_profiles (account_id, risk_level, max_daily_loss, max_position_limit, trading_enabled)
VALUES 
    ('DEFAULT', 'LOW', 50000, 5000000, true),
    ('MARGIN', 'MEDIUM', 100000, 10000000, true),
    ('PRO', 'HIGH', 500000, 50000000, true)
ON CONFLICT (account_id) DO NOTHING;

\c qts_auth;

-- Default roles
INSERT INTO roles (role_name, description)
VALUES 
    ('ADMIN', 'Full system access'),
    ('TRADER', 'Trading access'),
    ('VIEWER', 'Read-only access'),
    ('RISK_MANAGER', 'Risk management access')
ON CONFLICT (role_name) DO NOTHING;

-- Default permissions
INSERT INTO permissions (permission_name, description)
VALUES 
    ('order:create', 'Create orders'),
    ('order:read', 'Read orders'),
    ('order:cancel', 'Cancel orders'),
    ('position:read', 'Read positions'),
    ('risk:read', 'Read risk data'),
    ('risk:write', 'Modify risk rules'),
    ('admin:write', 'Admin access')
ON CONFLICT (permission_name) DO NOTHING;

-- Admin user (password: admin123 - CHANGE IN PRODUCTION!)
INSERT INTO users (user_id, username, email, password_hash, status, user_type)
VALUES (
    'admin-001',
    'admin',
    'admin@qts.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ACTIVE',
    'ADMIN'
)
ON CONFLICT (user_id) DO NOTHING;

-- Grant admin role
INSERT INTO user_roles (user_id, role_id)
SELECT 'admin-001', id FROM roles WHERE role_name = 'ADMIN'
ON CONFLICT DO NOTHING;

\c qts_trade;

-- Verification query
DO $$
BEGIN
    RAISE NOTICE '=== QTS Database Initialization Complete ===';
    RAISE NOTICE 'Databases: qts_trade, qts_auth, qts_risk';
    RAISE NOTICE 'Default admin user: admin@qts.local / admin123 (CHANGE IN PRODUCTION!)';
END $$;