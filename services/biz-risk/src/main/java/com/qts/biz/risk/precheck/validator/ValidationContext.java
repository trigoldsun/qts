package com.qts.biz.risk.precheck.validator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Default implementation of ValidationContext
 * Retrieves data from external services/repositories
 */
public class ValidationContext implements PreCheckValidator.ValidationContext {

    private final Long accountId;
    private final Map<String, BigDecimal> positionMap;   // symbol -> quantity
    private final Map<String, BigDecimal> priceMap;        // symbol -> previous close price
    private final BigDecimal totalAssets;
    private final BigDecimal availableCash;
    private final LocalDateTime currentTime;

    public ValidationContext(Long accountId, 
                             Map<String, BigDecimal> positionMap,
                             Map<String, BigDecimal> priceMap,
                             BigDecimal totalAssets,
                             BigDecimal availableCash,
                             LocalDateTime currentTime) {
        this.accountId = accountId;
        this.positionMap = positionMap;
        this.priceMap = priceMap;
        this.totalAssets = totalAssets;
        this.availableCash = availableCash;
        this.currentTime = currentTime;
    }

    @Override
    public BigDecimal getCurrentPosition(String symbol) {
        return positionMap.getOrDefault(symbol, BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    @Override
    public BigDecimal getAvailableCash() {
        return availableCash;
    }

    @Override
    public BigDecimal getPreviousClose(String symbol) {
        return priceMap.get(symbol);
    }

    @Override
    public LocalDateTime getCurrentTime() {
        return currentTime != null ? currentTime : LocalDateTime.now();
    }

    public Long getAccountId() {
        return accountId;
    }

    /**
     * Builder for ValidationContext
     */
    public static class Builder {
        private Long accountId;
        private Map<String, BigDecimal> positionMap = Map.of();
        private Map<String, BigDecimal> priceMap = Map.of();
        private BigDecimal totalAssets = BigDecimal.ZERO;
        private BigDecimal availableCash = BigDecimal.ZERO;
        private LocalDateTime currentTime;

        public Builder accountId(Long accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder positionMap(Map<String, BigDecimal> positionMap) {
            this.positionMap = positionMap;
            return this;
        }

        public Builder priceMap(Map<String, BigDecimal> priceMap) {
            this.priceMap = priceMap;
            return this;
        }

        public Builder totalAssets(BigDecimal totalAssets) {
            this.totalAssets = totalAssets;
            return this;
        }

        public Builder availableCash(BigDecimal availableCash) {
            this.availableCash = availableCash;
            return this;
        }

        public Builder currentTime(LocalDateTime currentTime) {
            this.currentTime = currentTime;
            return this;
        }

        public ValidationContext build() {
            return new ValidationContext(accountId, positionMap, priceMap, 
                                        totalAssets, availableCash, currentTime);
        }
    }
}