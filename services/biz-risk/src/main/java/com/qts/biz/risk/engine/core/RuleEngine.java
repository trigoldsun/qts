package com.qts.biz.risk.engine.core;

import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import com.qts.biz.risk.engine.model.dto.RiskCheckResponse;
import com.qts.biz.risk.engine.model.dto.RuleConfigDTO;

/**
 * Rule Engine Interface
 * Core interface for the risk rule evaluation engine
 * 
 * Follows ESD-MANDATORY-001 L2-001: Module division by Business Capability
 */
public interface RuleEngine {

    /**
     * Evaluate all active rules against the risk check request
     * Rules are evaluated in priority order (1-100, lower = higher priority)
     * Logic: AND/OR chain evaluation based on rule chain configuration
     * 
     * @param request Risk check request with account context
     * @return Risk check response with pass/fail result and rule details
     */
    RiskCheckResponse evaluate(RiskCheckRequest request);

    /**
     * Reload rules from storage (for dynamic rule update)
     */
    void reloadRules();

    /**
     * Add or update a rule dynamically
     * @param config Rule configuration
     */
    void upsertRule(RuleConfigDTO config);

    /**
     * Remove a rule by ruleId
     * @param ruleId Rule identifier
     */
    void removeRule(String ruleId);

    /**
     * Get all current rule configurations
     * @return List of rule configurations
     */
    java.util.List<RuleConfigDTO> getAllRules();
}
