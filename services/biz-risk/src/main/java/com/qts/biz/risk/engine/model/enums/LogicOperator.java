package com.qts.biz.risk.engine.model.enums;

/**
 * Logic Operator Enum - defines how rules are combined in a rule chain
 */
public enum LogicOperator {
    AND("All rules must pass"),
    OR("At least one rule must pass");

    private final String description;

    LogicOperator(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
