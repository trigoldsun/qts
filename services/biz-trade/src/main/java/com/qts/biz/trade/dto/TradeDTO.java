package com.qts.biz.trade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trade execution information
 */
public class TradeDTO {

    private String tradeId;
    private String orderId;
    private Long accountId;
    private String symbol;
    private String side; // BUY, SELL
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal amount;
    private LocalDateTime tradeTime;

    public TradeDTO() {}

    public String getTradeId() {
        return tradeId;
    }

    public void setTradeId(String tradeId) {
        this.tradeId = tradeId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(LocalDateTime tradeTime) {
        this.tradeTime = tradeTime;
    }

    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }

    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }
}