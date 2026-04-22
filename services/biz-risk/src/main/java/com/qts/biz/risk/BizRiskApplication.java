package com.qts.biz.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Biz-Risk Application
 * Risk control service providing pre-trade validation and real-time monitoring
 * 
 * Monitor module features:
 * - Trading channel metrics (order count, amount, cancel rate, latency P50/P95/P99)
 * - Risk metrics (positions, capital, margin ratio)
 * - WebSocket push at ≤1s frequency via /ws/risk/metrics
 * - Prometheus metrics export via /actuator/prometheus
 * 
 * Compliance: ESD-MANDATORY-001 L2/L4 Standards
 */
@SpringBootApplication(scanBasePackages = {
    "com.qts.biz.risk",
    "com.qts.biz.risk.monitor"
})
@EnableScheduling
public class BizRiskApplication {

    public static void main(String[] args) {
        SpringApplication.run(BizRiskApplication.class, args);
    }
}