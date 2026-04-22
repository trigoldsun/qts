package com.qts.biz.risk.audit;

import com.qts.biz.risk.audit.dto.AuditLogQueryRequest;
import com.qts.biz.risk.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST controller for audit log queries.
 * Provides paginated and filtered access to audit logs.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Query audit logs with optional filters.
     *
     * GET /api/v1/audit/logs
     *
     * @param userId        Filter by user ID (optional)
     * @param operationType Filter by operation type (optional)
     * @param startTime     Filter by start time, ISO-8601 format (optional)
     * @param endTime       Filter by end time, ISO-8601 format (optional)
     * @param page          Page number, 0-indexed (default: 0)
     * @param size          Page size (default: 20, max: 100)
     * @return Page of audit log responses
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<AuditLogResponse>> queryAuditLogs(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) AuditOperationType operationType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        // Limit page size
        if (size > 100) {
            size = 100;
        }

        AuditLogQueryRequest request = AuditLogQueryRequest.builder()
                .userId(userId)
                .operationType(operationType)
                .startTime(startTime)
                .endTime(endTime)
                .page(page)
                .size(size)
                .build();

        log.info("Query audit logs request: userId={}, operationType={}, startTime={}, endTime={}, page={}, size={}",
                userId, operationType, startTime, endTime, page, size);

        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(request);

        return ResponseEntity.ok(result);
    }

    /**
     * Get a single audit log by ID.
     *
     * GET /api/v1/audit/logs/{id}
     *
     * @param id Audit log ID
     * @return Audit log response or 404 if not found
     */
    @GetMapping("/logs/{id}")
    public ResponseEntity<AuditLogResponse> getAuditLogById(@PathVariable Long id) {
        log.debug("Get audit log by id: {}", id);

        AuditLogResponse response = auditLogService.getAuditLogById(id);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint for audit service.
     *
     * GET /api/v1/audit/health
     *
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("audit-service:OK");
    }
}
