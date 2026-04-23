package com.qts.biz.asset.grpc;

/**
 * Asset check response for fund sufficiency validation
 */
public final class AssetCheckResponse {
    private final boolean sufficient;
    private final String message;
    private final double availableCash;
    private final double availablePosition;

    private AssetCheckResponse(Builder builder) {
        this.sufficient = builder.sufficient;
        this.message = builder.message;
        this.availableCash = builder.availableCash;
        this.availablePosition = builder.availablePosition;
    }

    public boolean isSufficient() {
        return sufficient;
    }

    public String getMessage() {
        return message;
    }

    public double getAvailableCash() {
        return availableCash;
    }

    public double getAvailablePosition() {
        return availablePosition;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean sufficient = true;
        private String message = "";
        private double availableCash = 0.0;
        private double availablePosition = 0.0;

        public Builder setSufficient(boolean sufficient) {
            this.sufficient = sufficient;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setAvailableCash(double availableCash) {
            this.availableCash = availableCash;
            return this;
        }

        public Builder setAvailablePosition(double availablePosition) {
            this.availablePosition = availablePosition;
            return this;
        }

        public AssetCheckResponse build() {
            return new AssetCheckResponse(this);
        }
    }
}