package com.qts.biz.trade.repository;

import com.qts.biz.trade.dto.PositionQuery;
import com.qts.biz.trade.entity.PositionEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Position persistence
 */
public interface PositionRepository {

    /**
     * Find position by account and symbol
     */
    Optional<PositionEntity> findByAccountIdAndSymbol(Long accountId, String symbol);

    /**
     * Find all positions for an account with optional query filter
     */
    List<PositionEntity> findByAccountId(Long accountId, PositionQuery query);

    /**
     * Find all positions for an account (all symbols)
     */
    List<PositionEntity> findByAccountId(Long accountId);

    /**
     * Save or update position
     */
    PositionEntity save(PositionEntity position);

    /**
     * Delete position
     */
    void delete(Long accountId, String symbol);

    /**
     * Check if position exists
     */
    boolean exists(Long accountId, String symbol);
}