package com.qts.biz.risk.audit.dto;

import com.qts.biz.risk.audit.AuditLog;
import com.qts.biz.risk.audit.AuditOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response DTO for audit log queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    
    private Long id;
    private Instant timestamp;
    private String userId;
    private AuditOperationType operationType;
    private String businessData;
    private String ipAddress;
    
    /**
     * Convert entity to response DTO
     */
    public static AuditLogResponse fromEntity(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .userId(entity.getUserId())
                .operationType(entity.getOperationType())
                .businessData(entity.getBusinessData())
                .ipAddress(entity.getIpAddress())
                .build();
    }
}
