package com.qts.biz.settle.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StatementDTOTest {

    @Test
    void testBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        
        StatementDTO dto = StatementDTO.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .settleDate(settleDate)
                .statementType("DAILY")
                .totalAssetsStart(new BigDecimal("100000.0000"))
                .totalAssetsEnd(new BigDecimal("101000.0000"))
                .availableCash(new BigDecimal("50000.0000"))
                .frozenCash(new BigDecimal("10000.0000"))
                .marketValue(new BigDecimal("40000.0000"))
                .totalProfitLoss(new BigDecimal("1000.0000"))
                .todayProfitLoss(new BigDecimal("500.0000"))
                .totalCommission(new BigDecimal("10.0000"))
                .totalStampDuty(new BigDecimal("5.0000"))
                .totalExchangeFee(new BigDecimal("3.0000"))
                .netProfitLoss(new BigDecimal("982.0000"))
                .createdAt(createdAt)
                .build();
        
        assertEquals("STMT-001", dto.getStatementId());
        assertEquals("ACC-001", dto.getAccountId());
        assertEquals(settleDate, dto.getSettleDate());
        assertEquals("DAILY", dto.getStatementType());
        assertEquals(new BigDecimal("100000.0000"), dto.getTotalAssetsStart());
        assertEquals(new BigDecimal("101000.0000"), dto.getTotalAssetsEnd());
        assertEquals(new BigDecimal("50000.0000"), dto.getAvailableCash());
        assertEquals(new BigDecimal("10000.0000"), dto.getFrozenCash());
        assertEquals(new BigDecimal("40000.0000"), dto.getMarketValue());
        assertEquals(new BigDecimal("1000.0000"), dto.getTotalProfitLoss());
        assertEquals(new BigDecimal("500.0000"), dto.getTodayProfitLoss());
        assertEquals(new BigDecimal("10.0000"), dto.getTotalCommission());
        assertEquals(new BigDecimal("5.0000"), dto.getTotalStampDuty());
        assertEquals(new BigDecimal("3.0000"), dto.getTotalExchangeFee());
        assertEquals(new BigDecimal("982.0000"), dto.getNetProfitLoss());
        assertEquals(createdAt, dto.getCreatedAt());
    }

    @Test
    void testNoArgsConstructor() {
        StatementDTO dto = new StatementDTO();
        assertNull(dto.getStatementId());
        assertNull(dto.getAccountId());
    }

    @Test
    void testEqualsAndHashCode() {
        StatementDTO dto1 = StatementDTO.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        StatementDTO dto2 = StatementDTO.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        assertEquals(dto1, dto2);
        assertEquals(dto1.hashCode(), dto2.hashCode());
    }

    @Test
    void testNotEquals() {
        StatementDTO dto1 = StatementDTO.builder()
                .statementId("STMT-001")
                .build();
        
        StatementDTO dto2 = StatementDTO.builder()
                .statementId("STMT-002")
                .build();
        
        assertNotEquals(dto1, dto2);
    }

    @Test
    void testToString() {
        StatementDTO dto = StatementDTO.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        String str = dto.toString();
        assertTrue(str.contains("STMT-001"));
        assertTrue(str.contains("ACC-001"));
    }

    @Test
    void testSetters() {
        StatementDTO dto = new StatementDTO();
        dto.setStatementId("STMT-002");
        dto.setAccountId("ACC-002");
        
        assertEquals("STMT-002", dto.getStatementId());
        assertEquals("ACC-002", dto.getAccountId());
    }
}