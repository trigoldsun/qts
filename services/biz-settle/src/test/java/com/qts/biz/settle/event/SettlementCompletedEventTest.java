package com.qts.biz.settle.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SettlementCompletedEventTest {

    @Test
    void testBuilder() {
        LocalDateTime occurredAt = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        LocalDateTime completedAt = LocalDateTime.of(2024, 1, 15, 18, 35, 0);
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        
        SettlementCompletedEvent event = SettlementCompletedEvent.builder()
                .eventId("EVT-003")
                .eventType(SettlementCompletedEvent.EVENT_TYPE)
                .occurredAt(occurredAt)
                .version("1.0")
                .payload(SettlementCompletedEvent.Payload.builder()
                        .settleId("SETTLE-001")
                        .accountId("ACC-001")
                        .settleDate(settleDate)
                        .totalAssets(new BigDecimal("100000.0000"))
                        .availableCash(new BigDecimal("50000.0000"))
                        .frozenCash(new BigDecimal("10000.0000"))
                        .marketValue(new BigDecimal("40000.0000"))
                        .profitLoss(new BigDecimal("1000.0000"))
                        .commission(new BigDecimal("10.0000"))
                        .stampDuty(new BigDecimal("5.0000"))
                        .exchangeFee(new BigDecimal("3.0000"))
                        .netProfitLoss(new BigDecimal("982.0000"))
                        .completedAt(completedAt)
                        .build())
                .build();
        
        assertEquals("EVT-003", event.getEventId());
        assertEquals(SettlementCompletedEvent.EVENT_TYPE, event.getEventType());
        assertEquals(occurredAt, event.getOccurredAt());
        
        SettlementCompletedEvent.Payload payload = event.getPayload();
        assertEquals("SETTLE-001", payload.getSettleId());
        assertEquals("ACC-001", payload.getAccountId());
        assertEquals(completedAt, payload.getCompletedAt());
        assertEquals(new BigDecimal("982.0000"), payload.getNetProfitLoss());
    }

    @Test
    void testEventTypeConstant() {
        assertEquals("SETTLEMENT_COMPLETED", SettlementCompletedEvent.EVENT_TYPE);
    }

    @Test
    void testNoArgsConstructor() {
        SettlementCompletedEvent event = new SettlementCompletedEvent();
        assertNull(event.getEventId());
    }

    @Test
    void testEqualsAndHashCode() {
        SettlementCompletedEvent event1 = SettlementCompletedEvent.builder()
                .eventId("EVT-001")
                .eventType("SETTLEMENT_COMPLETED")
                .build();
        
        SettlementCompletedEvent event2 = SettlementCompletedEvent.builder()
                .eventId("EVT-001")
                .eventType("SETTLEMENT_COMPLETED")
                .build();
        
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testNotEquals() {
        SettlementCompletedEvent event1 = SettlementCompletedEvent.builder()
                .eventId("EVT-001")
                .build();
        
        SettlementCompletedEvent event2 = SettlementCompletedEvent.builder()
                .eventId("EVT-002")
                .build();
        
        assertNotEquals(event1, event2);
    }

    @Test
    void testToString() {
        SettlementCompletedEvent event = SettlementCompletedEvent.builder()
                .eventId("EVT-003")
                .eventType(SettlementCompletedEvent.EVENT_TYPE)
                .build();
        
        String str = event.toString();
        assertTrue(str.contains("EVT-003"));
        assertTrue(str.contains("SETTLEMENT_COMPLETED"));
    }

    @Test
    void testPayloadBuilder() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime completedAt = LocalDateTime.of(2024, 1, 15, 18, 35, 0);
        
        SettlementCompletedEvent.Payload payload = SettlementCompletedEvent.Payload.builder()
                .settleId("SETTLE-002")
                .accountId("ACC-002")
                .settleDate(settleDate)
                .completedAt(completedAt)
                .totalAssets(BigDecimal.valueOf(150000))
                .build();
        
        assertEquals("SETTLE-002", payload.getSettleId());
        assertEquals("ACC-002", payload.getAccountId());
        assertEquals(settleDate, payload.getSettleDate());
        assertEquals(completedAt, payload.getCompletedAt());
    }

    @Test
    void testPayloadEqualsAndHashCode() {
        SettlementCompletedEvent.Payload payload1 = SettlementCompletedEvent.Payload.builder()
                .settleId("SETTLE-001")
                .build();
        
        SettlementCompletedEvent.Payload payload2 = SettlementCompletedEvent.Payload.builder()
                .settleId("SETTLE-001")
                .build();
        
        assertEquals(payload1, payload2);
        assertEquals(payload1.hashCode(), payload2.hashCode());
    }

    @Test
    void testPayloadAllFields() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        LocalDateTime completedAt = LocalDateTime.of(2024, 1, 15, 18, 35, 0);
        
        SettlementCompletedEvent.Payload payload = SettlementCompletedEvent.Payload.builder()
                .settleId("SETTLE-003")
                .accountId("ACC-003")
                .settleDate(settleDate)
                .totalAssets(new BigDecimal("300000.0000"))
                .availableCash(new BigDecimal("100000.0000"))
                .frozenCash(new BigDecimal("50000.0000"))
                .marketValue(new BigDecimal("150000.0000"))
                .profitLoss(new BigDecimal("5000.0000"))
                .commission(new BigDecimal("50.0000"))
                .stampDuty(new BigDecimal("25.0000"))
                .exchangeFee(new BigDecimal("15.0000"))
                .netProfitLoss(new BigDecimal("4910.0000"))
                .completedAt(completedAt)
                .build();
        
        assertEquals(new BigDecimal("300000.0000"), payload.getTotalAssets());
        assertEquals(new BigDecimal("4910.0000"), payload.getNetProfitLoss());
    }
}