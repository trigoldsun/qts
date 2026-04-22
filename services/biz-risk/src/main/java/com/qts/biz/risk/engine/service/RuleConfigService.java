package com.qts.biz.risk.engine.service;

import com.qts.biz.risk.engine.model.dto.RuleConfigDTO;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * Rule Configuration Service Interface
 */
public interface RuleConfigService {

    /**
     * Get all active rules
     * Cached in Redis with TTL 60s
     */
    @Cacheable(value = "riskRules", key = "'all_active_rules'")
    List<RuleConfigDTO> getAllActiveRules();

    /**
     * Save or update a rule configuration
     * Evicts the cache after update
     */
    @CacheEvict(value = "riskRules", allEntries = true)
    void saveRuleConfig(RuleConfigDTO config);

    /**
     * Delete a rule configuration
     * Evicts the cache after delete
     */
    @CacheEvict(value = "riskRules", allEntries = true)
    void deleteRuleConfig(String ruleId);

    /**
     * Get a rule by ruleId
     */
    RuleConfigDTO getRuleById(String ruleId);
}
