package com.qts.biz.risk.audit.dto;

import com.qts.biz.risk.audit.AuditOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data transfer object for audit log events via Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogDTO {
    
    /**
     * Timestamp with millisecond precision
     */
    private Instant timestamp;
    
    /**
     * User ID who performed the operation
     */
    private String userId;
    
    /**
     * Type of operation performed
     */
    private AuditOperationType operationType;
    
    /**
     * Business data in JSON format
     */
    private String businessData;
    
    /**
     * IP address of the client
     */
    private String ipAddress;
}
