package com.qts.biz.risk.audit;

/**
 * Audit operation types for compliance logging.
 * Following ESD-MANDATORY-001 L2-005: Observability Design
 */
public enum AuditOperationType {
    ORDER_SUBMIT("订单提交"),
    ORDER_MODIFY("订单修改"),
    ORDER_CANCEL("订单撤销"),
    RISK_PRECHECK_PASS("风控预检通过"),
    RISK_PRECHECK_REJECT("风控预检拦截"),
    ACCOUNT_FREEZE("账户冻结"),
    ACCOUNT_UNFREEZE("账户解冻"),
    SYSTEM_EVENT("系统事件");

    private final String description;

    AuditOperationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
