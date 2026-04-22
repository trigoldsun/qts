package com.qts.biz.risk.engine.model.enums;

/**
 * Rule Type Enum - defines the category of risk rule
 */
public enum RuleType {
    POSITION_LIMIT("PositionLimitRule", "Single position holding limit"),
    SINGLE_ORDER_LIMIT("SingleOrderLimitRule", "Single order amount limit"),
    DAILY_LOSS_LIMIT("DailyLossLimitRule", "Daily loss limit"),
    NET_BUY_LIMIT("NetBuyLimitRule", "Daily net buy limit");

    private final String ruleClass;
    private final String description;

    RuleType(String ruleClass, String description) {
        this.ruleClass = ruleClass;
        this.description = description;
    }

    public String getRuleClass() {
        return ruleClass;
    }

    public String getDescription() {
        return description;
    }
}
