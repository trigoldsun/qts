package com.qts.biz.settle.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileResult {

    private String reconcileId;
    private String accountId;
    private LocalDateTime reconcileTime;
    private String assetStatus;
    private String positionStatus;
    private Integer differenceCount;
    private BigDecimal cashDifference;
    private BigDecimal marketValueDifference;
}
