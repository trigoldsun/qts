package com.qts.biz.risk.engine.repository;

import com.qts.biz.risk.engine.model.entity.RiskRuleConfigEntity;
import com.qts.biz.risk.engine.model.enums.RuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Risk Rule Configuration Repository
 * JPA Repository for PostgreSQL risk_rule_config table
 */
@Repository
public interface RiskRuleConfigRepository extends JpaRepository<RiskRuleConfigEntity, Long> {

    /**
     * Find a rule by its unique ruleId
     */
    Optional<RiskRuleConfigEntity> findByRuleId(String ruleId);

    /**
     * Find all rules by status, ordered by priority
     */
    List<RiskRuleConfigEntity> findByStatusOrderByPriorityAsc(RuleStatus status);

    /**
     * Check if a rule exists by ruleId
     */
    boolean existsByRuleId(String ruleId);

    /**
     * Delete a rule by ruleId
     */
    void deleteByRuleId(String ruleId);
}
