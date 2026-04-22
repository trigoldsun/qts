package com.qts.biz.settle.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AccountSettlementEventTest {

    @Test
    void testBuilder() {
        LocalDateTime occurredAt = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        
        AccountSettlementEvent event = AccountSettlementEvent.builder()
                .eventId("EVT-002")
                .eventType("ACCOUNT_SETTLEMENT")
                .occurredAt(occurredAt)
                .version("1.0")
                .payload(AccountSettlementEvent.Payload.builder()
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
                        .build())
                .build();
        
        assertEquals("EVT-002", event.getEventId());
        assertEquals("ACCOUNT_SETTLEMENT", event.getEventType());
        assertEquals(occurredAt, event.getOccurredAt());
        
        AccountSettlementEvent.Payload payload = event.getPayload();
        assertEquals("SETTLE-001", payload.getSettleId());
        assertEquals("ACC-001", payload.getAccountId());
        assertEquals(new BigDecimal("100000.0000"), payload.getTotalAssets());
        assertEquals(new BigDecimal("50000.0000"), payload.getAvailableCash());
        assertEquals(new BigDecimal("10000.0000"), payload.getFrozenCash());
    }

    @Test
    void testNoArgsConstructor() {
        AccountSettlementEvent event = new AccountSettlementEvent();
        assertNull(event.getEventId());
    }

    @Test
    void testEqualsAndHashCode() {
        AccountSettlementEvent event1 = AccountSettlementEvent.builder()
                .eventId("EVT-001")
                .eventType("ACCOUNT")
                .build();
        
        AccountSettlementEvent event2 = AccountSettlementEvent.builder()
                .eventId("EVT-001")
                .eventType("ACCOUNT")
                .build();
        
        assertEquals(event1, event2);
        assertEquals(event1.hashCode(), event2.hashCode());
    }

    @Test
    void testNotEquals() {
        AccountSettlementEvent event1 = AccountSettlementEvent.builder()
                .eventId("EVT-001")
                .build();
        
        AccountSettlementEvent event2 = AccountSettlementEvent.builder()
                .eventId("EVT-002")
                .build();
        
        assertNotEquals(event1, event2);
    }

    @Test
    void testToString() {
        AccountSettlementEvent event = AccountSettlementEvent.builder()
                .eventId("EVT-002")
                .eventType("ACCOUNT")
                .build();
        
        String str = event.toString();
        assertTrue(str.contains("EVT-002"));
    }

    @Test
    void testPayloadAllFields() {
        LocalDate settleDate = LocalDate.of(2024, 1, 15);
        AccountSettlementEvent.Payload payload = AccountSettlementEvent.Payload.builder()
                .settleId("SETTLE-002")
                .accountId("ACC-002")
                .settleDate(settleDate)
                .totalAssets(new BigDecimal("200000.0000"))
                .availableCash(new BigDecimal("80000.0000"))
                .frozenCash(new BigDecimal("20000.0000"))
                .marketValue(new BigDecimal("100000.0000"))
                .profitLoss(new BigDecimal("2000.0000"))
                .commission(new BigDecimal("20.0000"))
                .stampDuty(new BigDecimal("10.0000"))
                .exchangeFee(new BigDecimal("6.0000"))
                .netProfitLoss(new BigDecimal("1964.0000"))
                .build();
        
        assertEquals("SETTLE-002", payload.getSettleId());
        assertEquals("ACC-002", payload.getAccountId());
        assertEquals(settleDate, payload.getSettleDate());
        assertEquals(new BigDecimal("200000.0000"), payload.getTotalAssets());
        assertEquals(new BigDecimal("1964.0000"), payload.getNetProfitLoss());
    }

    @Test
    void testPayloadEqualsAndHashCode() {
        AccountSettlementEvent.Payload payload1 = AccountSettlementEvent.Payload.builder()
                .settleId("SETTLE-001")
                .build();
        
        AccountSettlementEvent.Payload payload2 = AccountSettlementEvent.Payload.builder()
                .settleId("SETTLE-001")
                .build();
        
        assertEquals(payload1, payload2);
        assertEquals(payload1.hashCode(), payload2.hashCode());
    }
}