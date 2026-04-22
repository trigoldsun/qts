package com.qts.biz.risk.engine.model.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Risk Check Request DTO
 * Input data for risk rule evaluation
 */
public class RiskCheckRequest {

    private String accountId;
    private String symbol;
    private String side; // BUY or SELL
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal orderAmount; // Computed: price * quantity

    // Account context data for rule evaluation
    private Map<String, Object> accountContext;

    // Current positions for this account
    private Map<String, BigDecimal> positions; // symbol -> position value

    // Daily trading stats
    private BigDecimal dailyBuyAmount;
    private BigDecimal dailySellAmount;
    private BigDecimal dailyProfitLoss;
    private BigDecimal totalAssetValue; // Total portfolio value

    public RiskCheckRequest() {}

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public Map<String, Object> getAccountContext() {
        return accountContext;
    }

    public void setAccountContext(Map<String, Object> accountContext) {
        this.accountContext = accountContext;
    }

    public Map<String, BigDecimal> getPositions() {
        return positions;
    }

    public void setPositions(Map<String, BigDecimal> positions) {
        this.positions = positions;
    }

    public BigDecimal getDailyBuyAmount() {
        return dailyBuyAmount;
    }

    public void setDailyBuyAmount(BigDecimal dailyBuyAmount) {
        this.dailyBuyAmount = dailyBuyAmount;
    }

    public BigDecimal getDailySellAmount() {
        return dailySellAmount;
    }

    public void setDailySellAmount(BigDecimal dailySellAmount) {
        this.dailySellAmount = dailySellAmount;
    }

    public BigDecimal getDailyProfitLoss() {
        return dailyProfitLoss;
    }

    public void setDailyProfitLoss(BigDecimal dailyProfitLoss) {
        this.dailyProfitLoss = dailyProfitLoss;
    }

    public BigDecimal getTotalAssetValue() {
        return totalAssetValue;
    }

    public void setTotalAssetValue(BigDecimal totalAssetValue) {
        this.totalAssetValue = totalAssetValue;
    }

    /**
     * Compute order amount from price and quantity if not set
     */
    public BigDecimal computeOrderAmount() {
        if (orderAmount != null) {
            return orderAmount;
        }
        if (price != null && quantity != null) {
            this.orderAmount = price.multiply(new BigDecimal(quantity));
        }
        return orderAmount;
    }
}
