package com.qts.biz.account.grpc;

/**
 * Account validation request for gRPC call
 */
public final class AccountValidateRequest {
    private final String accountId;

    private AccountValidateRequest(Builder builder) {
        this.accountId = builder.accountId;
    }

    public String getAccountId() {
        return accountId;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String accountId = "";

        public Builder setAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public AccountValidateRequest build() {
            return new AccountValidateRequest(this);
        }
    }
}