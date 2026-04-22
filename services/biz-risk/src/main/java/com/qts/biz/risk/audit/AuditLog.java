package com.qts.biz.risk.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Audit log entity for compliance.
 * Partitioned by month for efficient querying and retention management.
 * Data retention: minimum 5 years (non-deletable, append-only).
 */
@Entity
@Table(name = "audit_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Timestamp with millisecond precision
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * User ID who performed the operation
     */
    @Column(name = "user_id", nullable = false)
    private String userId;

    /**
     * Type of operation performed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private AuditOperationType operationType;

    /**
     * Business data in JSON format
     */
    @Column(name = "business_data", columnDefinition = "TEXT")
    private String businessData;

    /**
     * IP address of the client
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * Create time
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private Instant createTime;

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = Instant.now();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }
}
