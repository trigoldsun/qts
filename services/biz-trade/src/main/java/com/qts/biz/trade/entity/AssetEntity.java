package com.qts.biz.trade.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Asset Entity
 * Represents account asset data stored in PostgreSQL
 */
@Entity
@Table(name = "t_asset")
public class AssetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, unique = true)
    private Long accountId;

    @Column(name = "currency", length = 10)
    private String currency = "CNY";

    @Column(name = "total_assets", precision = 20, scale = 4)
    private BigDecimal totalAssets = BigDecimal.ZERO;

    @Column(name = "available_cash", precision = 20, scale = 4)
    private BigDecimal availableCash = BigDecimal.ZERO;

    @Column(name = "frozen_cash", precision = 20, scale = 4)
    private BigDecimal frozenCash = BigDecimal.ZERO;

    @Column(name = "market_value", precision = 20, scale = 4)
    private BigDecimal marketValue = BigDecimal.ZERO;

    @Column(name = "total_profit_loss", precision = 20, scale = 4)
    private BigDecimal totalProfitLoss = BigDecimal.ZERO;

    @Column(name = "today_profit_loss", precision = 20, scale = 4)
    private BigDecimal todayProfitLoss = BigDecimal.ZERO;

    @Column(name = "margin", precision = 20, scale = 4)
    private BigDecimal margin = BigDecimal.ZERO;

    @Column(name = "maintenance_margin", precision = 20, scale = 4)
    private BigDecimal maintenanceMargin = BigDecimal.ZERO;

    @Column(name = "risk_level", length = 20)
    private String riskLevel = "NORMAL";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
