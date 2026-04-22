package com.qts.biz.risk.monitor.config;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.prometheus.PrometheusEndpointAutoConfiguration;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Actuator and Prometheus Configuration
 * Exposes /actuator/prometheus endpoint for metrics scraping
 * 
 * Compliance: ESD-MANDATORY-001 L4 Deployment & Operations Standards
 */
@Configuration
@Import({PrometheusMetricsExportAutoConfiguration.class, PrometheusEndpointAutoConfiguration.class})
public class ActuatorConfiguration {

    /**
     * Customize actuator endpoints
     */
    @Bean
    public WebEndpointProperties.CustomBeanDefinition customWebEndpointProperties() {
        WebEndpointProperties.CustomBeanDefinition custom = new WebEndpointProperties.CustomBeanDefinition();
        return custom;
    }
}
