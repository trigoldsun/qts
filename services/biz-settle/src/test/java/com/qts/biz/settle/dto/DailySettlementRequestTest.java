package com.qts.biz.settle.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DailySettlementRequestTest {

    @Test
    void testBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        List<String> accounts = List.of("ACC-001", "ACC-002");
        
        DailySettlementRequest request = DailySettlementRequest.builder()
                .settleDate(settleDate)
                .accountIds(accounts)
                .build();
        
        assertEquals(settleDate, request.getSettleDate());
        assertEquals(accounts, request.getAccountIds());
    }

    @Test
    void testNoArgsConstructor() {
        DailySettlementRequest request = new DailySettlementRequest();
        assertNull(request.getSettleDate());
        assertNull(request.getAccountIds());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        List<String> accounts = List.of("ACC-001");
        
        DailySettlementRequest request = new DailySettlementRequest(settleDate, accounts);
        
        assertEquals(settleDate, request.getSettleDate());
        assertEquals(accounts, request.getAccountIds());
    }

    @Test
    void testEqualsAndHashCode() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        List<String> accounts = List.of("ACC-001");
        
        DailySettlementRequest request1 = DailySettlementRequest.builder()
                .settleDate(settleDate)
                .accountIds(accounts)
                .build();
        
        DailySettlementRequest request2 = DailySettlementRequest.builder()
                .settleDate(settleDate)
                .accountIds(accounts)
                .build();
        
        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void testNotEquals() {
        DailySettlementRequest request1 = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .accountIds(List.of("ACC-001"))
                .build();
        
        DailySettlementRequest request2 = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 16))
                .accountIds(List.of("ACC-001"))
                .build();
        
        assertNotEquals(request1, request2);
    }

    @Test
    void testSettleDateNull() {
        DailySettlementRequest request = DailySettlementRequest.builder().build();
        assertNull(request.getSettleDate());
    }

    @Test
    void testAccountIdsNull() {
        DailySettlementRequest request = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .build();
        assertNull(request.getAccountIds());
    }

    @Test
    void testSetters() {
        DailySettlementRequest request = new DailySettlementRequest();
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        List<String> accounts = List.of("ACC-003");
        
        request.setSettleDate(settleDate);
        request.setAccountIds(accounts);
        
        assertEquals(settleDate, request.getSettleDate());
        assertEquals(accounts, request.getAccountIds());
    }
}