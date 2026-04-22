package com.qts.biz.trade.service;

import com.qts.biz.trade.aggregate.PositionAggregate;
import com.qts.biz.trade.dto.PagedResult;
import com.qts.biz.trade.dto.PositionDTO;
import com.qts.biz.trade.dto.PositionQuery;
import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.entity.PositionEntity;
import com.qts.biz.trade.repository.PositionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Position management service
 * Handles position queries, freezing, and trade execution updates
 */
@Service
public class PositionManager {

    private static final Logger logger = LoggerFactory.getLogger(PositionManager.class);

    private final PositionRepository positionRepository;

    @Autowired
    public PositionManager(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /**
     * Get single position by account and symbol
     */
    public PositionDTO getPosition(Long accountId, String symbol) {
        validateAccountId(accountId);
        validateSymbol(symbol);

        logger.debug("Getting position for accountId={}, symbol={}", accountId, symbol);

        Optional<PositionEntity> entityOpt = positionRepository.findByAccountIdAndSymbol(accountId, symbol);
        if (entityOpt.isEmpty()) {
            return null;
        }

        return toDTO(entityOpt.get());
    }

    /**
     * List positions for an account with optional query filter
     */
    public PagedResult<PositionDTO> listPositions(Long accountId, PositionQuery query, int page, int pageSize) {
        validateAccountId(accountId);

        logger.debug("Listing positions for accountId={}, query={}, page={}, pageSize={}", 
                    accountId, query, page, pageSize);

        List<PositionEntity> entities = positionRepository.findByAccountId(accountId, query);

        long totalItems = entities.size();
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalItems / pageSize) : 0;

        // Apply pagination
        int fromIndex = (page - 1) * pageSize;
        if (fromIndex >= entities.size()) {
            return new PagedResult<>(List.of(), page, pageSize, totalItems);
        }

        int toIndex = Math.min(fromIndex + pageSize, entities.size());
        List<PositionDTO> dtos = entities.subList(fromIndex, toIndex)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

        return new PagedResult<>(dtos, page, pageSize, totalItems);
    }

    /**
     * List positions without pagination (default page=1, pageSize=100)
     */
    public PagedResult<PositionDTO> listPositions(Long accountId, PositionQuery query) {
        return listPositions(accountId, query, 1, 100);
    }

    /**
     * Freeze position quantity (for order placement)
     */
    public void freezePosition(Long accountId, String symbol, BigDecimal qty) {
        validateAccountId(accountId);
        validateSymbol(symbol);
        validateQuantity(qty, "freeze");

        logger.info("Freezing position: accountId={}, symbol={}, qty={}", accountId, symbol, qty);

        PositionAggregate aggregate = getOrCreateAggregate(accountId, symbol);
        
        try {
            aggregate.freeze(qty);
            saveAggregate(aggregate);
            logger.info("Position frozen successfully: accountId={}, symbol={}, qty={}", accountId, symbol, qty);
        } catch (IllegalStateException e) {
            logger.warn("Failed to freeze position: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Unfreeze position quantity (for order cancellation)
     */
    public void unfreezePosition(Long accountId, String symbol, BigDecimal qty) {
        validateAccountId(accountId);
        validateSymbol(symbol);
        validateQuantity(qty, "unfreeze");

        logger.info("Unfreezing position: accountId={}, symbol={}, qty={}", accountId, symbol, qty);

        PositionAggregate aggregate = getAggregateOrThrow(accountId, symbol);
        
        try {
            aggregate.unfreeze(qty);
            saveAggregate(aggregate);
            logger.info("Position unfrozen successfully: accountId={}, symbol={}, qty={}", accountId, symbol, qty);
        } catch (IllegalStateException e) {
            logger.warn("Failed to unfreeze position: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Add position (on buy trade execution)
     */
    public void addPosition(TradeDTO trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }

        logger.info("Adding position: accountId={}, symbol={}, side={}, qty={}, price={}", 
                   trade.getAccountId(), trade.getSymbol(), trade.getSide(), trade.getQuantity(), trade.getPrice());

        if (!trade.isBuy()) {
            logger.warn("addPosition called with non-BUY trade, ignoring");
            return;
        }

        PositionAggregate aggregate = getOrCreateAggregate(trade.getAccountId(), trade.getSymbol());
        aggregate.applyTrade(trade);
        saveAggregate(aggregate);

        logger.info("Position added successfully for tradeId={}", trade.getTradeId());
    }

    /**
     * Reduce position (on sell trade execution)
     */
    public void reducePosition(TradeDTO trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }

        logger.info("Reducing position: accountId={}, symbol={}, side={}, qty={}, price={}", 
                   trade.getAccountId(), trade.getSymbol(), trade.getSide(), trade.getQuantity(), trade.getPrice());

        if (!trade.isSell()) {
            logger.warn("reducePosition called with non-SELL trade, ignoring");
            return;
        }

        PositionAggregate aggregate = getAggregateOrThrow(trade.getAccountId(), trade.getSymbol());
        
        try {
            aggregate.applyTrade(trade);
            saveAggregate(aggregate);
            logger.info("Position reduced successfully for tradeId={}", trade.getTradeId());
        } catch (IllegalStateException e) {
            logger.error("Failed to reduce position: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Update market price and recalculate values
     */
    public void updateMarketPrice(Long accountId, String symbol, BigDecimal marketPrice) {
        if (accountId == null || symbol == null || marketPrice == null) {
            throw new IllegalArgumentException("accountId, symbol, and marketPrice are required");
        }

        Optional<PositionEntity> entityOpt = positionRepository.findByAccountIdAndSymbol(accountId, symbol);
        if (entityOpt.isEmpty()) {
            return; // No position to update
        }

        PositionAggregate aggregate = toAggregate(entityOpt.get());
        aggregate.recalculate(marketPrice);
        saveAggregate(aggregate);
    }

    // Private helper methods

    private PositionAggregate getOrCreateAggregate(Long accountId, String symbol) {
        Optional<PositionEntity> entityOpt = positionRepository.findByAccountIdAndSymbol(accountId, symbol);
        
        if (entityOpt.isPresent()) {
            return toAggregate(entityOpt.get());
        } else {
            PositionAggregate aggregate = new PositionAggregate(accountId, symbol, symbol);
            return aggregate;
        }
    }

    private PositionAggregate getAggregateOrThrow(Long accountId, String symbol) {
        Optional<PositionEntity> entityOpt = positionRepository.findByAccountIdAndSymbol(accountId, symbol);
        if (entityOpt.isEmpty()) {
            throw new IllegalStateException("Position not found for accountId=" + accountId + ", symbol=" + symbol);
        }
        return toAggregate(entityOpt.get());
    }

    private PositionAggregate toAggregate(PositionEntity entity) {
        PositionAggregate aggregate = new PositionAggregate(
                entity.getAccountId(), 
                entity.getSymbol(), 
                entity.getSymbolName() != null ? entity.getSymbolName() : entity.getSymbol()
        );
        
        aggregate.setId(entity.getId());
        aggregate.setQuantity(entity.getQuantity() != null ? entity.getQuantity() : BigDecimal.ZERO);
        aggregate.setFrozenQuantity(entity.getFrozenQuantity() != null ? entity.getFrozenQuantity() : BigDecimal.ZERO);
        aggregate.setCostPrice(entity.getCostPrice() != null ? entity.getCostPrice() : BigDecimal.ZERO);
        aggregate.setMarketPrice(entity.getMarketPrice() != null ? entity.getMarketPrice() : BigDecimal.ZERO);
        aggregate.setTodayBuyQuantity(entity.getTodayBuyQuantity() != null ? entity.getTodayBuyQuantity() : BigDecimal.ZERO);
        aggregate.setTodaySellQuantity(entity.getTodaySellQuantity() != null ? entity.getTodaySellQuantity() : BigDecimal.ZERO);
        aggregate.setTodayBuyAmount(entity.getTodayBuyAmount() != null ? entity.getTodayBuyAmount() : BigDecimal.ZERO);
        aggregate.setTodaySellAmount(entity.getTodaySellAmount() != null ? entity.getTodaySellAmount() : BigDecimal.ZERO);
        aggregate.setCreateTime(entity.getCreateTime() != null ? entity.getCreateTime() : LocalDateTime.now());
        aggregate.setUpdateTime(entity.getUpdateTime() != null ? entity.getUpdateTime() : LocalDateTime.now());
        
        return aggregate;
    }

    private void saveAggregate(PositionAggregate aggregate) {
        PositionEntity entity = toEntity(aggregate);
        positionRepository.save(entity);
    }

    private PositionEntity toEntity(PositionAggregate aggregate) {
        PositionEntity entity = new PositionEntity();
        entity.setId(aggregate.getId());
        entity.setAccountId(aggregate.getAccountId());
        entity.setSymbol(aggregate.getSymbol());
        entity.setSymbolName(aggregate.getSymbolName());
        entity.setQuantity(aggregate.getQuantity());
        entity.setAvailableQuantity(aggregate.getAvailableQuantity());
        entity.setFrozenQuantity(aggregate.getFrozenQuantity());
        entity.setCostPrice(aggregate.getCostPrice());
        entity.setMarketPrice(aggregate.getMarketPrice());
        entity.setTodayBuyQuantity(aggregate.getTodayBuyQuantity());
        entity.setTodaySellQuantity(aggregate.getTodaySellQuantity());
        entity.setTodayBuyAmount(aggregate.getTodayBuyAmount());
        entity.setTodaySellAmount(aggregate.getTodaySellAmount());
        entity.setCreateTime(aggregate.getCreateTime());
        entity.setUpdateTime(aggregate.getUpdateTime());
        return entity;
    }

    private PositionDTO toDTO(PositionEntity entity) {
        BigDecimal quantity = entity.getQuantity() != null ? entity.getQuantity() : BigDecimal.ZERO;
        BigDecimal frozenQty = entity.getFrozenQuantity() != null ? entity.getFrozenQuantity() : BigDecimal.ZERO;
        BigDecimal costPrice = entity.getCostPrice() != null ? entity.getCostPrice() : BigDecimal.ZERO;
        BigDecimal marketPrice = entity.getMarketPrice() != null ? entity.getMarketPrice() : BigDecimal.ZERO;

        BigDecimal availableQuantity = quantity.subtract(frozenQty);
        BigDecimal marketValue = quantity.multiply(marketPrice);
        BigDecimal profitLoss = marketPrice.subtract(costPrice).multiply(quantity);
        BigDecimal profitLossRatio = BigDecimal.ZERO;
        if (costPrice.compareTo(BigDecimal.ZERO) > 0) {
            profitLossRatio = marketPrice.subtract(costPrice).divide(costPrice, 4, RoundingMode.HALF_UP);
        }

        return PositionDTO.builder()
                .symbol(entity.getSymbol())
                .symbolName(entity.getSymbolName())
                .quantity(quantity)
                .availableQuantity(availableQuantity)
                .frozenQuantity(frozenQty)
                .costPrice(costPrice)
                .marketPrice(marketPrice)
                .marketValue(marketValue)
                .profitLoss(profitLoss)
                .profitLossRatio(profitLossRatio)
                .todayBuyQuantity(entity.getTodayBuyQuantity() != null ? entity.getTodayBuyQuantity() : BigDecimal.ZERO)
                .todaySellQuantity(entity.getTodaySellQuantity() != null ? entity.getTodaySellQuantity() : BigDecimal.ZERO)
                .todayBuyAmount(entity.getTodayBuyAmount() != null ? entity.getTodayBuyAmount() : BigDecimal.ZERO)
                .todaySellAmount(entity.getTodaySellAmount() != null ? entity.getTodaySellAmount() : BigDecimal.ZERO)
                .build();
    }

    // Validation helpers

    private void validateAccountId(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("AccountId cannot be null");
        }
    }

    private void validateSymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
    }

    private void validateQuantity(BigDecimal qty, String operation) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive for " + operation);
        }
    }
}