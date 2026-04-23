package com.qts.biz.risk.precheck.service.impl;

import com.qts.biz.risk.precheck.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of MarketDataService
 * In production, this would call external market data service (e.g., from exchange)
 */
@Service
public class MockMarketDataServiceImpl implements MarketDataService {

    private static final Logger logger = LoggerFactory.getLogger(MockMarketDataServiceImpl.class);

    // Mock data storage: symbol -> previous close price
    private final Map<String, BigDecimal> priceData = new HashMap<>();

    public MockMarketDataServiceImpl() {
        // Initialize with some default prices
        logger.info("MockMarketDataService initialized");
    }

    @Override
    public BigDecimal getPreviousClose(String symbol) {
        logger.debug("Getting previous close for symbol={}", symbol);
        return priceData.get(symbol);
    }

    /**
     * Update price data (for testing purposes)
     */
    public void updatePrice(String symbol, BigDecimal price) {
        priceData.put(symbol, price);
    }
}