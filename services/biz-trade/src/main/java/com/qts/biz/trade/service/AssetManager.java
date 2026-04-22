package com.qts.biz.trade.service;

import com.qts.biz.trade.aggregate.AssetAggregate;
import com.qts.biz.trade.dto.AssetDTO;
import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.entity.AssetEntity;
import com.qts.biz.trade.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Asset Manager Service
 * Handles fund management operations including freeze, unfreeze, and trade updates
 */
@Service
public class AssetManager {

    private static final Logger logger = LoggerFactory.getLogger(AssetManager.class);

    private final AssetRepository assetRepository;

    @Autowired
    public AssetManager(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * Get asset information for an account
     * @param accountId Account ID
     * @return AssetDTO with current asset state
     * @throws IllegalArgumentException if account not found
     */
    @Transactional(readOnly = true)
    public AssetDTO getAsset(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        logger.info("Getting asset for accountId={}", accountId);

        AssetEntity entity = assetRepository.findByAccountId(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        return convertToDTO(entity);
    }

    /**
     * Freeze cash for order placement
     * @param accountId Account ID
     * @param amount Amount to freeze
     * @throws IllegalArgumentException if insufficient cash or account not found
     */
    @Transactional
    public void freezeCash(Long accountId, BigDecimal amount) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        logger.info("Freezing cash for accountId={}, amount={}", accountId, amount);

        AssetEntity entity = assetRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        AssetAggregate aggregate = new AssetAggregate(entity);
        aggregate.freeze(amount);

        assetRepository.save(entity);
        logger.info("Successfully froze cash for accountId={}", accountId);
    }

    /**
     * Unfreeze cash when order is cancelled
     * @param accountId Account ID
     * @param amount Amount to unfreeze
     * @throws IllegalArgumentException if insufficient frozen cash or account not found
     */
    @Transactional
    public void unfreezeCash(Long accountId, BigDecimal amount) {
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        logger.info("Unfreezing cash for accountId={}, amount={}", accountId, amount);

        AssetEntity entity = assetRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        AssetAggregate aggregate = new AssetAggregate(entity);
        aggregate.unfreeze(amount);

        assetRepository.save(entity);
        logger.info("Successfully unfroze cash for accountId={}", accountId);
    }

    /**
     * Update asset from trade execution
     * @param trade Trade execution event
     * @throws IllegalArgumentException if trade is null or account not found
     */
    @Transactional
    public void updateAssetFromTrade(TradeDTO trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }

        Long accountId = trade.getAccountId();
        logger.info("Updating asset from trade for accountId={}, tradeId={}", accountId, trade.getTradeId());

        AssetEntity entity = assetRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        AssetAggregate aggregate = new AssetAggregate(entity);
        aggregate.applyTrade(trade);

        assetRepository.save(entity);
        logger.info("Successfully updated asset from trade for accountId={}", accountId);
    }

    /**
     * Convert entity to DTO
     * @param entity Asset entity
     * @return AssetDTO
     */
    private AssetDTO convertToDTO(AssetEntity entity) {
        AssetDTO dto = new AssetDTO();
        dto.setAccountId(entity.getAccountId());
        dto.setCurrency(entity.getCurrency());
        dto.setTotalAssets(entity.getTotalAssets());
        dto.setAvailableCash(entity.getAvailableCash());
        dto.setFrozenCash(entity.getFrozenCash());
        dto.setMarketValue(entity.getMarketValue());
        dto.setTotalProfitLoss(entity.getTotalProfitLoss());
        dto.setTodayProfitLoss(entity.getTodayProfitLoss());
        dto.setMargin(entity.getMargin());
        dto.setMaintenanceMargin(entity.getMaintenanceMargin());
        dto.setRiskLevel(AssetDTO.RiskLevel.valueOf(entity.getRiskLevel()));
        return dto;
    }
}
