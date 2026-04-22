package com.qts.biz.settle.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日终清算响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySettlementResponse {

    private String settleId;
    private LocalDate settleDate;
    private String status;
    private LocalDateTime startedAt;
    private Integer accountsCount;
}
