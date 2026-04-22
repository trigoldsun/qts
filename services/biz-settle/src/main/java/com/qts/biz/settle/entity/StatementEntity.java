package com.qts.biz.settle.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单实体
 * 日结单/月结单
 */
@Entity
@Table(name = "statements", schema = "settle")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatementEntity {

    @Id
    @Column(name = "statement_id", length = 32)
    private String statementId;

    @Column(name = "account_id", nullable = false, length = 32)
    private String accountId;

    @Column(name = "settle_date", nullable = false)
    private LocalDate settleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statement_type", nullable = false, length = 10)
    private StatementType statementType;

    @Column(name = "total_assets_start", precision = 18, scale = 4)
    private BigDecimal totalAssetsStart;

    @Column(name = "total_assets_end", precision = 18, scale = 4)
    private BigDecimal totalAssetsEnd;

    @Column(name = "available_cash", precision = 18, scale = 4)
    private BigDecimal availableCash;

    @Column(name = "frozen_cash", precision = 18, scale = 4)
    private BigDecimal frozenCash;

    @Column(name = "market_value", precision = 18, scale = 4)
    private BigDecimal marketValue;

    @Column(name = "total_profit_loss", precision = 18, scale = 4)
    private BigDecimal totalProfitLoss;

    @Column(name = "today_profit_loss", precision = 18, scale = 4)
    private BigDecimal todayProfitLoss;

    @Column(name = "total_commission", precision = 12, scale = 4)
    private BigDecimal totalCommission;

    @Column(name = "total_stamp_duty", precision = 12, scale = 4)
    private BigDecimal totalStampDuty;

    @Column(name = "total_exchange_fee", precision = 12, scale = 4)
    private BigDecimal totalExchangeFee;

    @Column(name = "net_profit_loss", precision = 18, scale = 4)
    private BigDecimal netProfitLoss;

    @Column(name = "positions_json", columnDefinition = "TEXT")
    private String positionsJson;

    @Column(name = "trades_json", columnDefinition = "TEXT")
    private String tradesJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum StatementType {
        DAILY,   // 日结单
        MONTHLY  // 月结单
    }
}
