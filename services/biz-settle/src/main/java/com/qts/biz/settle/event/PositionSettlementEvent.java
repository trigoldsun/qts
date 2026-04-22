package com.qts.biz.settle.event;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 持仓清算事件（来自BIZ-TRADE）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionSettlementEvent {

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
        private String symbol;
        private Integer quantity;
        private BigDecimal costPrice;
        private BigDecimal marketPrice;
        private BigDecimal profitLoss;
        private BigDecimal commission;
        private BigDecimal stampDuty;
        private BigDecimal exchangeFee;
    }
}
