package com.qts.biz.settle.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementDTO {

    private String statementId;
    private String accountId;
    private LocalDate settleDate;
    private String statementType;
    private BigDecimal totalAssetsStart;
    private BigDecimal totalAssetsEnd;
    private BigDecimal availableCash;
    private BigDecimal frozenCash;
    private BigDecimal marketValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal todayProfitLoss;
    private BigDecimal totalCommission;
    private BigDecimal totalStampDuty;
    private BigDecimal totalExchangeFee;
    private BigDecimal netProfitLoss;
    private LocalDateTime createdAt;
}
