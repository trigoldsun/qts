package com.qts.biz.trade.dto;

import java.math.BigDecimal;

/**
 * Position DTO for API responses
 */
public class PositionDTO {
    
    private String symbol;
    private String symbolName;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal frozenQuantity;
    private BigDecimal costPrice;
    private BigDecimal marketPrice;
    private BigDecimal marketValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossRatio;
    private BigDecimal todayBuyQuantity;
    private BigDecimal todaySellQuantity;
    private BigDecimal todayBuyAmount;
    private BigDecimal todaySellAmount;

    public PositionDTO() {}

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public void setSymbolName(String symbolName) {
        this.symbolName = symbolName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(BigDecimal availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public BigDecimal getFrozenQuantity() {
        return frozenQuantity;
    }

    public void setFrozenQuantity(BigDecimal frozenQuantity) {
        this.frozenQuantity = frozenQuantity;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public BigDecimal getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(BigDecimal marketPrice) {
        this.marketPrice = marketPrice;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
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

    public BigDecimal getTodayBuyQuantity() {
        return todayBuyQuantity;
    }

    public void setTodayBuyQuantity(BigDecimal todayBuyQuantity) {
        this.todayBuyQuantity = todayBuyQuantity;
    }

    public BigDecimal getTodaySellQuantity() {
        return todaySellQuantity;
    }

    public void setTodaySellQuantity(BigDecimal todaySellQuantity) {
        this.todaySellQuantity = todaySellQuantity;
    }

    public BigDecimal getTodayBuyAmount() {
        return todayBuyAmount;
    }

    public void setTodayBuyAmount(BigDecimal todayBuyAmount) {
        this.todayBuyAmount = todayBuyAmount;
    }

    public BigDecimal getTodaySellAmount() {
        return todaySellAmount;
    }

    public void setTodaySellAmount(BigDecimal todaySellAmount) {
        this.todaySellAmount = todaySellAmount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PositionDTO dto = new PositionDTO();

        public Builder symbol(String symbol) { dto.symbol = symbol; return this; }
        public Builder symbolName(String symbolName) { dto.symbolName = symbolName; return this; }
        public Builder quantity(BigDecimal quantity) { dto.quantity = quantity; return this; }
        public Builder availableQuantity(BigDecimal availableQuantity) { dto.availableQuantity = availableQuantity; return this; }
        public Builder frozenQuantity(BigDecimal frozenQuantity) { dto.frozenQuantity = frozenQuantity; return this; }
        public Builder costPrice(BigDecimal costPrice) { dto.costPrice = costPrice; return this; }
        public Builder marketPrice(BigDecimal marketPrice) { dto.marketPrice = marketPrice; return this; }
        public Builder marketValue(BigDecimal marketValue) { dto.marketValue = marketValue; return this; }
        public Builder profitLoss(BigDecimal profitLoss) { dto.profitLoss = profitLoss; return this; }
        public Builder profitLossRatio(BigDecimal profitLossRatio) { dto.profitLossRatio = profitLossRatio; return this; }
        public Builder todayBuyQuantity(BigDecimal todayBuyQuantity) { dto.todayBuyQuantity = todayBuyQuantity; return this; }
        public Builder todaySellQuantity(BigDecimal todaySellQuantity) { dto.todaySellQuantity = todaySellQuantity; return this; }
        public Builder todayBuyAmount(BigDecimal todayBuyAmount) { dto.todayBuyAmount = todayBuyAmount; return this; }
        public Builder todaySellAmount(BigDecimal todaySellAmount) { dto.todaySellAmount = todaySellAmount; return this; }

        public PositionDTO build() {
            return dto;
        }
    }
}