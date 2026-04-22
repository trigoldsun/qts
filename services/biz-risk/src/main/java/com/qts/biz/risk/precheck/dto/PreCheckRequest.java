package com.qts.biz.risk.precheck.dto;

import java.math.BigDecimal;

/**
 * Pre-check request DTO
 * Input for risk pre-check validation
 */
public class PreCheckRequest {

    private Long accountId;
    private String symbol;
    private String side;  // BUY or SELL
    private BigDecimal price;
    private BigDecimal quantity;
    private BigDecimal marginRate;  // 保证金率，如0.1表示10%

    public PreCheckRequest() {}

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

    public BigDecimal getMarginRate() {
        return marginRate;
    }

    public void setMarginRate(BigDecimal marginRate) {
        this.marginRate = marginRate;
    }

    public boolean isBuy() {
        return "BUY".equalsIgnoreCase(side);
    }

    public boolean isSell() {
        return "SELL".equalsIgnoreCase(side);
    }
}