package com.qts.biz.settle.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class StatementEntityTest {

    @Test
    void testBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        
        StatementEntity entity = StatementEntity.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .settleDate(settleDate)
                .statementType(StatementEntity.StatementType.DAILY)
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
                .positionsJson("[{\"symbol\":\"600000\"}]")
                .tradesJson("[{\"tradeId\":\"T001\"}]")
                .createdAt(createdAt)
                .build();
        
        assertEquals("STMT-001", entity.getStatementId());
        assertEquals("ACC-001", entity.getAccountId());
        assertEquals(settleDate, entity.getSettleDate());
        assertEquals(StatementEntity.StatementType.DAILY, entity.getStatementType());
        assertEquals(new BigDecimal("100000.0000"), entity.getTotalAssetsStart());
        assertEquals(new BigDecimal("101000.0000"), entity.getTotalAssetsEnd());
        assertEquals("[{\"symbol\":\"600000\"}]", entity.getPositionsJson());
    }

    @Test
    void testNoArgsConstructor() {
        StatementEntity entity = new StatementEntity();
        assertNull(entity.getStatementId());
        assertNull(entity.getAccountId());
    }

    @Test
    void testEqualsAndHashCode() {
        StatementEntity entity1 = StatementEntity.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        StatementEntity entity2 = StatementEntity.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testNotEquals() {
        StatementEntity entity1 = StatementEntity.builder()
                .statementId("STMT-001")
                .build();
        
        StatementEntity entity2 = StatementEntity.builder()
                .statementId("STMT-002")
                .build();
        
        assertNotEquals(entity1, entity2);
    }

    @Test
    void testToString() {
        StatementEntity entity = StatementEntity.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .statementType(StatementEntity.StatementType.DAILY)
                .build();
        
        String str = entity.toString();
        assertTrue(str.contains("STMT-001"));
        assertTrue(str.contains("DAILY"));
    }

    @Test
    void testStatementType() {
        assertEquals(2, StatementEntity.StatementType.values().length);
        assertEquals(StatementEntity.StatementType.DAILY, 
                StatementEntity.StatementType.valueOf("DAILY"));
        assertEquals(StatementEntity.StatementType.MONTHLY, 
                StatementEntity.StatementType.valueOf("MONTHLY"));
    }

    @Test
    void testSetters() {
        StatementEntity entity = new StatementEntity();
        entity.setStatementId("STMT-002");
        entity.setStatementType(StatementEntity.StatementType.MONTHLY);
        
        assertEquals("STMT-002", entity.getStatementId());
        assertEquals(StatementEntity.StatementType.MONTHLY, entity.getStatementType());
    }

    @Test
    void testMonthlyStatementType() {
        StatementEntity entity = StatementEntity.builder()
                .statementId("STMT-MONTHLY")
                .statementType(StatementEntity.StatementType.MONTHLY)
                .build();
        
        assertEquals(StatementEntity.StatementType.MONTHLY, entity.getStatementType());
    }
}