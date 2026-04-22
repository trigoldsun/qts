package com.qts.biz.settle.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SettlementLogEntityTest {

    @Test
    void testBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        
        SettlementLogEntity entity = SettlementLogEntity.builder()
                .logId("LOG-001")
                .settleId("SETTLE-001")
                .accountId("ACC-001")
                .settleDate(settleDate)
                .settleType(SettlementLogEntity.SettlementType.ACCOUNT_BALANCE)
                .symbol("600000")
                .changeType("BUY")
                .amountBefore(new BigDecimal("10000.0000"))
                .amountAfter(new BigDecimal("9000.0000"))
                .quantityBefore(100)
                .quantityAfter(200)
                .profitLoss(new BigDecimal("500.0000"))
                .commission(new BigDecimal("10.0000"))
                .stampDuty(new BigDecimal("5.0000"))
                .exchangeFee(new BigDecimal("3.0000"))
                .description("Position updated")
                .createdAt(LocalDateTime.now())
                .build();
        
        assertEquals("LOG-001", entity.getLogId());
        assertEquals("SETTLE-001", entity.getSettleId());
        assertEquals("ACC-001", entity.getAccountId());
        assertEquals(settleDate, entity.getSettleDate());
        assertEquals(SettlementLogEntity.SettlementType.ACCOUNT_BALANCE, entity.getSettleType());
        assertEquals("600000", entity.getSymbol());
        assertEquals("BUY", entity.getChangeType());
        assertEquals(new BigDecimal("10000.0000"), entity.getAmountBefore());
        assertEquals(new BigDecimal("9000.0000"), entity.getAmountAfter());
        assertEquals(100, entity.getQuantityBefore());
        assertEquals(200, entity.getQuantityAfter());
        assertEquals(new BigDecimal("500.0000"), entity.getProfitLoss());
    }

    @Test
    void testNoArgsConstructor() {
        SettlementLogEntity entity = new SettlementLogEntity();
        assertNull(entity.getLogId());
        assertNull(entity.getSettleId());
    }

    @Test
    void testEqualsAndHashCode() {
        SettlementLogEntity entity1 = SettlementLogEntity.builder()
                .logId("LOG-001")
                .build();
        
        SettlementLogEntity entity2 = SettlementLogEntity.builder()
                .logId("LOG-001")
                .build();
        
        assertEquals(entity1, entity2);
        assertEquals(entity1.hashCode(), entity2.hashCode());
    }

    @Test
    void testNotEquals() {
        SettlementLogEntity entity1 = SettlementLogEntity.builder()
                .logId("LOG-001")
                .build();
        
        SettlementLogEntity entity2 = SettlementLogEntity.builder()
                .logId("LOG-002")
                .build();
        
        assertNotEquals(entity1, entity2);
    }

    @Test
    void testToString() {
        SettlementLogEntity entity = SettlementLogEntity.builder()
                .logId("LOG-001")
                .changeType("SELL")
                .build();
        
        String str = entity.toString();
        assertTrue(str.contains("LOG-001"));
        assertTrue(str.contains("SELL"));
    }

    @Test
    void testSettlementType() {
        assertEquals(5, SettlementLogEntity.SettlementType.values().length);
        assertEquals(SettlementLogEntity.SettlementType.ACCOUNT_BALANCE, 
                SettlementLogEntity.SettlementType.valueOf("ACCOUNT_BALANCE"));
        assertEquals(SettlementLogEntity.SettlementType.POSITION, 
                SettlementLogEntity.SettlementType.valueOf("POSITION"));
        assertEquals(SettlementLogEntity.SettlementType.PROFIT_LOSS, 
                SettlementLogEntity.SettlementType.valueOf("PROFIT_LOSS"));
        assertEquals(SettlementLogEntity.SettlementType.COMMISSION, 
                SettlementLogEntity.SettlementType.valueOf("COMMISSION"));
        assertEquals(SettlementLogEntity.SettlementType.INTEREST, 
                SettlementLogEntity.SettlementType.valueOf("INTEREST"));
    }

    @Test
    void testAllSettlementTypes() {
        for (SettlementLogEntity.SettlementType type : SettlementLogEntity.SettlementType.values()) {
            SettlementLogEntity entity = SettlementLogEntity.builder()
                    .logId("LOG-" + type.name())
                    .settleType(type)
                    .build();
            assertEquals(type, entity.getSettleType());
        }
    }

    @Test
    void testSetters() {
        SettlementLogEntity entity = new SettlementLogEntity();
        entity.setLogId("LOG-002");
        entity.setSettleType(SettlementLogEntity.SettlementType.PROFIT_LOSS);
        entity.setCommission(new BigDecimal("15.0000"));
        
        assertEquals("LOG-002", entity.getLogId());
        assertEquals(SettlementLogEntity.SettlementType.PROFIT_LOSS, entity.getSettleType());
        assertEquals(new BigDecimal("15.0000"), entity.getCommission());
    }
}