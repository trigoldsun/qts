package com.qts.biz.risk.engine.core;

import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;

/**
 * Individual Rule Interface
 * Each rule implementation must implement this interface
 * 
 * Rules are stateless and thread-safe
 */
public interface RiskRule {

    /**
     * Get the unique rule identifier
     */
    String getRuleId();

    /**
     * Get the rule name for display
     */
    String getRuleName();

    /**
     * Get the rule type
     */
    String getRuleType();

    /**
     * Evaluate the rule against the request
     * 
     * @param request Risk check request with all context data
     * @return Rule evaluation result (pass/fail with details)
     */
    RuleResult evaluate(RiskCheckRequest request);

    /**
     * Check if this rule is enabled
     */
    boolean isEnabled();

    /**
     * Get rule priority (1-100, lower = higher priority)
     */
    int getPriority();
}
