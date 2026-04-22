package com.qts.biz.risk.engine.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Risk Check Response DTO
 * Result of risk rule evaluation
 */
public class RiskCheckResponse {

    private boolean passed;
    private String accountId;
    private String message;
    private String rejectCode;
    private List<RuleCheckResult> ruleResults;

    public RiskCheckResponse() {
        this.ruleResults = new ArrayList<>();
    }

    public static RiskCheckResponse pass(String accountId) {
        RiskCheckResponse response = new RiskCheckResponse();
        response.setPassed(true);
        response.setAccountId(accountId);
        response.setMessage("Risk check passed");
        return response;
    }

    public static RiskCheckResponse reject(String accountId, String rejectCode, String message) {
        RiskCheckResponse response = new RiskCheckResponse();
        response.setPassed(false);
        response.setAccountId(accountId);
        response.setRejectCode(rejectCode);
        response.setMessage(message);
        return response;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRejectCode() {
        return rejectCode;
    }

    public void setRejectCode(String rejectCode) {
        this.rejectCode = rejectCode;
    }

    public List<RuleCheckResult> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(List<RuleCheckResult> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public void addRuleResult(RuleCheckResult result) {
        this.ruleResults.add(result);
    }

    /**
     * Inner class for individual rule check results
     */
    public static class RuleCheckResult {
        private String ruleId;
        private String ruleName;
        private String ruleType;
        private boolean passed;
        private String detail;

        public RuleCheckResult() {}

        public RuleCheckResult(String ruleId, String ruleName, String ruleType, boolean passed, String detail) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.ruleType = ruleType;
            this.passed = passed;
            this.detail = detail;
        }

        public String getRuleId() {
            return ruleId;
        }

        public void setRuleId(String ruleId) {
            this.ruleId = ruleId;
        }

        public String getRuleName() {
            return ruleName;
        }

        public void setRuleName(String ruleName) {
            this.ruleName = ruleName;
        }

        public String getRuleType() {
            return ruleType;
        }

        public void setRuleType(String ruleType) {
            this.ruleType = ruleType;
        }

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getDetail() {
            return detail;
        }

        public void setDetail(String detail) {
            this.detail = detail;
        }
    }
}
