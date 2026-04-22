package com.qts.biz.settle.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SettlementTaskEntityTest {

    @Test
    void testBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime startedAt = LocalDateTime.of(2024, 1, 15, 18, 0, 0);
        
        SettlementTaskEntity entity = SettlementTaskEntity.builder()
                .settleId("SETTLE-001")
                .settleDate(settleDate)
                .status(SettlementTaskEntity.SettlementStatus.PENDING)
                .startedAt(startedAt)
                .accountsCount(10)
                .processedCount(5)
                .failedCount(1)
                .totalAssetsStart(new BigDecimal("100000.0000"))
                .totalAssetsEnd(new BigDecimal("101000.0000"))
                .errorMessage(null)
                .createdBy("system")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        assertEquals("SETTLE-001", entity.getSettleId());
        assertEquals(settleDate, entity.getSettleDate());
        assertEquals(SettlementTaskEntity.SettlementStatus.PENDING, entity.getStatus());
        assertEquals(startedAt, entity.getStartedAt());
        assertEquals(10, entity.getAccountsCount());
        assertEquals(5, entity.getProcessedCount());
        assertEquals(1, entity.getFailedCount());
        assertEquals(new BigDecimal("100000.0000"), entity.getTotalAssetsStart());
        assertEquals(new BigDecimal("101000.0000"), entity.getTotalAssetsEnd());
        assertNull(entity.getErrorMessage());
        assertEquals("system", entity.getCreatedBy());
    }

    @Test
    void testNoArgsConstructor() {
        SettlementTaskEntity entity = new SettlementTaskEntity();
        assertNull(entity.getSettleId());
        assertNull(entity.getSettleDate());
        assertNull(entity.getStatus());
    }

    @Test
    void testAllArgsConstructor() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        
        SettlementTaskEntity entity = new SettlementTaskEntity(
                "SETTLE-002",
                settleDate,
                SettlementTaskEntity.SettlementStatus.PROCESSING,
                LocalDateTime.now(),
                LocalDateTime.now(),
                5,
                3,
                1,
                BigDecimal.valueOf(50000),
                BigDecimal.valueOf(51000),
                "some error",
                "admin",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
        
        assertEquals("SETTLE-002", entity.getSettleId());
        assertEquals(settleDate, entity.getSettleDate());
        assertEquals(SettlementTaskEntity.SettlementStatus.PROCESSING, entity.getStatus());
        assertEquals(5, entity.getAccountsCount());
    }

    @Test
    void testEqualsAndHashCode() {
        SettlementTaskEntity entity1 = SettlementTaskEntity.builder()
                .settleId("SETTLE-001")
                .build();
        
        SettlementTaskEntity entity2 = SettlementTaskEntity.builder()
                .settleId("SETTLE-001")
                .build();
        
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testNotEquals() {
        SettlementTaskEntity entity1 = SettlementTaskEntity.builder()
                .settleId("SETTLE-001")
                .build();
        
        SettlementTaskEntity entity2 = SettlementTaskEntity.builder()
                .settleId("SETTLE-002")
                .build();
        
        assertNotEquals(entity1, entity2);
    }

    @Test
    void testToString() {
        SettlementTaskEntity entity = SettlementTaskEntity.builder()
                .settleId("SETTLE-001")
                .status(SettlementTaskEntity.SettlementStatus.COMPLETED)
                .build();
        
        String str = entity.toString();
        assertTrue(str.contains("SETTLE-001"));
        assertTrue(str.contains("COMPLETED"));
    }

    @Test
    void testSettlementStatus() {
        assertEquals(4, SettlementTaskEntity.SettlementStatus.values().length);
        assertEquals(SettlementTaskEntity.SettlementStatus.PENDING, 
                SettlementTaskEntity.SettlementStatus.valueOf("PENDING"));
        assertEquals(SettlementTaskEntity.SettlementStatus.PROCESSING, 
                SettlementTaskEntity.SettlementStatus.valueOf("PROCESSING"));
        assertEquals(SettlementTaskEntity.SettlementStatus.COMPLETED, 
                SettlementTaskEntity.SettlementStatus.valueOf("COMPLETED"));
        assertEquals(SettlementTaskEntity.SettlementStatus.FAILED, 
                SettlementTaskEntity.SettlementStatus.valueOf("FAILED"));
    }

    @Test
    void testSetters() {
        SettlementTaskEntity entity = new SettlementTaskEntity();
        entity.setSettleId("SETTLE-003");
        entity.setStatus(SettlementTaskEntity.SettlementStatus.FAILED);
        entity.setErrorMessage("Database error");
        
        assertEquals("SETTLE-003", entity.getSettleId());
        assertEquals(SettlementTaskEntity.SettlementStatus.FAILED, entity.getStatus());
        assertEquals("Database error", entity.getErrorMessage());
    }
}