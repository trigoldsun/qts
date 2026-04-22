package com.qts.biz.risk.engine.core;

/**
 * Rule Evaluation Result
 * Immutable result of a single rule evaluation
 */
public final class RuleResult {

    private final String ruleId;
    private final String ruleName;
    private final boolean passed;
    private final String detail;
    private final String rejectCode;

    private RuleResult(String ruleId, String ruleName, boolean passed, String detail, String rejectCode) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.passed = passed;
        this.detail = detail;
        this.rejectCode = rejectCode;
    }

    /**
     * Create a pass result
     */
    public static RuleResult pass(String ruleId, String ruleName, String detail) {
        return new RuleResult(ruleId, ruleName, true, detail, null);
    }

    /**
     * Create a fail result with reject code
     */
    public static RuleResult fail(String ruleId, String ruleName, String rejectCode, String detail) {
        return new RuleResult(ruleId, ruleName, false, detail, rejectCode);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getDetail() {
        return detail;
    }

    public String getRejectCode() {
        return rejectCode;
    }

    @Override
    public String toString() {
        return "RuleResult{" +
                "ruleId='" + ruleId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", passed=" + passed +
                ", detail='" + detail + '\'' +
                ", rejectCode='" + rejectCode + '\'' +
                '}';
    }
}
