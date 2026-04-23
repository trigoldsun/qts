package com.qts.biz.risk.precheck.service;

import java.math.BigDecimal;

/**
 * Market Data Service Interface
 * Provides market data (prices) for risk validation
 */
public interface MarketDataService {

    /**
     * Get previous close price for a symbol
     * @param symbol Stock symbol
     * @return Previous close price, null if not available
     */
    BigDecimal getPreviousClose(String symbol);
}