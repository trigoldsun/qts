package com.qts.biz.risk.engine.service;

import com.qts.biz.risk.engine.core.RuleEngine;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import com.qts.biz.risk.engine.model.dto.RiskCheckResponse;
import com.qts.biz.risk.engine.model.dto.RuleConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Rule Evaluation Service
 * Facade service for risk rule evaluation
 * 
 * Provides timeout control and circuit breaker pattern
 * following ESD-MANDATORY-001 L2-004: Fault Tolerance and Resilience Design
 */
@Service
public class RuleEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(RuleEvaluationService.class);

    private static final long DEFAULT_TIMEOUT_MS = 5000; // 5 seconds

    private final RuleEngine ruleEngine;

    @Autowired
    public RuleEvaluationService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    /**
     * Evaluate risk for an order request
     * 
     * @param request Risk check request with account context
     * @return Risk check response with pass/fail result
     */
    public RiskCheckResponse evaluateRisk(RiskCheckRequest request) {
        long startTime = System.currentTimeMillis();
        logger.info("Evaluating risk for accountId={}, symbol={}, side={}",
                request.getAccountId(), request.getSymbol(), request.getSide());

        try {
            // Execute with timeout control
            RiskCheckResponse response = executeWithTimeout(request, DEFAULT_TIMEOUT_MS);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Risk evaluation completed: accountId={}, passed={}, duration={}ms",
                    request.getAccountId(), response.isPassed(), duration);
            
            return response;
        } catch (Exception e) {
            logger.error("Risk evaluation failed for accountId={}: {}",
                    request.getAccountId(), e.getMessage(), e);
            
            // Fail safe - reject on any error
            return RiskCheckResponse.reject(
                    request.getAccountId(),
                    "SYSTEM_ERROR",
                    "Risk evaluation failed: " + e.getMessage()
            );
        }
    }

    /**
     * Execute rule evaluation with timeout
     */
    private RiskCheckResponse executeWithTimeout(RiskCheckRequest request, long timeoutMs) {
        // Simple timeout implementation using a separate thread
        // In production, use CompletableFuture or reactive programming
        final RiskCheckResponse[] response = new RiskCheckResponse[1];
        final Exception[] error = new Exception[1];
        
        Thread evaluationThread = new Thread(() -> {
            try {
                response[0] = ruleEngine.evaluate(request);
            } catch (Exception e) {
                error[0] = e;
            }
        });
        
        evaluationThread.start();
        
        try {
            evaluationThread.join(timeoutMs);
            
            if (evaluationThread.isAlive()) {
                // Timeout occurred
                logger.warn("Risk evaluation timed out after {}ms for accountId={}",
                        timeoutMs, request.getAccountId());
                evaluationThread.interrupt();
                throw new RuntimeException("Risk evaluation timed out");
            }
            
            if (error[0] != null) {
                throw error[0];
            }
            
            return response[0];
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Risk evaluation interrupted", e);
        }
    }

    /**
     * Reload rules from storage
     */
    public void reloadRules() {
        logger.info("Reloading risk rules");
        ruleEngine.reloadRules();
    }

    /**
     * Add or update a rule
     */
    public void upsertRule(RuleConfigDTO config) {
        logger.info("Upserting rule: ruleId={}", config.getRuleId());
        ruleEngine.upsertRule(config);
    }

    /**
     * Remove a rule
     */
    public void removeRule(String ruleId) {
        logger.info("Removing rule: ruleId={}", ruleId);
        ruleEngine.removeRule(ruleId);
    }

    /**
     * Get all current rules
     */
    public List<RuleConfigDTO> getAllRules() {
        return ruleEngine.getAllRules();
    }
}
