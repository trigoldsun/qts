package com.qts.biz.settle.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReconcileResultTest {

    @Test
    void testBuilder() {
        LocalDateTime reconcileTime = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        
        ReconcileResult result = ReconcileResult.builder()
                .reconcileId("REC-001")
                .accountId("ACC-001")
                .reconcileTime(reconcileTime)
                .assetStatus("MATCH")
                .positionStatus("MATCH")
                .differenceCount(0)
                .cashDifference(BigDecimal.ZERO)
                .marketValueDifference(BigDecimal.ZERO)
                .build();
        
        assertEquals("REC-001", result.getReconcileId());
        assertEquals("ACC-001", result.getAccountId());
        assertEquals(reconcileTime, result.getReconcileTime());
        assertEquals("MATCH", result.getAssetStatus());
        assertEquals("MATCH", result.getPositionStatus());
        assertEquals(0, result.getDifferenceCount());
        assertEquals(BigDecimal.ZERO, result.getCashDifference());
        assertEquals(BigDecimal.ZERO, result.getMarketValueDifference());
    }

    @Test
    void testNoArgsConstructor() {
        ReconcileResult result = new ReconcileResult();
        assertNull(result.getReconcileId());
        assertNull(result.getAccountId());
    }

    @Test
    void testEqualsAndHashCode() {
        ReconcileResult result1 = ReconcileResult.builder()
                .reconcileId("REC-001")
                .accountId("ACC-001")
                .assetStatus("MATCH")
                .build();
        
        ReconcileResult result2 = ReconcileResult.builder()
                .reconcileId("REC-001")
                .accountId("ACC-001")
                .assetStatus("MATCH")
                .build();
        
        assertEquals(result1, result2);
        assertEquals(result1.hashCode(), result2.hashCode());
    }

    @Test
    void testNotEquals() {
        ReconcileResult result1 = ReconcileResult.builder()
                .reconcileId("REC-001")
                .build();
        
        ReconcileResult result2 = ReconcileResult.builder()
                .reconcileId("REC-002")
                .build();
        
        assertNotEquals(result1, result2);
    }

    @Test
    void testToString() {
        ReconcileResult result = ReconcileResult.builder()
                .reconcileId("REC-001")
                .assetStatus("MATCH")
                .build();
        
        String str = result.toString();
        assertTrue(str.contains("REC-001"));
        assertTrue(str.contains("MATCH"));
    }
}