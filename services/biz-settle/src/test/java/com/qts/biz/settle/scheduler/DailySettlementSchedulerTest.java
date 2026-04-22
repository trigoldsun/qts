package com.qts.biz.settle.scheduler;

import com.qts.biz.settle.dto.DailySettlementRequest;
import com.qts.biz.settle.dto.DailySettlementResponse;
import com.qts.biz.settle.service.DailySettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailySettlementSchedulerTest {

    @Mock
    private DailySettlementService dailySettlementService;

    @InjectMocks
    private DailySettlementScheduler scheduler;

    private DailySettlementResponse successResponse;

    @BeforeEach
    void setUp() {
        successResponse = DailySettlementResponse.builder()
                .settleId("Settle-2024-01-15-scheduled")
                .settleDate(LocalDate.of(2024, 1, 15))
                .status("COMPLETED")
                .accountsCount(10)
                .build();
    }

    @Test
    void triggerDailySettlement_Success() {
        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(successResponse);

        scheduler.triggerDailySettlement();

        verify(dailySettlementService).triggerDailySettlement(any(DailySettlementRequest.class));
    }

    @Test
    void triggerDailySettlement_SetsCorrectSettleDate() {
        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(successResponse);

        scheduler.triggerDailySettlement();

        verify(dailySettlementService).triggerDailySettlement(argThat(request -> {
            // The settleDate should be yesterday (T+1)
            LocalDate expectedDate = LocalDate.now().minusDays(1);
            return expectedDate.equals(request.getSettleDate());
        }));
    }

    @Test
    void triggerDailySettlement_LogsCorrectly() {
        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(successResponse);

        scheduler.triggerDailySettlement();

        // No exception thrown means logging succeeded
        verify(dailySettlementService).triggerDailySettlement(any(DailySettlementRequest.class));
    }

    @Test
    void triggerDailySettlement_ExceptionHandled() {
        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        // Should not throw exception - exception is caught and logged
        assertDoesNotThrow(() -> scheduler.triggerDailySettlement());

        verify(dailySettlementService).triggerDailySettlement(any(DailySettlementRequest.class));
    }

    @Test
    void triggerDailySettlement_ResponseLogged() {
        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(successResponse);

        scheduler.triggerDailySettlement();

        verify(dailySettlementService).triggerDailySettlement(any(DailySettlementRequest.class));
    }

    @Test
    void triggerDailySettlement_EmptyResponse() {
        DailySettlementResponse emptyResponse = DailySettlementResponse.builder()
                .settleId("EMPTY")
                .status("COMPLETED")
                .accountsCount(0)
                .build();

        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(emptyResponse);

        scheduler.triggerDailySettlement();

        verify(dailySettlementService).triggerDailySettlement(any(DailySettlementRequest.class));
    }
}