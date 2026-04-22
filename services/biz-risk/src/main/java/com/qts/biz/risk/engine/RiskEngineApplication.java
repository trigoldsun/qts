package com.qts.biz.risk.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Risk Rule Engine Application
 * 
 * A configurable rule engine for financial risk management
 * 
 * Features:
 * - Rule chain with AND/OR logic
 * - Priority-based rule evaluation (1-100)
 * - PostgreSQL storage for rule configurations
 * - Redis caching (TTL 60s)
 * - Default preset rules: PositionLimit, SingleOrderLimit, DailyLossLimit, NetBuyLimit
 * 
 * Follows ESD-MANDATORY-001 L2 Architecture Design Standards:
 * - L2-001: Module Division
 * - L2-004: Fault Tolerance and Resilience Design
 * - L2-005: Observability Design
 */
@SpringBootApplication
@EnableCaching
public class RiskEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(RiskEngineApplication.class, args);
    }
}
