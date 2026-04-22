package com.qts.biz.risk.audit.dto;

import com.qts.biz.risk.audit.AuditOperationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Query request for audit log search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogQueryRequest {
    
    /**
     * User ID to filter (optional)
     */
    private String userId;
    
    /**
     * Operation type to filter (optional)
     */
    private AuditOperationType operationType;
    
    /**
     * Start time for range query (inclusive)
     */
    private Instant startTime;
    
    /**
     * End time for range query (inclusive)
     */
    private Instant endTime;
    
    /**
     * Page number (0-indexed)
     */
    @Builder.Default
    private Integer page = 0;
    
    /**
     * Page size
     */
    @Builder.Default
    private Integer size = 20;
}
