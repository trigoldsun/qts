package com.qts.biz.risk.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository for audit log persistence.
 * Note: Per compliance requirements, delete operations are not allowed.
 * Only insert and select operations are permitted.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID with pagination
     */
    Page<AuditLog> findByUserId(String userId, Pageable pageable);

    /**
     * Find audit logs by operation type with pagination
     */
    Page<AuditLog> findByOperationType(AuditOperationType operationType, Pageable pageable);

    /**
     * Find audit logs within a time range with pagination
     */
    Page<AuditLog> findByTimestampBetween(Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Find audit logs by user ID and operation type with pagination
     */
    Page<AuditLog> findByUserIdAndOperationType(String userId, AuditOperationType operationType, Pageable pageable);

    /**
     * Find audit logs by user ID and time range with pagination
     */
    Page<AuditLog> findByUserIdAndTimestampBetween(String userId, Instant startTime, Instant endTime, Pageable pageable);

    /**
     * Find audit logs by operation type and time range with pagination
     */
    Page<AuditLog> findByOperationTypeAndTimestampBetween(
            AuditOperationType operationType, 
            Instant startTime, 
            Instant endTime, 
            Pageable pageable);

    /**
     * Find audit logs by all criteria with pagination
     */
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) AND " +
           "(:operationType IS NULL OR a.operationType = :operationType) AND " +
           "(:startTime IS NULL OR a.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR a.timestamp <= :endTime)")
    Page<AuditLog> findByAllCriteria(
            @Param("userId") String userId,
            @Param("operationType") AuditOperationType operationType,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime,
            Pageable pageable);

    /**
     * Batch save for Kafka consumer
     */
    List<AuditLog> saveAll(Iterable<AuditLog> entities);
}
