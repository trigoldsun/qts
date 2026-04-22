package com.qts.biz.trade.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Position entity for database persistence
 */
public class PositionEntity {

    private Long id;
    private Long accountId;
    private String symbol;
    private String symbolName;
    private BigDecimal quantity;
    private BigDecimal availableQuantity;
    private BigDecimal frozenQuantity;
    private BigDecimal costPrice;
    private BigDecimal marketPrice;
    private BigDecimal todayBuyQuantity;
    private BigDecimal todaySellQuantity;
    private BigDecimal todayBuyAmount;
    private BigDecimal todaySellAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public PositionEntity() {}

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}