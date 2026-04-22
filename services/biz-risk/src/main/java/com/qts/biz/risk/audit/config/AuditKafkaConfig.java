package com.qts.biz.risk.audit.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Kafka configuration for audit log messaging.
 */
@Configuration
public class AuditKafkaConfig {

    @Value("${audit.kafka.topic}")
    private String auditTopic;

    /**
     * Create the audit log topic if it doesn't exist
     */
    @Bean
    public NewTopic auditLogTopic() {
        return TopicBuilder.name(auditTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
