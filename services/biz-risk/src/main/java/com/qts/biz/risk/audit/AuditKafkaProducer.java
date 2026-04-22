package com.qts.biz.risk.audit;

import com.qts.biz.risk.audit.dto.AuditLogDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for async audit log writing.
 * Sends audit events to the audit-log topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditKafkaProducer {

    private final KafkaTemplate<String, AuditLogDTO> kafkaTemplate;

    @Value("${audit.kafka.topic}")
    private String auditTopic;

    /**
     * Send audit log to Kafka asynchronously.
     * Uses userId as the partition key for ordering guarantees per user.
     *
     * @param auditLogDTO the audit log event
     * @return CompletableFuture for async result handling
     */
    public CompletableFuture<SendResult<String, AuditLogDTO>> sendAuditLog(AuditLogDTO auditLogDTO) {
        String key = auditLogDTO.getUserId();
        
        log.debug("Sending audit log to Kafka topic {}: userId={}, operationType={}", 
                auditTopic, auditLogDTO.getUserId(), auditLogDTO.getOperationType());
        
        return kafkaTemplate.send(auditTopic, key, auditLogDTO)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send audit log to Kafka: userId={}, operationType={}, error={}",
                                auditLogDTO.getUserId(), auditLogDTO.getOperationType(), ex.getMessage());
                    } else {
                        log.debug("Successfully sent audit log: topic={}, partition={}, offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
