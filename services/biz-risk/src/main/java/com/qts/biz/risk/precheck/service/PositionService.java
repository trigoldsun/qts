package com.qts.biz.risk.precheck.service;

import java.math.BigDecimal;

/**
 * Position Service Interface
 * Provides position data for risk validation
 */
public interface PositionService {

    /**
     * Get current position quantity for a symbol
     * @param accountId Account ID
     * @param symbol Stock symbol
     * @return Position quantity (0 if no position)
     */
    BigDecimal getPosition(Long accountId, String symbol);

    /**
     * Get all positions for an account
     * @param accountId Account ID
     * @return Map of symbol to position quantity
     */
    java.util.Map<String, BigDecimal> getAllPositions(Long accountId);
}