package com.qts.biz.risk.audit;

import com.qts.biz.risk.audit.dto.AuditLogDTO;
import com.qts.biz.risk.audit.dto.AuditLogQueryRequest;
import com.qts.biz.risk.audit.dto.AuditLogResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditLogService.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditKafkaProducer kafkaProducer;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLogDTO testAuditLogDTO;
    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testAuditLogDTO = AuditLogDTO.builder()
                .timestamp(Instant.now())
                .userId("user123")
                .operationType(AuditOperationType.ORDER_SUBMIT)
                .businessData("{\"orderId\":\"ORD001\",\"amount\":1000}")
                .ipAddress("192.168.1.100")
                .build();

        testAuditLog = AuditLog.builder()
                .id(1L)
                .timestamp(Instant.now())
                .userId("user123")
                .operationType(AuditOperationType.ORDER_SUBMIT)
                .businessData("{\"orderId\":\"ORD001\",\"amount\":1000}")
                .ipAddress("192.168.1.100")
                .createTime(Instant.now())
                .build();
    }

    @Test
    void logAuditEvent_ShouldSendToKafka() {
        // Given
        when(kafkaProducer.sendAuditLog(any(AuditLogDTO.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        auditLogService.logAuditEvent(
                "user123",
                AuditOperationType.ORDER_SUBMIT,
                "{\"orderId\":\"ORD001\"}",
                "192.168.1.100"
        );

        // Then
        ArgumentCaptor<AuditLogDTO> captor = ArgumentCaptor.forClass(AuditLogDTO.class);
        verify(kafkaProducer, times(1)).sendAuditLog(captor.capture());

        AuditLogDTO sentDto = captor.getValue();
        assertEquals("user123", sentDto.getUserId());
        assertEquals(AuditOperationType.ORDER_SUBMIT, sentDto.getOperationType());
        assertEquals("{\"orderId\":\"ORD001\"}", sentDto.getBusinessData());
        assertEquals("192.168.1.100", sentDto.getIpAddress());
        assertNotNull(sentDto.getTimestamp());
    }

    @Test
    void logAuditEvent_WithCustomTimestamp_ShouldUseProvidedTimestamp() {
        // Given
        Instant customTimestamp = Instant.parse("2024-01-15T10:30:00Z");
        when(kafkaProducer.sendAuditLog(any(AuditLogDTO.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        auditLogService.logAuditEvent(
                "user123",
                AuditOperationType.ORDER_CANCEL,
                "{}",
                "10.0.0.1",
                customTimestamp
        );

        // Then
        ArgumentCaptor<AuditLogDTO> captor = ArgumentCaptor.forClass(AuditLogDTO.class);
        verify(kafkaProducer).sendAuditLog(captor.capture());

        assertEquals(customTimestamp, captor.getValue().getTimestamp());
    }

    @Test
    void queryAuditLogs_WithNoFilters_ShouldReturnAllLogs() {
        // Given
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByAllCriteria(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        AuditLogQueryRequest request = AuditLogQueryRequest.builder()
                .page(0)
                .size(20)
                .build();

        // When
        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("user123", result.getContent().get(0).getUserId());
        verify(auditLogRepository).findByAllCriteria(
                isNull(), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void queryAuditLogs_WithUserIdFilter_ShouldFilterByUserId() {
        // Given
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByAllCriteria(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        AuditLogQueryRequest request = AuditLogQueryRequest.builder()
                .userId("user123")
                .page(0)
                .size(20)
                .build();

        // When
        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditLogRepository).findByAllCriteria(
                eq("user123"), isNull(), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void queryAuditLogs_WithOperationTypeFilter_ShouldFilterByType() {
        // Given
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByAllCriteria(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        AuditLogQueryRequest request = AuditLogQueryRequest.builder()
                .operationType(AuditOperationType.ORDER_SUBMIT)
                .page(0)
                .size(20)
                .build();

        // When
        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(request);

        // Then
        assertNotNull(result);
        verify(auditLogRepository).findByAllCriteria(
                isNull(), eq(AuditOperationType.ORDER_SUBMIT), isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void queryAuditLogs_WithTimeRange_ShouldFilterByTimeRange() {
        // Given
        Instant startTime = Instant.parse("2024-01-01T00:00:00Z");
        Instant endTime = Instant.parse("2024-01-31T23:59:59Z");
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByAllCriteria(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        AuditLogQueryRequest request = AuditLogQueryRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .page(0)
                .size(20)
                .build();

        // When
        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(request);

        // Then
        assertNotNull(result);
        verify(auditLogRepository).findByAllCriteria(
                isNull(), isNull(), eq(startTime), eq(endTime), any(Pageable.class)
        );
    }

    @Test
    void queryAuditLogs_WithAllFilters_ShouldFilterByAllCriteria() {
        // Given
        Instant startTime = Instant.parse("2024-01-01T00:00:00Z");
        Instant endTime = Instant.parse("2024-01-31T23:59:59Z");
        Page<AuditLog> mockPage = new PageImpl<>(List.of(testAuditLog));
        when(auditLogRepository.findByAllCriteria(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPage);

        AuditLogQueryRequest request = AuditLogQueryRequest.builder()
                .userId("user123")
                .operationType(AuditOperationType.ORDER_SUBMIT)
                .startTime(startTime)
                .endTime(endTime)
                .page(0)
                .size(20)
                .build();

        // When
        Page<AuditLogResponse> result = auditLogService.queryAuditLogs(request);

        // Then
        assertNotNull(result);
        verify(auditLogRepository).findByAllCriteria(
                eq("user123"), eq(AuditOperationType.ORDER_SUBMIT), eq(startTime), eq(endTime), any(Pageable.class)
        );
    }

    @Test
    void getAuditLogById_WhenExists_ShouldReturnLog() {
        // Given
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(testAuditLog));

        // When
        AuditLogResponse result = auditLogService.getAuditLogById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("user123", result.getUserId());
        assertEquals(AuditOperationType.ORDER_SUBMIT, result.getOperationType());
    }

    @Test
    void getAuditLogById_WhenNotExists_ShouldReturnNull() {
        // Given
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        AuditLogResponse result = auditLogService.getAuditLogById(999L);

        // Then
        assertNull(result);
    }

    @Test
    void saveAuditLog_ShouldSaveDirectlyToRepository() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(testAuditLog);

        // When
        Long savedId = auditLogService.saveAuditLog(testAuditLogDTO);

        // Then
        assertEquals(1L, savedId);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logAuditEvent_AllOperationTypes_ShouldBeLoggedCorrectly() {
        // Given
        when(kafkaProducer.sendAuditLog(any(AuditLogDTO.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When & Then - Test each operation type
        for (AuditOperationType operationType : AuditOperationType.values()) {
            auditLogService.logAuditEvent(
                    "user-" + operationType.name(),
                    operationType,
                    "{}",
                    "127.0.0.1"
            );

            ArgumentCaptor<AuditLogDTO> captor = ArgumentCaptor.forClass(AuditLogDTO.class);
            verify(kafkaProducer, atLeastOnce()).sendAuditLog(captor.capture());

            AuditLogDTO sentDto = captor.getValue();
            assertEquals(operationType, sentDto.getOperationType());
        }
    }
}
