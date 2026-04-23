package com.qts.biz.risk.grpc;

/**
 * Rule Check Result Proto
 * Individual rule check result within a risk check response
 */
public final class RuleCheckResultProto {
    private final String ruleId;
    private final String ruleName;
    private final String ruleType;
    private final boolean passed;
    private final String detail;

    private RuleCheckResultProto(Builder builder) {
        this.ruleId = builder.ruleId;
        this.ruleName = builder.ruleName;
        this.ruleType = builder.ruleType;
        this.passed = builder.passed;
        this.detail = builder.detail;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getRuleType() {
        return ruleType;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getDetail() {
        return detail;
    }

    public Builder toBuilder() {
        return new Builder()
                .setRuleId(ruleId)
                .setRuleName(ruleName)
                .setRuleType(ruleType)
                .setPassed(passed)
                .setDetail(detail);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private String ruleId = "";
        private String ruleName = "";
        private String ruleType = "";
        private boolean passed = false;
        private String detail = "";

        public Builder setRuleId(String ruleId) {
            this.ruleId = ruleId;
            return this;
        }

        public Builder setRuleName(String ruleName) {
            this.ruleName = ruleName;
            return this;
        }

        public Builder setRuleType(String ruleType) {
            this.ruleType = ruleType;
            return this;
        }

        public Builder setPassed(boolean passed) {
            this.passed = passed;
            return this;
        }

        public Builder setDetail(String detail) {
            this.detail = detail;
            return this;
        }

        public RuleCheckResultProto build() {
            return new RuleCheckResultProto(this);
        }
    }
}