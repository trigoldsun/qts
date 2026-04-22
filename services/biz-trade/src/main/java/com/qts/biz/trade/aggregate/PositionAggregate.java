package com.qts.biz.trade.aggregate;

import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.event.PositionFrozenEvent;
import com.qts.biz.trade.event.PositionUnfrozenEvent;
import com.qts.biz.trade.event.PositionUpdatedEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Position aggregate root with DDD principles
 * Encapsulates position state and business rules
 */
public class PositionAggregate {

    private Long id;
    private Long accountId;
    private String symbol;
    private String symbolName;
    private BigDecimal quantity;
    private BigDecimal frozenQuantity;
    private BigDecimal costPrice;
    private BigDecimal marketPrice;
    private BigDecimal todayBuyQuantity;
    private BigDecimal todaySellQuantity;
    private BigDecimal todayBuyAmount;
    private BigDecimal todaySellAmount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private final List<Object> domainEvents = new ArrayList<>();

    public PositionAggregate() {
        this.quantity = BigDecimal.ZERO;
        this.frozenQuantity = BigDecimal.ZERO;
        this.costPrice = BigDecimal.ZERO;
        this.marketPrice = BigDecimal.ZERO;
        this.todayBuyQuantity = BigDecimal.ZERO;
        this.todaySellQuantity = BigDecimal.ZERO;
        this.todayBuyAmount = BigDecimal.ZERO;
        this.todaySellAmount = BigDecimal.ZERO;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    public PositionAggregate(Long accountId, String symbol, String symbolName) {
        this();
        this.accountId = accountId;
        this.symbol = symbol;
        this.symbolName = symbolName;
    }

    /**
     * Freeze quantity for pending order
     */
    public void freeze(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Freeze quantity must be positive");
        }
        BigDecimal available = getAvailableQuantity();
        if (available.compareTo(qty) < 0) {
            throw new IllegalStateException("Insufficient available quantity: available=" + available + ", requested=" + qty);
        }
        this.frozenQuantity = this.frozenQuantity.add(qty);
        this.updateTime = LocalDateTime.now();
        domainEvents.add(new PositionFrozenEvent(accountId, symbol, qty));
    }

    /**
     * Unfreeze quantity when order cancelled
     */
    public void unfreeze(BigDecimal qty) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Unfreeze quantity must be positive");
        }
        if (this.frozenQuantity.compareTo(qty) < 0) {
            throw new IllegalStateException("Cannot unfreeze more than frozen: frozen=" + this.frozenQuantity + ", requested=" + qty);
        }
        this.frozenQuantity = this.frozenQuantity.subtract(qty);
        this.updateTime = LocalDateTime.now();
        domainEvents.add(new PositionUnfrozenEvent(accountId, symbol, qty));
    }

    /**
     * Apply trade execution to position
     */
    public void applyTrade(TradeDTO trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }
        if (!symbol.equals(trade.getSymbol())) {
            throw new IllegalArgumentException("Trade symbol does not match position: position=" + symbol + ", trade=" + trade.getSymbol());
        }

        BigDecimal tradeQty = trade.getQuantity();
        BigDecimal tradePrice = trade.getPrice();

        if (trade.isBuy()) {
            // Update cost price using weighted average
            BigDecimal totalValue = this.quantity.multiply(this.costPrice).add(tradeQty.multiply(tradePrice));
            BigDecimal totalQty = this.quantity.add(tradeQty);
            if (totalQty.compareTo(BigDecimal.ZERO) > 0) {
                this.costPrice = totalValue.divide(totalQty, 8, RoundingMode.HALF_UP);
            }
            this.quantity = this.quantity.add(tradeQty);
            this.todayBuyQuantity = this.todayBuyQuantity.add(tradeQty);
            this.todayBuyAmount = this.todayBuyAmount.add(tradeQty.multiply(tradePrice));
        } else if (trade.isSell()) {
            // Reduce position
            if (this.quantity.compareTo(tradeQty) < 0) {
                throw new IllegalStateException("Insufficient quantity to sell: quantity=" + this.quantity + ", requested=" + tradeQty);
            }
            this.quantity = this.quantity.subtract(tradeQty);
            // Reduce frozen quantity if partially filled
            this.todaySellQuantity = this.todaySellQuantity.add(tradeQty);
            this.todaySellAmount = this.todaySellAmount.add(tradeQty.multiply(tradePrice));
        }

        this.updateTime = LocalDateTime.now();
        domainEvents.add(new PositionUpdatedEvent(accountId, symbol, quantity, costPrice, marketPrice));
    }

    /**
     * Recalculate market value and profit/loss with current market price
     */
    public void recalculate(BigDecimal currentMarketPrice) {
        if (currentMarketPrice == null || currentMarketPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Market price must be positive");
        }
        this.marketPrice = currentMarketPrice;
        this.updateTime = LocalDateTime.now();
    }

    /**
     * Get available quantity (total - frozen)
     */
    public BigDecimal getAvailableQuantity() {
        return this.quantity.subtract(this.frozenQuantity);
    }

    /**
     * Get market value = quantity * marketPrice
     */
    public BigDecimal getMarketValue() {
        return this.quantity.multiply(this.marketPrice);
    }

    /**
     * Get profit/loss = (marketPrice - costPrice) * quantity
     */
    public BigDecimal getProfitLoss() {
        return this.marketPrice.subtract(this.costPrice).multiply(this.quantity);
    }

    /**
     * Get profit/loss ratio = (marketPrice - costPrice) / costPrice
     */
    public BigDecimal getProfitLossRatio() {
        if (this.costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return this.marketPrice.subtract(this.costPrice).divide(this.costPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * Get and clear domain events
     */
    public List<Object> getDomainEvents() {
        List<Object> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    // Getters
    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public String getSymbol() { return symbol; }
    public String getSymbolName() { return symbolName; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getFrozenQuantity() { return frozenQuantity; }
    public BigDecimal getCostPrice() { return costPrice; }
    public BigDecimal getMarketPrice() { return marketPrice; }
    public BigDecimal getTodayBuyQuantity() { return todayBuyQuantity; }
    public BigDecimal getTodaySellQuantity() { return todaySellQuantity; }
    public BigDecimal getTodayBuyAmount() { return todayBuyAmount; }
    public BigDecimal getTodaySellAmount() { return todaySellAmount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }

    // Setters for repository
    public void setId(Long id) { this.id = id; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setSymbolName(String symbolName) { this.symbolName = symbolName; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public void setFrozenQuantity(BigDecimal frozenQuantity) { this.frozenQuantity = frozenQuantity; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public void setMarketPrice(BigDecimal marketPrice) { this.marketPrice = marketPrice; }
    public void setTodayBuyQuantity(BigDecimal todayBuyQuantity) { this.todayBuyQuantity = todayBuyQuantity; }
    public void setTodaySellQuantity(BigDecimal todaySellQuantity) { this.todaySellQuantity = todaySellQuantity; }
    public void setTodayBuyAmount(BigDecimal todayBuyAmount) { this.todayBuyAmount = todayBuyAmount; }
    public void setTodaySellAmount(BigDecimal todaySellAmount) { this.todaySellAmount = todaySellAmount; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}