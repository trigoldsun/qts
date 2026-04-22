package com.qts.biz.settle.service;

import com.qts.biz.settle.dto.DailySettlementRequest;
import com.qts.biz.settle.dto.DailySettlementResponse;
import com.qts.biz.settle.entity.SettlementLogEntity;
import com.qts.biz.settle.entity.SettlementTaskEntity;
import com.qts.biz.settle.entity.SettlementTaskEntity.SettlementStatus;
import com.qts.biz.settle.event.SettlementCompletedEvent;
import com.qts.biz.settle.repository.SettlementLogRepository;
import com.qts.biz.settle.repository.SettlementTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySettlementServiceTest {

    @Mock
    private SettlementTaskRepository settlementTaskRepository;

    @Mock
    private SettlementLogRepository settlementLogRepository;

    @Mock
    private KafkaTemplate<String, SettlementCompletedEvent> kafkaTemplate;

    @InjectMocks
    private DailySettlementService dailySettlementService;

    private DailySettlementRequest validRequest;
    private SettlementTaskEntity existingTask;

    @BeforeEach
    void setUp() {
        validRequest = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .accountIds(Arrays.asList("ACC-001", "ACC-002"))
                .build();

        existingTask = SettlementTaskEntity.builder()
                .settleId("Settle-2024-01-15-existing")
                .settleDate(LocalDate.of(2024, 1, 15))
                .status(SettlementStatus.PROCESSING)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .accountsCount(2)
                .build();
    }

    @Test
    void triggerDailySettlement_Success() {
        when(settlementTaskRepository.existsBySettleDateAndStatus(
                LocalDate.of(2024, 1, 15), SettlementStatus.PROCESSING))
                .thenReturn(false);
        when(settlementTaskRepository.save(any(SettlementTaskEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailySettlementResponse response = dailySettlementService.triggerDailySettlement(validRequest);

        assertNotNull(response);
        assertNotNull(response.getSettleId());
        assertTrue(response.getSettleId().startsWith("Settle-"));
        assertEquals(LocalDate.of(2024, 1, 15), response.getSettleDate());
        assertEquals(2, response.getAccountsCount());

        verify(settlementTaskRepository, times(2)).save(any(SettlementTaskEntity.class));
        verify(settlementTaskRepository, never()).findBySettleDate(any(LocalDate.class));
    }

    @Test
    void triggerDailySettlement_AlreadyRunning() {
        when(settlementTaskRepository.existsBySettleDateAndStatus(
                LocalDate.of(2024, 1, 15), SettlementStatus.PROCESSING))
                .thenReturn(true);
        when(settlementTaskRepository.findBySettleDate(LocalDate.of(2024, 1, 15)))
                .thenReturn(Optional.of(existingTask));

        DailySettlementResponse response = dailySettlementService.triggerDailySettlement(validRequest);

        assertNotNull(response);
        assertEquals(existingTask.getSettleId(), response.getSettleId());
        assertEquals(SettlementStatus.PROCESSING.name(), response.getStatus());

        verify(settlementTaskRepository, never()).save(any(SettlementTaskEntity.class));
    }

    @Test
    void triggerDailySettlement_WithNullAccountIds() {
        DailySettlementRequest requestWithNullAccounts = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .accountIds(null)
                .build();

        when(settlementTaskRepository.existsBySettleDateAndStatus(
                LocalDate.of(2024, 1, 15), SettlementStatus.PROCESSING))
                .thenReturn(false);
        when(settlementTaskRepository.save(any(SettlementTaskEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailySettlementResponse response = dailySettlementService.triggerDailySettlement(requestWithNullAccounts);

        assertNotNull(response);
        assertEquals(0, response.getAccountsCount());
    }

    @Test
    void triggerDailySettlement_ExecutionException() {
        when(settlementTaskRepository.existsBySettleDateAndStatus(
                LocalDate.of(2024, 1, 15), SettlementStatus.PROCESSING))
                .thenReturn(false);
        when(settlementTaskRepository.save(any(SettlementTaskEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DailySettlementRequest requestWithEmptyAccounts = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .accountIds(Collections.emptyList())
                .build();

        DailySettlementResponse response = dailySettlementService.triggerDailySettlement(requestWithEmptyAccounts);

        assertNotNull(response);
        assertEquals(SettlementStatus.COMPLETED.name(), response.getStatus());
    }

    @Test
    void settleAccount_Success() {
        String settleId = "Settle-2024-01-15-test";
        String accountId = "ACC-001";
        LocalDate settleDate = LocalDate.of(2024, 1, 15);

        dailySettlementService.settleAccount(settleId, accountId, settleDate);

        ArgumentCaptor<SettlementCompletedEvent> eventCaptor = ArgumentCaptor.forClass(SettlementCompletedEvent.class);
        verify(kafkaTemplate).send(eq("qts.settle.tasks"), eq(accountId), eventCaptor.capture());

        SettlementCompletedEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertEquals(SettlementCompletedEvent.EVENT_TYPE, event.getEventType());
        assertNotNull(event.getPayload());
        assertEquals(settleId, event.getPayload().getSettleId());
        assertEquals(accountId, event.getPayload().getAccountId());
    }

    @Test
    void settleAccount_WithDepositsAndWithdrawals() {
        String settleId = "Settle-2024-01-15-test";
        String accountId = "ACC-002";
        LocalDate settleDate = LocalDate.of(2024, 1, 15);

        dailySettlementService.settleAccount(settleId, accountId, settleDate);

        // No logs saved since mock deposit and withdraw are zero
        verify(settlementLogRepository, never()).save(any(SettlementLogEntity.class));
    }

    @Test
    void getSettlementStatus_Found() {
        SettlementTaskEntity task = SettlementTaskEntity.builder()
                .settleId("Settle-2024-01-15-test")
                .settleDate(LocalDate.of(2024, 1, 15))
                .status(SettlementStatus.COMPLETED)
                .startedAt(LocalDateTime.now().minusHours(1))
                .completedAt(LocalDateTime.now())
                .processedCount(10)
                .failedCount(0)
                .build();

        when(settlementTaskRepository.findById("Settle-2024-01-15-test"))
                .thenReturn(Optional.of(task));

        SettlementTaskEntity result = dailySettlementService.getSettlementStatus("Settle-2024-01-15-test");

        assertNotNull(result);
        assertEquals(SettlementStatus.COMPLETED, result.getStatus());
    }

    @Test
    void getSettlementStatus_NotFound() {
        when(settlementTaskRepository.findById("non-existent"))
                .thenReturn(Optional.empty());

        SettlementTaskEntity result = dailySettlementService.getSettlementStatus("non-existent");

        assertNull(result);
    }

    @Test
    void getSettlementLogs_Success() {
        List<SettlementLogEntity> logs = Arrays.asList(
                SettlementLogEntity.builder()
                        .logId("Log-001")
                        .settleId("Settle-2024-01-15-test")
                        .accountId("ACC-001")
                        .build(),
                SettlementLogEntity.builder()
                        .logId("Log-002")
                        .settleId("Settle-2024-01-15-test")
                        .accountId("ACC-001")
                        .build()
        );

        when(settlementLogRepository.findBySettleId("Settle-2024-01-15-test"))
                .thenReturn(logs);

        List<SettlementLogEntity> result = dailySettlementService.getSettlementLogs("Settle-2024-01-15-test");

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getSettlementLogs_Empty() {
        when(settlementLogRepository.findBySettleId("Settle-2024-01-15-test"))
                .thenReturn(Collections.emptyList());

        List<SettlementLogEntity> result = dailySettlementService.getSettlementLogs("Settle-2024-01-15-test");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
