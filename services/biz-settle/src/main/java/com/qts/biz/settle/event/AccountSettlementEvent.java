package com.qts.biz.settle.event;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账户清算事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSettlementEvent {

    private String eventId;
    private String eventType;
    private LocalDateTime occurredAt;
    private String version;
    private Payload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payload {
        private String settleId;
        private String accountId;
        private LocalDate settleDate;
        private BigDecimal totalAssets;
        private BigDecimal availableCash;
        private BigDecimal frozenCash;
        private BigDecimal marketValue;
        private BigDecimal profitLoss;
        private BigDecimal commission;
        private BigDecimal stampDuty;
        private BigDecimal exchangeFee;
        private BigDecimal netProfitLoss;
    }
}
