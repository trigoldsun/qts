package com.qts.biz.trade.dto;

import java.math.BigDecimal;

/**
 * Asset Data Transfer Object
 * Represents account asset information including cash and positions
 */
public class AssetDTO {

    private Long accountId;
    private String currency = "CNY";
    private BigDecimal totalAssets = BigDecimal.ZERO;
    private BigDecimal availableCash = BigDecimal.ZERO;
    private BigDecimal frozenCash = BigDecimal.ZERO;
    private BigDecimal marketValue = BigDecimal.ZERO;
    private BigDecimal totalProfitLoss = BigDecimal.ZERO;
    private BigDecimal todayProfitLoss = BigDecimal.ZERO;
    private BigDecimal margin = BigDecimal.ZERO;
    private BigDecimal maintenanceMargin = BigDecimal.ZERO;
    private RiskLevel riskLevel = RiskLevel.NORMAL;

    public enum RiskLevel {
        NORMAL, WARNING, DANGER
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getAvailableCash() {
        return availableCash;
    }

    public void setAvailableCash(BigDecimal availableCash) {
        this.availableCash = availableCash;
    }

    public BigDecimal getFrozenCash() {
        return frozenCash;
    }

    public void setFrozenCash(BigDecimal frozenCash) {
        this.frozenCash = frozenCash;
    }

    public BigDecimal getMarketValue() {
        return marketValue;
    }

    public void setMarketValue(BigDecimal marketValue) {
        this.marketValue = marketValue;
    }

    public BigDecimal getTotalProfitLoss() {
        return totalProfitLoss;
    }

    public void setTotalProfitLoss(BigDecimal totalProfitLoss) {
        this.totalProfitLoss = totalProfitLoss;
    }

    public BigDecimal getTodayProfitLoss() {
        return todayProfitLoss;
    }

    public void setTodayProfitLoss(BigDecimal todayProfitLoss) {
        this.todayProfitLoss = todayProfitLoss;
    }

    public BigDecimal getMargin() {
        return margin;
    }

    public void setMargin(BigDecimal margin) {
        this.margin = margin;
    }

    public BigDecimal getMaintenanceMargin() {
        return maintenanceMargin;
    }

    public void setMaintenanceMargin(BigDecimal maintenanceMargin) {
        this.maintenanceMargin = maintenanceMargin;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(RiskLevel riskLevel) {
        this.riskLevel = riskLevel;
    }
}
