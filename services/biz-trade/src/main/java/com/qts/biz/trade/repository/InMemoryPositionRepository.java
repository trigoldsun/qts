package com.qts.biz.trade.repository;

import com.qts.biz.trade.dto.PositionQuery;
import com.qts.biz.trade.entity.PositionEntity;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * In-memory implementation of PositionRepository
 * Production implementation would use PostgreSQL with JPA
 */
@Repository
public class InMemoryPositionRepository implements PositionRepository {

    // Key: accountId + ":" + symbol
    private final Map<String, PositionEntity> positions = new ConcurrentHashMap<>();

    @Override
    public Optional<PositionEntity> findByAccountIdAndSymbol(Long accountId, String symbol) {
        return Optional.ofNullable(positions.get(cacheKey(accountId, symbol)));
    }

    @Override
    public List<PositionEntity> findByAccountId(Long accountId, PositionQuery query) {
        Predicate<PositionEntity> predicate = p -> {
            if (!p.getAccountId().equals(accountId)) return false;
            if (query != null && query.getSymbol() != null && !query.getSymbol().isEmpty()) {
                if (!p.getSymbol().contains(query.getSymbol())) return false;
            }
            if (query != null && query.getSymbolName() != null && !query.getSymbolName().isEmpty()) {
                if (!p.getSymbolName().contains(query.getSymbolName())) return false;
            }
            return true;
        };
        
        return positions.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    @Override
    public List<PositionEntity> findByAccountId(Long accountId) {
        return findByAccountId(accountId, null);
    }

    @Override
    public PositionEntity save(PositionEntity position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (position.getAccountId() == null || position.getSymbol() == null) {
            throw new IllegalArgumentException("AccountId and Symbol are required");
        }
        
        PositionEntity existing = positions.get(cacheKey(position.getAccountId(), position.getSymbol()));
        if (existing != null) {
            position.setId(existing.getId());
            position.setCreateTime(existing.getCreateTime());
        } else {
            position.setId(System.currentTimeMillis());
            position.setCreateTime(LocalDateTime.now());
        }
        position.setUpdateTime(LocalDateTime.now());
        
        positions.put(cacheKey(position.getAccountId(), position.getSymbol()), position);
        return position;
    }

    @Override
    public void delete(Long accountId, String symbol) {
        positions.remove(cacheKey(accountId, symbol));
    }

    @Override
    public boolean exists(Long accountId, String symbol) {
        return positions.containsKey(cacheKey(accountId, symbol));
    }

    private String cacheKey(Long accountId, String symbol) {
        return accountId + ":" + symbol;
    }

    // Helper method for testing
    public void clear() {
        positions.clear();
    }
}