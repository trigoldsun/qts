package com.qts.biz.asset.grpc;

/**
 * Asset check request for fund sufficiency validation
 */
public final class AssetCheckRequest {
    private final String accountId;
    private final String symbol;
    private final String side;  // BUY or SELL
    private final double amount;
    private final int quantity;

    private AssetCheckRequest(Builder builder) {
        this.accountId = builder.accountId;
        this.symbol = builder.symbol;
        this.side = builder.side;
        this.amount = builder.amount;
        this.quantity = builder.quantity;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public double getAmount() {
        return amount;
    }

    public int getQuantity() {
        return quantity;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String accountId = "";
        private String symbol = "";
        private String side = "";
        private double amount = 0.0;
        private int quantity = 0;

        public Builder setAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setSymbol(String symbol) {
            this.symbol = symbol;
            return this;
        }

        public Builder setSide(String side) {
            this.side = side;
            return this;
        }

        public Builder setAmount(double amount) {
            this.amount = amount;
            return this;
        }

        public Builder setQuantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public AssetCheckRequest build() {
            return new AssetCheckRequest(this);
        }
    }
}