package com.qts.biz.risk.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qts.biz.risk.engine.model.dto.RuleConfigDTO;
import com.qts.biz.risk.engine.model.entity.RiskRuleConfigEntity;
import com.qts.biz.risk.engine.model.enums.LogicOperator;
import com.qts.biz.risk.engine.model.enums.RuleStatus;
import com.qts.biz.risk.engine.model.enums.RuleType;
import com.qts.biz.risk.engine.repository.RiskRuleConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Rule Configuration Service Implementation
 * 
 * Storage: PostgreSQL via JPA
 * Cache: Redis with TTL 60s (via Spring Cache abstraction)
 * 
 * Follows ESD-MANDATORY-001 L2-005: Observability Design
 * - JSON format logging
 * - trace_id support via MDC
 */
@Service
public class RuleConfigServiceImpl implements RuleConfigService {

    private static final Logger logger = LoggerFactory.getLogger(RuleConfigServiceImpl.class);

    private final RiskRuleConfigRepository repository;
    private final ObjectMapper objectMapper;

    @Autowired
    public RuleConfigServiceImpl(RiskRuleConfigRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Cacheable(value = "riskRules", key = "'all_active_rules'", unless = "#result == null || #result.isEmpty()")
    public List<RuleConfigDTO> getAllActiveRules() {
        logger.info("Fetching all active rules from database");
        
        List<RiskRuleConfigEntity> entities = repository.findByStatusOrderByPriorityAsc(RuleStatus.ACTIVE);
        
        List<RuleConfigDTO> configs = entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        logger.info("Retrieved {} active rules from database", configs.size());
        return configs;
    }

    @Override
    @Transactional
    @CacheEvict(value = "riskRules", allEntries = true)
    public void saveRuleConfig(RuleConfigDTO config) {
        logger.info("Saving rule config: ruleId={}, ruleName={}", config.getRuleId(), config.getRuleName());
        
        RiskRuleConfigEntity entity = repository.findByRuleId(config.getRuleId())
                .orElse(new RiskRuleConfigEntity());
        
        entity.setRuleId(config.getRuleId());
        entity.setRuleName(config.getRuleName());
        entity.setRuleType(config.getRuleType());
        entity.setStatus(config.getStatus());
        entity.setPriority(config.getPriority());
        entity.setLogicOperator(config.getLogicOperator());
        entity.setDescription(config.getDescription());
        
        if (config.getParameters() != null) {
            try {
                entity.setParameters(objectMapper.writeValueAsString(config.getParameters()));
            } catch (JsonProcessingException e) {
                logger.error("Failed to serialize parameters for ruleId={}: {}", 
                        config.getRuleId(), e.getMessage());
                throw new RuntimeException("Failed to serialize rule parameters", e);
            }
        }
        
        repository.save(entity);
        logger.info("Rule config saved successfully: ruleId={}", config.getRuleId());
    }

    @Override
    @Transactional
    @CacheEvict(value = "riskRules", allEntries = true)
    public void deleteRuleConfig(String ruleId) {
        logger.info("Deleting rule config: ruleId={}", ruleId);
        
        Optional<RiskRuleConfigEntity> entity = repository.findByRuleId(ruleId);
        if (entity.isPresent()) {
            repository.delete(entity.get());
            logger.info("Rule config deleted: ruleId={}", ruleId);
        } else {
            logger.warn("Rule config not found for deletion: ruleId={}", ruleId);
        }
    }

    @Override
    @Cacheable(value = "riskRules", key = "#ruleId")
    public RuleConfigDTO getRuleById(String ruleId) {
        logger.debug("Fetching rule config by ruleId={}", ruleId);
        
        return repository.findByRuleId(ruleId)
                .map(this::toDTO)
                .orElse(null);
    }

    private RuleConfigDTO toDTO(RiskRuleConfigEntity entity) {
        RuleConfigDTO dto = new RuleConfigDTO();
        dto.setId(entity.getId());
        dto.setRuleId(entity.getRuleId());
        dto.setRuleName(entity.getRuleName());
        dto.setRuleType(entity.getRuleType());
        dto.setStatus(entity.getStatus());
        dto.setPriority(entity.getPriority());
        dto.setLogicOperator(entity.getLogicOperator());
        dto.setDescription(entity.getDescription());
        dto.setVersion(entity.getVersion());
        
        if (entity.getParameters() != null && !entity.getParameters().isEmpty()) {
            try {
                Map<String, Object> params = objectMapper.readValue(
                        entity.getParameters(),
                        new TypeReference<Map<String, Object>>() {}
                );
                dto.setParameters(params);
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize parameters for ruleId={}: {}", 
                        entity.getRuleId(), e.getMessage());
                dto.setParameters(Collections.emptyMap());
            }
        }
        
        return dto;
    }
}
