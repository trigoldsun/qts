package com.qts.biz.settle.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReconcileRecordEntityTest {

    @Test
    void testBuilder() {
        LocalDateTime reconcileTime = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        
        ReconcileRecordEntity entity = ReconcileRecordEntity.builder()
                .reconcileId("REC-001")
                .accountId("ACC-001")
                .reconcileTime(reconcileTime)
                .assetStatus(ReconcileRecordEntity.ReconcileStatus.MATCH)
                .positionStatus(ReconcileRecordEntity.ReconcileStatus.MATCH)
                .systemAvailableCash(new BigDecimal("50000.0000"))
                .brokerAvailableCash(new BigDecimal("50000.0000"))
                .cashDifference(BigDecimal.ZERO)
                .systemMarketValue(new BigDecimal("40000.0000"))
                .brokerMarketValue(new BigDecimal("40000.0000"))
                .marketValueDifference(BigDecimal.ZERO)
                .differenceCount(0)
                .differenceDetails(null)
                .createdAt(LocalDateTime.now())
                .build();
        
        assertEquals("REC-001", entity.getReconcileId());
        assertEquals("ACC-001", entity.getAccountId());
        assertEquals(reconcileTime, entity.getReconcileTime());
        assertEquals(ReconcileRecordEntity.ReconcileStatus.MATCH, entity.getAssetStatus());
        assertEquals(ReconcileRecordEntity.ReconcileStatus.MATCH, entity.getPositionStatus());
        assertEquals(BigDecimal.ZERO, entity.getCashDifference());
        assertEquals(0, entity.getDifferenceCount());
    }

    @Test
    void testNoArgsConstructor() {
        ReconcileRecordEntity entity = new ReconcileRecordEntity();
        assertNull(entity.getReconcileId());
        assertNull(entity.getAccountId());
    }

    @Test
    void testEqualsAndHashCode() {
        ReconcileRecordEntity entity1 = ReconcileRecordEntity.builder()
                .reconcileId("REC-001")
                .build();
        
        ReconcileRecordEntity entity2 = ReconcileRecordEntity.builder()
                .reconcileId("REC-001")
                .build();
        
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testNotEquals() {
        ReconcileRecordEntity entity1 = ReconcileRecordEntity.builder()
                .reconcileId("REC-001")
                .build();
        
        ReconcileRecordEntity entity2 = ReconcileRecordEntity.builder()
                .reconcileId("REC-002")
                .build();
        
        assertNotEquals(entity1, entity2);
    }

    @Test
    void testToString() {
        ReconcileRecordEntity entity = ReconcileRecordEntity.builder()
                .reconcileId("REC-001")
                .assetStatus(ReconcileRecordEntity.ReconcileStatus.MATCH)
                .build();
        
        String str = entity.toString();
        assertTrue(str.contains("REC-001"));
    }

    @Test
    void testReconcileStatus() {
        assertEquals(3, ReconcileRecordEntity.ReconcileStatus.values().length);
        assertEquals(ReconcileRecordEntity.ReconcileStatus.MATCH, 
                ReconcileRecordEntity.ReconcileStatus.valueOf("MATCH"));
        assertEquals(ReconcileRecordEntity.ReconcileStatus.DISCARD, 
                ReconcileRecordEntity.ReconcileStatus.valueOf("DISCARD"));
        assertEquals(ReconcileRecordEntity.ReconcileStatus.PENDING, 
                ReconcileRecordEntity.ReconcileStatus.valueOf("PENDING"));
    }

    @Test
    void testDiscardStatus() {
        ReconcileRecordEntity entity = ReconcileRecordEntity.builder()
                .reconcileId("REC-DIFF")
                .assetStatus(ReconcileRecordEntity.ReconcileStatus.DISCARD)
                .positionStatus(ReconcileRecordEntity.ReconcileStatus.DISCARD)
                .differenceCount(5)
                .differenceDetails("5 differences found")
                .build();
        
        assertEquals(ReconcileRecordEntity.ReconcileStatus.DISCARD, entity.getAssetStatus());
        assertEquals(5, entity.getDifferenceCount());
        assertEquals("5 differences found", entity.getDifferenceDetails());
    }

    @Test
    void testPendingStatus() {
        ReconcileRecordEntity entity = ReconcileRecordEntity.builder()
                .reconcileId("REC-PENDING")
                .assetStatus(ReconcileRecordEntity.ReconcileStatus.PENDING)
                .build();
        
        assertEquals(ReconcileRecordEntity.ReconcileStatus.PENDING, entity.getAssetStatus());
    }
}