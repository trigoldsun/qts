package com.qts.biz.risk.engine.model.dto;

import com.qts.biz.risk.engine.model.enums.LogicOperator;
import com.qts.biz.risk.engine.model.enums.RuleStatus;
import com.qts.biz.risk.engine.model.enums.RuleType;

import java.util.Map;

/**
 * Rule Configuration DTO
 * Represents a rule configuration for storage and management
 */
public class RuleConfigDTO {

    private Long id;
    private String ruleId;
    private String ruleName;
    private RuleType ruleType;
    private RuleStatus status;
    private Integer priority; // 1-100, lower is higher priority
    private LogicOperator logicOperator; // AND/OR for chain
    private Map<String, Object> parameters; // Rule-specific parameters in JSON format
    private String description;
    private Long version;

    public RuleConfigDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public RuleStatus getStatus() {
        return status;
    }

    public void setStatus(RuleStatus status) {
        this.status = status;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public LogicOperator getLogicOperator() {
        return logicOperator;
    }

    public void setLogicOperator(LogicOperator logicOperator) {
        this.logicOperator = logicOperator;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
