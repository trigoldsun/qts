package com.qts.biz.risk.audit;

import com.qts.biz.risk.audit.dto.AuditLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Kafka consumer for batch writing audit logs to PostgreSQL.
 * Accumulates records and flushes based on batch size or time interval.
 */
@Slf4j
@Component
public class AuditKafkaConsumer {

    private final AuditLogRepository auditLogRepository;
    private final int batchSize;
    private final int flushIntervalMs;
    
    private final List<AuditLogDTO> buffer = new CopyOnWriteArrayList<>();
    private final ReentrantLock flushLock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public AuditKafkaConsumer(
            AuditLogRepository auditLogRepository,
            @Value("${audit.kafka.batch-size:100}") int batchSize,
            @Value("${audit.kafka.flush-interval-ms:5000}") int flushIntervalMs) {
        this.auditLogRepository = auditLogRepository;
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        
        // Schedule periodic flush
        scheduleFlush();
    }

    /**
     * Schedule periodic flush based on time interval
     */
    private void scheduleFlush() {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                this::flushIfNeeded,
                flushIntervalMs,
                flushIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Kafka listener for audit log messages.
     * Uses manual acknowledgment for reliable processing.
     */
    @KafkaListener(
            topics = "${audit.kafka.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeAuditLog(AuditLogDTO auditLogDTO, Acknowledgment ack) {
        log.debug("Received audit log from Kafka: userId={}, operationType={}",
                auditLogDTO.getUserId(), auditLogDTO.getOperationType());
        
        buffer.add(auditLogDTO);
        
        // Flush if batch size reached
        if (buffer.size() >= batchSize) {
            flush();
        }
        
        // Acknowledge after adding to buffer (actual persistence is async)
        ack.acknowledge();
    }

    /**
     * Flush buffer if there are pending records and either size reached or time interval elapsed
     */
    private void flushIfNeeded() {
        if (!buffer.isEmpty()) {
            flush();
        }
    }

    /**
     * Flush buffered records to database
     */
    public void flush() {
        if (buffer.isEmpty()) {
            return;
        }

        flushLock.lock();
        try {
            // Take current buffer and clear it
            List<AuditLogDTO> toFlush = new ArrayList<>(buffer);
            buffer.clear();
            
            log.info("Flushing {} audit logs to database", toFlush.size());
            
            // Convert DTOs to entities
            List<AuditLog> entities = toFlush.stream()
                    .map(this::toEntity)
                    .toList();
            
            // Batch save
            auditLogRepository.saveAll(entities);
            
            log.info("Successfully persisted {} audit logs to database", entities.size());
        } catch (Exception e) {
            log.error("Failed to flush audit logs to database: {}", e.getMessage(), e);
            // In case of failure, we could implement retry logic here
        } finally {
            flushLock.unlock();
        }
    }

    /**
     * Convert DTO to entity
     */
    private AuditLog toEntity(AuditLogDTO dto) {
        return AuditLog.builder()
                .timestamp(dto.getTimestamp())
                .userId(dto.getUserId())
                .operationType(dto.getOperationType())
                .businessData(dto.getBusinessData())
                .ipAddress(dto.getIpAddress())
                .build();
    }

    /**
     * Get current buffer size (for testing)
     */
    public int getBufferSize() {
        return buffer.size();
    }
}
