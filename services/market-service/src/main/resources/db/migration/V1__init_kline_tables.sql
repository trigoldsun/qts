-- K线数据表
-- 支持1分钟/5分钟/1小时/日K线
CREATE TABLE IF NOT EXISTS kline (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL COMMENT '标的代码，如 BTC-USDT, SH600000',
    period VARCHAR(4) NOT NULL COMMENT 'K线周期：1m, 5m, 1h, 1d',
    timestamp TIMESTAMP NOT NULL COMMENT 'K线开盘时间',
    open_price DECIMAL(18, 8) NOT NULL COMMENT '开盘价',
    high_price DECIMAL(18, 8) NOT NULL COMMENT '最高价',
    low_price DECIMAL(18, 8) NOT NULL COMMENT '最低价',
    close_price DECIMAL(18, 8) NOT NULL COMMENT '收盘价',
    volume BIGINT NOT NULL COMMENT '成交量',
    amount DECIMAL(18, 4) NOT NULL COMMENT '成交额',
    adjustment VARCHAR(16) DEFAULT 'NONE' COMMENT '复权类型：FORWARD, BACKWARD, NONE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_kline_symbol_period_time ON kline(symbol, period, timestamp DESC);
CREATE INDEX idx_kline_timestamp ON kline(timestamp DESC);

-- 标的信息表
CREATE TABLE IF NOT EXISTS symbol_info (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL UNIQUE COMMENT '标的代码',
    symbol_name VARCHAR(64) COMMENT '标的名称',
    exchange VARCHAR(16) NOT NULL COMMENT '交易所：SH, SZ, BINANCE, OKX',
    listing_date DATE COMMENT '上市日期',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE, SUSPENDED, DELISTED',
    symbol_type VARCHAR(16) DEFAULT 'STOCK' COMMENT '标的类型：STOCK, CRYPTO, FUTURES',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_symbol_exchange ON symbol_info(exchange);
CREATE INDEX idx_symbol_status ON symbol_info(status);

-- 注释
COMMENT ON TABLE kline IS 'K线数据表';
COMMENT ON COLUMN kline.symbol IS '标的代码';
COMMENT ON COLUMN kline.period IS 'K线周期：1m=1分钟, 5m=5分钟, 1h=1小时, 1d=日K';
COMMENT ON COLUMN kline.adjustment IS '复权类型：FORWARD=前复权, BACKWARD=后复权, NONE=不复权';
