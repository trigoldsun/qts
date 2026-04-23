package com.qts.biz.account.grpc;

/**
 * Account validation response from gRPC call
 */
public final class AccountValidateResponse {
    private final boolean valid;
    private final String message;

    private AccountValidateResponse(Builder builder) {
        this.valid = builder.valid;
        this.message = builder.message;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean valid = false;
        private String message = "";

        public Builder setValid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public AccountValidateResponse build() {
            return new AccountValidateResponse(this);
        }
    }
}