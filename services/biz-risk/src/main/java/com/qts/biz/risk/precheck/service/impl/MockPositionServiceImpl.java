package com.qts.biz.risk.precheck.service.impl;

import com.qts.biz.risk.precheck.service.PositionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Mock implementation of PositionService
 * In production, this would call external position service/database
 */
@Service
public class MockPositionServiceImpl implements PositionService {

    private static final Logger logger = LoggerFactory.getLogger(MockPositionServiceImpl.class);

    // Mock data storage: accountId -> (symbol -> quantity)
    private final Map<Long, Map<String, BigDecimal>> positionData = new HashMap<>();

    public MockPositionServiceImpl() {
        // Initialize with empty positions
        logger.info("MockPositionService initialized");
    }

    @Override
    public BigDecimal getPosition(Long accountId, String symbol) {
        logger.debug("Getting position for accountId={}, symbol={}", accountId, symbol);
        Map<String, BigDecimal> accountPositions = positionData.get(accountId);
        if (accountPositions == null) {
            return BigDecimal.ZERO;
        }
        return accountPositions.getOrDefault(symbol, BigDecimal.ZERO);
    }

    @Override
    public Map<String, BigDecimal> getAllPositions(Long accountId) {
        logger.debug("Getting all positions for accountId={}", accountId);
        return new HashMap<>(positionData.getOrDefault(accountId, Map.of()));
    }

    /**
     * Update position (for testing purposes)
     */
    public void updatePosition(Long accountId, String symbol, BigDecimal quantity) {
        positionData.computeIfAbsent(accountId, k -> new HashMap<>()).put(symbol, quantity);
    }
}