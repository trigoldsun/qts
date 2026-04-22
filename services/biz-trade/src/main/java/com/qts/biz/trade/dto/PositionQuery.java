package com.qts.biz.trade.dto;

import java.math.BigDecimal;

/**
 * Query object for position list search
 */
public class PositionQuery {

    private String symbol;
    private String symbolName;
    private String side; // BUY, SELL, ALL

    public PositionQuery() {}

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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final PositionQuery query = new PositionQuery();

        public Builder symbol(String symbol) { query.symbol = symbol; return this; }
        public Builder symbolName(String symbolName) { query.symbolName = symbolName; return this; }
        public Builder side(String side) { query.side = side; return this; }

        public PositionQuery build() {
            return query;
        }
    }
}