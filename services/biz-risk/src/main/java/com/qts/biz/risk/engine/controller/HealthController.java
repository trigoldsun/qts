package com.qts.biz.risk.engine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qts.biz.risk.engine.service.RuleEvaluationService;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller
 * Following ESD-MANDATORY-001 L2-005: Observability Design
 * 
 * /healthz - Liveness probe
 * /readyz - Readiness probe (includes dependency checks)
 */
@RestController
public class HealthController {

    private final RuleEvaluationService ruleEvaluationService;

    @Autowired
    public HealthController(RuleEvaluationService ruleEvaluationService) {
        this.ruleEvaluationService = ruleEvaluationService;
    }

    /**
     * Liveness probe
     */
    @GetMapping("/healthz")
    public ResponseEntity<Map<String, Object>> healthz() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "biz-risk-engine");
        return ResponseEntity.ok(response);
    }

    /**
     * Readiness probe with dependency checks
     */
    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> readyz() {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> checks = new HashMap<>();
        
        try {
            // Check if rules are loaded
            int ruleCount = ruleEvaluationService.getAllRules().size();
            checks.put("rules", "UP (" + ruleCount + " rules loaded)");
        } catch (Exception e) {
            checks.put("rules", "DOWN: " + e.getMessage());
        }
        
        boolean allHealthy = checks.values().stream().allMatch(v -> v.startsWith("UP"));
        response.put("status", allHealthy ? "UP" : "DOWN");
        response.put("checks", checks);
        
        return allHealthy ? ResponseEntity.ok(response) : 
                ResponseEntity.status(503).body(response);
    }
}
