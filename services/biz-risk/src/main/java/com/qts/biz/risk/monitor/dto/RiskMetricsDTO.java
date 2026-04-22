package com.qts.biz.risk.monitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Risk Metrics DTO
 * Real-time risk monitoring metrics
 */
public class RiskMetricsDTO {

    private String accountId;
    private LocalDateTime timestamp;
    
    // Position metrics
    private List<PositionMetric> positions;
    private BigDecimal totalPositionValue;
    private Integer totalPositionCount;
    
    // Capital metrics
    private BigDecimal totalAssets;
    private BigDecimal occupiedCapital; // occupied = margin + frozen
    private BigDecimal availableCapital;
    private BigDecimal cashOccupied;
    private BigDecimal marginOccupied;
    
    // Margin ratio metrics
    private BigDecimal marginRatio; // margin / total assets %
    private BigDecimal maintenanceMarginRatio; // maintenance margin / total assets %
    private BigDecimal warningMarginRatioThreshold;
    private BigDecimal dangerMarginRatioThreshold;
    
    // Risk level
    private String riskLevel; // NORMAL, WARNING, DANGER
    
    // Leverage
    private BigDecimal leverage;

    public RiskMetricsDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public List<PositionMetric> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionMetric> positions) {
        this.positions = positions;
    }

    public BigDecimal getTotalPositionValue() {
        return totalPositionValue;
    }

    public void setTotalPositionValue(BigDecimal totalPositionValue) {
        this.totalPositionValue = totalPositionValue;
    }

    public Integer getTotalPositionCount() {
        return totalPositionCount;
    }

    public void setTotalPositionCount(Integer totalPositionCount) {
        this.totalPositionCount = totalPositionCount;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getOccupiedCapital() {
        return occupiedCapital;
    }

    public void setOccupiedCapital(BigDecimal occupiedCapital) {
        this.occupiedCapital = occupiedCapital;
    }

    public BigDecimal getAvailableCapital() {
        return availableCapital;
    }

    public void setAvailableCapital(BigDecimal availableCapital) {
        this.availableCapital = availableCapital;
    }

    public BigDecimal getCashOccupied() {
        return cashOccupied;
    }

    public void setCashOccupied(BigDecimal cashOccupied) {
        this.cashOccupied = cashOccupied;
    }

    public BigDecimal getMarginOccupied() {
        return marginOccupied;
    }

    public void setMarginOccupied(BigDecimal marginOccupied) {
        this.marginOccupied = marginOccupied;
    }

    public BigDecimal getMarginRatio() {
        return marginRatio;
    }

    public void setMarginRatio(BigDecimal marginRatio) {
        this.marginRatio = marginRatio;
    }

    public BigDecimal getMaintenanceMarginRatio() {
        return maintenanceMarginRatio;
    }

    public void setMaintenanceMarginRatio(BigDecimal maintenanceMarginRatio) {
        this.maintenanceMarginRatio = maintenanceMarginRatio;
    }

    public BigDecimal getWarningMarginRatioThreshold() {
        return warningMarginRatioThreshold;
    }

    public void setWarningMarginRatioThreshold(BigDecimal warningMarginRatioThreshold) {
        this.warningMarginRatioThreshold = warningMarginRatioThreshold;
    }

    public BigDecimal getDangerMarginRatioThreshold() {
        return dangerMarginRatioThreshold;
    }

    public void setDangerMarginRatioThreshold(BigDecimal dangerMarginRatioThreshold) {
        this.dangerMarginRatioThreshold = dangerMarginRatioThreshold;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    /**
     * Position metric for a single symbol
     */
    public static class PositionMetric {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal marketValue;
        private BigDecimal costPrice;
        private BigDecimal profitLoss;
        private BigDecimal profitLossRatio;

        public PositionMetric() {}

        public String getSymbol() {
            return symbol;
        }

        public void setSymbol(String symbol) {
            this.symbol = symbol;
        }

        public BigDecimal getQuantity() {
            return quantity;
        }

        public void setQuantity(BigDecimal quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getMarketValue() {
            return marketValue;
        }

        public void setMarketValue(BigDecimal marketValue) {
            this.marketValue = marketValue;
        }

        public BigDecimal getCostPrice() {
            return costPrice;
        }

        public void setCostPrice(BigDecimal costPrice) {
            this.costPrice = costPrice;
        }

        public BigDecimal getProfitLoss() {
            return profitLoss;
        }

        public void setProfitLoss(BigDecimal profitLoss) {
            this.profitLoss = profitLoss;
        }

        public BigDecimal getProfitLossRatio() {
            return profitLossRatio;
        }

        public void setProfitLossRatio(BigDecimal profitLossRatio) {
            this.profitLossRatio = profitLossRatio;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final PositionMetric m = new PositionMetric();

            public Builder symbol(String val) { m.symbol = val; return this; }
            public Builder quantity(BigDecimal val) { m.quantity = val; return this; }
            public Builder marketValue(BigDecimal val) { m.marketValue = val; return this; }
            public Builder costPrice(BigDecimal val) { m.costPrice = val; return this; }
            public Builder profitLoss(BigDecimal val) { m.profitLoss = val; return this; }
            public Builder profitLossRatio(BigDecimal val) { m.profitLossRatio = val; return this; }

            public PositionMetric build() {
                return m;
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RiskMetricsDTO dto = new RiskMetricsDTO();

        public Builder accountId(String val) { dto.accountId = val; return this; }
        public Builder positions(List<PositionMetric> val) { dto.positions = val; return this; }
        public Builder totalPositionValue(BigDecimal val) { dto.totalPositionValue = val; return this; }
        public Builder totalPositionCount(Integer val) { dto.totalPositionCount = val; return this; }
        public Builder totalAssets(BigDecimal val) { dto.totalAssets = val; return this; }
        public Builder occupiedCapital(BigDecimal val) { dto.occupiedCapital = val; return this; }
        public Builder availableCapital(BigDecimal val) { dto.availableCapital = val; return this; }
        public Builder cashOccupied(BigDecimal val) { dto.cashOccupied = val; return this; }
        public Builder marginOccupied(BigDecimal val) { dto.marginOccupied = val; return this; }
        public Builder marginRatio(BigDecimal val) { dto.marginRatio = val; return this; }
        public Builder maintenanceMarginRatio(BigDecimal val) { dto.maintenanceMarginRatio = val; return this; }
        public Builder warningMarginRatioThreshold(BigDecimal val) { dto.warningMarginRatioThreshold = val; return this; }
        public Builder dangerMarginRatioThreshold(BigDecimal val) { dto.dangerMarginRatioThreshold = val; return this; }
        public Builder riskLevel(String val) { dto.riskLevel = val; return this; }
        public Builder leverage(BigDecimal val) { dto.leverage = val; return this; }

        public RiskMetricsDTO build() {
            return dto;
        }
    }
}
