package com.qts.biz.risk.monitor.config;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Actuator and Prometheus Configuration
 * Exposes /actuator/prometheus endpoint for metrics scraping
 * 
 * Compliance: ESD-MANDATORY-001 L4 Deployment & Operations Standards
 */
@Configuration
@Import({
    MetricsAutoConfiguration.class,
    PrometheusMetricsExportAutoConfiguration.class
})
public class ActuatorConfiguration {
    // Configuration placeholder for Spring Boot 3.x compatibility
}
