package com.qts.biz.risk.audit;

import com.qts.biz.risk.audit.dto.AuditLogDTO;
import com.qts.biz.risk.audit.dto.AuditLogQueryRequest;
import com.qts.biz.risk.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main service for audit log operations.
 * Provides async logging via Kafka and query capabilities.
 * 
 * Compliance: Data retention >= 5 years, non-deletable, append-only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditKafkaProducer kafkaProducer;
    private final AuditLogRepository auditLogRepository;

    // Simple in-memory cache for recent audit logs (optional optimization)
    private final Map<String, AuditLogDTO> recentLogsCache = new ConcurrentHashMap<>();

    /**
     * Log an audit event asynchronously via Kafka.
     * This is the primary method for recording audit events.
     *
     * @param userId        User ID
     * @param operationType Operation type
     * @param businessData  Business data as JSON string
     * @param ipAddress     Client IP address
     */
    public void logAuditEvent(String userId, AuditOperationType operationType, 
                              String businessData, String ipAddress) {
        logAuditEvent(userId, operationType, businessData, ipAddress, Instant.now());
    }

    /**
     * Log an audit event with custom timestamp.
     *
     * @param userId        User ID
     * @param operationType Operation type
     * @param businessData  Business data as JSON string
     * @param ipAddress     Client IP address
     * @param timestamp     Custom timestamp
     */
    public void logAuditEvent(String userId, AuditOperationType operationType,
                              String businessData, String ipAddress, Instant timestamp) {
        AuditLogDTO auditLogDTO = AuditLogDTO.builder()
                .timestamp(timestamp)
                .userId(userId)
                .operationType(operationType)
                .businessData(businessData)
                .ipAddress(ipAddress)
                .build();

        log.debug("Logging audit event: userId={}, operationType={}, ipAddress={}",
                userId, operationType, ipAddress);

        // Send to Kafka asynchronously
        kafkaProducer.sendAuditLog(auditLogDTO);

        // Optionally cache recent logs
        String cacheKey = userId + ":" + timestamp.toEpochMilli();
        recentLogsCache.put(cacheKey, auditLogDTO);

        // Limit cache size
        if (recentLogsCache.size() > 1000) {
            // Remove oldest entries
            recentLogsCache.entrySet().stream()
                    .limit(100)
                    .map(Map.Entry::getKey)
                    .forEach(recentLogsCache::remove);
        }
    }

    /**
     * Query audit logs with filters and pagination.
     *
     * @param request Query request with filters
     * @return Page of audit log responses
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> queryAuditLogs(AuditLogQueryRequest request) {
        // Build pageable with sorting
        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20,
                Sort.by(Sort.Direction.DESC, "timestamp")
        );

        log.debug("Querying audit logs: userId={}, operationType={}, startTime={}, endTime={}, page={}, size={}",
                request.getUserId(), request.getOperationType(),
                request.getStartTime(), request.getEndTime(),
                request.getPage(), request.getSize());

        // Use the flexible query method
        Page<AuditLog> page = auditLogRepository.findByAllCriteria(
                request.getUserId(),
                request.getOperationType(),
                request.getStartTime(),
                request.getEndTime(),
                pageable
        );

        // Convert to response DTOs
        return page.map(AuditLogResponse::fromEntity);
    }

    /**
     * Get audit log by ID.
     *
     * @param id Audit log ID
     * @return Audit log response or null if not found
     */
    @Transactional(readOnly = true)
    public AuditLogResponse getAuditLogById(Long id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::fromEntity)
                .orElse(null);
    }

    /**
     * Direct synchronous save (for testing or special cases).
     * Note: In production, prefer async logging via Kafka.
     *
     * @param auditLogDTO Audit log DTO
     * @return Saved audit log entity ID
     */
    @Transactional
    public Long saveAuditLog(AuditLogDTO auditLogDTO) {
        AuditLog entity = AuditLog.builder()
                .timestamp(auditLogDTO.getTimestamp())
                .userId(auditLogDTO.getUserId())
                .operationType(auditLogDTO.getOperationType())
                .businessData(auditLogDTO.getBusinessData())
                .ipAddress(auditLogDTO.getIpAddress())
                .build();

        AuditLog saved = auditLogRepository.save(entity);
        log.debug("Direct save of audit log: id={}", saved.getId());
        return saved.getId();
    }
}
