package com.qts.biz.settle.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PositionSettlementEventTest {

    @Test
    void testBuilder() {
        LocalDateTime occurredAt = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        
        PositionSettlementEvent event = PositionSettlementEvent.builder()
                .eventId("EVT-001")
                .eventType("POSITION_SETTLEMENT")
                .occurredAt(occurredAt)
                .version("1.0")
                .payload(PositionSettlementEvent.Payload.builder()
                        .settleId("SETTLE-001")
                        .accountId("ACC-001")
                        .settleDate(settleDate)
                        .symbol("600000")
                        .quantity(100)
                        .costPrice(new BigDecimal("10.0000"))
                        .marketPrice(new BigDecimal("11.0000"))
                        .profitLoss(new BigDecimal("100.0000"))
                        .commission(new BigDecimal("10.0000"))
                        .stampDuty(new BigDecimal("5.0000"))
                        .exchangeFee(new BigDecimal("3.0000"))
                        .build())
                .build();
        
        assertEquals("EVT-001", event.getEventId());
        assertEquals("POSITION_SETTLEMENT", event.getEventType());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals("1.0", event.getVersion());
        
        PositionSettlementEvent.Payload payload = event.getPayload();
        assertEquals("SETTLE-001", payload.getSettleId());
        assertEquals("ACC-001", payload.getAccountId());
        assertEquals(settleDate, payload.getSettleDate());
        assertEquals("600000", payload.getSymbol());
        assertEquals(100, payload.getQuantity());
        assertEquals(new BigDecimal("10.0000"), payload.getCostPrice());
        assertEquals(new BigDecimal("11.0000"), payload.getMarketPrice());
    }

    @Test
    void testNoArgsConstructor() {
        PositionSettlementEvent event = new PositionSettlementEvent();
        assertNull(event.getEventId());
        assertNull(event.getEventType());
    }

    @Test
    void testEqualsAndHashCode() {
        PositionSettlementEvent event1 = PositionSettlementEvent.builder()
                .eventId("EVT-001")
                .eventType("POSITION")
                .build();
        
        PositionSettlementEvent event2 = PositionSettlementEvent.builder()
                .eventId("EVT-001")
                .eventType("POSITION")
                .build();
        
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testNotEquals() {
        PositionSettlementEvent event1 = PositionSettlementEvent.builder()
                .eventId("EVT-001")
                .build();
        
        PositionSettlementEvent event2 = PositionSettlementEvent.builder()
                .eventId("EVT-002")
                .build();
        
        assertNotEquals(event1, event2);
    }

    @Test
    void testToString() {
        PositionSettlementEvent event = PositionSettlementEvent.builder()
                .eventId("EVT-001")
                .eventType("POSITION")
                .build();
        
        String str = event.toString();
        assertTrue(str.contains("EVT-001"));
        assertTrue(str.contains("POSITION"));
    }

    @Test
    void testPayloadBuilder() {
        PositionSettlementEvent.Payload payload = PositionSettlementEvent.Payload.builder()
                .settleId("SETTLE-001")
                .accountId("ACC-001")
                .symbol("600001")
                .quantity(200)
                .build();
        
        assertEquals("SETTLE-001", payload.getSettleId());
        assertEquals("ACC-001", payload.getAccountId());
        assertEquals("600001", payload.getSymbol());
        assertEquals(200, payload.getQuantity());
    }

    @Test
    void testPayloadEqualsAndHashCode() {
        PositionSettlementEvent.Payload payload1 = PositionSettlementEvent.Payload.builder()
                .settleId("SETTLE-001")
                .build();
        
        PositionSettlementEvent.Payload payload2 = PositionSettlementEvent.Payload.builder()
                .settleId("SETTLE-001")
                .build();
        
        assertEquals(payload1, payload2);
    }

    @Test
    void testSetters() {
        PositionSettlementEvent event = new PositionSettlementEvent();
        event.setEventId("EVT-002");
        event.setEventType("NEW_POSITION");
        
        assertEquals("EVT-002", event.getEventId());
        assertEquals("NEW_POSITION", event.getEventType());
    }
}