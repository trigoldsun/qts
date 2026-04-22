package com.qts.biz.risk.monitor.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monitor Service Configuration
 * Configures Micrometer and Prometheus exporter
 * 
 * Compliance: ESD-MANDATORY-001 L4 Deployment & Operations Standards
 */
@Configuration
public class MonitorConfiguration {

    /**
     * Configure Prometheus Meter Registry
     * Exposes metrics at /actuator/prometheus endpoint
     */
    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry(MeterRegistry meterRegistry) {
        if (meterRegistry instanceof PrometheusMeterRegistry) {
            return (PrometheusMeterRegistry) meterRegistry;
        }
        
        // Create a new Prometheus registry if the existing one is not Prometheus-based
        PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Merge metrics from the primary registry
        prometheusRegistry.config().commonTags(Tags.of("service", "biz-risk-monitor"));
        
        return prometheusRegistry;
    }
}
