package com.qts.biz.trade.event;

import com.qts.biz.trade.dto.TradeDTO;

import java.time.LocalDateTime;

/**
 * Trade Executed Event
 * Published to Kafka when a trade is executed
 */
public class TradeExecutedEvent {

    private String eventId;
    private String tradeId;
    private String orderId;
    private String accountId;
    private String symbol;
    private String side;
    private Double price;
    private Integer quantity;
    private Double amount;
    private LocalDateTime tradeTime;
    private LocalDateTime occurredAt;

    public TradeExecutedEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }

    public TradeExecutedEvent(TradeDTO trade) {
        this();
        this.tradeId = trade.getTradeId();
        this.orderId = trade.getOrderId();
        this.accountId = trade.getAccountId() != null ? trade.getAccountId().toString() : null;
        this.symbol = trade.getSymbol();
        this.side = trade.getSide();
        this.price = trade.getPrice() != null ? trade.getPrice().doubleValue() : null;
        this.quantity = trade.getQuantity() != null ? trade.getQuantity().intValue() : null;
        this.amount = trade.getAmount() != null ? trade.getAmount().doubleValue() : null;
        this.tradeTime = trade.getTradeTime();
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

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

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public LocalDateTime getTradeTime() {
        return tradeTime;
    }

    public void setTradeTime(LocalDateTime tradeTime) {
        this.tradeTime = tradeTime;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public String toString() {
        return "TradeExecutedEvent{" +
                "eventId='" + eventId + '\'' +
                ", tradeId='" + tradeId + '\'' +
                ", orderId='" + orderId + '\'' +
                ", accountId='" + accountId + '\'' +
                ", symbol='" + symbol + '\'' +
                ", side='" + side + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", amount=" + amount +
                ", tradeTime=" + tradeTime +
                ", occurredAt=" + occurredAt +
                '}';
    }
}
