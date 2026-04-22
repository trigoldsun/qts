package com.qts.biz.settle.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DailySettlementResponseTest {

    @Test
    void testBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime startedAt = LocalDateTime.of(2024, 1, 15, 18, 0, 0);
        
        DailySettlementResponse response = DailySettlementResponse.builder()
                .settleId("SETTLE-001")
                .settleDate(settleDate)
                .status("COMPLETED")
                .startedAt(startedAt)
                .accountsCount(5)
                .build();
        
        assertEquals("SETTLE-001", response.getSettleId());
        assertEquals(settleDate, response.getSettleDate());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(startedAt, response.getStartedAt());
        assertEquals(5, response.getAccountsCount());
    }

    @Test
    void testNoArgsConstructor() {
        DailySettlementResponse response = new DailySettlementResponse();
        assertNull(response.getSettleId());
        assertNull(response.getSettleDate());
        assertNull(response.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime startedAt = LocalDateTime.of(2024, 1, 15, 18, 0, 0);
        
        DailySettlementResponse response = new DailySettlementResponse(
                "SETTLE-002", settleDate, "PROCESSING", startedAt, 10);
        
        assertEquals("SETTLE-002", response.getSettleId());
        assertEquals(settleDate, response.getSettleDate());
        assertEquals("PROCESSING", response.getStatus());
        assertEquals(startedAt, response.getStartedAt());
        assertEquals(10, response.getAccountsCount());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime startedAt = LocalDateTime.of(2024, 1, 15, 18, 0, 0);
        
        DailySettlementResponse response1 = DailySettlementResponse.builder()
                .settleId("SETTLE-001")
                .settleDate(settleDate)
                .status("COMPLETED")
                .startedAt(startedAt)
                .accountsCount(5)
                .build();
        
        DailySettlementResponse response2 = DailySettlementResponse.builder()
                .settleId("SETTLE-001")
                .settleDate(settleDate)
                .status("COMPLETED")
                .startedAt(startedAt)
                .accountsCount(5)
                .build();
        
        assertEquals(response1, response2);
        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void testNotEquals() {
        DailySettlementResponse response1 = DailySettlementResponse.builder()
                .settleId("SETTLE-001")
                .build();
        
        DailySettlementResponse response2 = DailySettlementResponse.builder()
                .settleId("SETTLE-002")
                .build();
        
        assertNotEquals(response1, response2);
    }

    @Test
    void testToString() {
        DailySettlementResponse response = DailySettlementResponse.builder()
                .settleId("SETTLE-001")
                .status("COMPLETED")
                .accountsCount(5)
                .build();
        
        String str = response.toString();
        assertTrue(str.contains("SETTLE-001"));
        assertTrue(str.contains("COMPLETED"));
    }
}