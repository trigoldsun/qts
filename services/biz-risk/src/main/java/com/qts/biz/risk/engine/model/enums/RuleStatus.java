package com.qts.biz.risk.engine.model.enums;

/**
 * Rule Status Enum - defines the active state of a rule
 */
public enum RuleStatus {
    ACTIVE("Rule is active and enforced"),
    INACTIVE("Rule is inactive and not enforced"),
    SUSPENDED("Rule is temporarily suspended");

    private final String description;

    RuleStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
