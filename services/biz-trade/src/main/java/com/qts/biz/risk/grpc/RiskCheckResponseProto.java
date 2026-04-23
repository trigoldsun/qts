package com.qts.biz.risk.grpc;

import java.util.List;

/**
 * Risk Check Response Proto
 * gRPC response message for pre-trade risk validation result
 */
public final class RiskCheckResponseProto {
    private final boolean passed;
    private final String accountId;
    private final String message;
    private final String rejectCode;
    private final List<RuleCheckResultProto> ruleResults;

    private RiskCheckResponseProto(Builder builder) {
        this.passed = builder.passed;
        this.accountId = builder.accountId;
        this.message = builder.message;
        this.rejectCode = builder.rejectCode;
        this.ruleResults = builder.ruleResults;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getMessage() {
        return message;
    }

    public String getRejectCode() {
        return rejectCode;
    }

    public List<RuleCheckResultProto> getRuleResults() {
        return ruleResults;
    }

    public Builder toBuilder() {
        return new Builder()
                .setPassed(passed)
                .setAccountId(accountId)
                .setMessage(message)
                .setRejectCode(rejectCode)
                .setRuleResults(ruleResults);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private boolean passed = false;
        private String accountId = "";
        private String message = "";
        private String rejectCode = "";
        private List<RuleCheckResultProto> ruleResults = List.of();

        public Builder setPassed(boolean passed) {
            this.passed = passed;
            return this;
        }

        public Builder setAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        public Builder setRejectCode(String rejectCode) {
            this.rejectCode = rejectCode;
            return this;
        }

        public Builder setRuleResults(List<RuleCheckResultProto> ruleResults) {
            this.ruleResults = ruleResults;
            return this;
        }

        public RiskCheckResponseProto build() {
            return new RiskCheckResponseProto(this);
        }
    }
}