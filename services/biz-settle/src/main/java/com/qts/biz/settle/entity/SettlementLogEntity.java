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
 * 清算日志实体
 * 记录每笔清算的详细变更
 */
@Entity
@Table(name = "settlement_logs", schema = "settle")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementLogEntity {

    @Id
    @Column(name = "log_id", length = 32)
    private String logId;

    @Column(name = "settle_id", nullable = false, length = 32)
    private String settleId;

    @Column(name = "account_id", nullable = false, length = 32)
    private String accountId;

    @Column(name = "settle_date", nullable = false)
    private LocalDate settleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "settle_type", nullable = false, length = 20)
    private SettlementType settleType;

    @Column(name = "symbol", length = 16)
    private String symbol;

    @Column(name = "change_type", nullable = false, length = 50)
    private String changeType;

    @Column(name = "amount_before", precision = 18, scale = 4)
    private BigDecimal amountBefore;

    @Column(name = "amount_after", precision = 18, scale = 4)
    private BigDecimal amountAfter;

    @Column(name = "quantity_before")
    private Integer quantityBefore;

    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @Column(name = "profit_loss", precision = 18, scale = 4)
    private BigDecimal profitLoss;

    @Column(name = "commission", precision = 12, scale = 4)
    private BigDecimal commission;

    @Column(name = "stamp_duty", precision = 12, scale = 4)
    private BigDecimal stampDuty;

    @Column(name = "exchange_fee", precision = 12, scale = 4)
    private BigDecimal exchangeFee;

    @Column(name = "description", length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum SettlementType {
        ACCOUNT_BALANCE,  // 账户余额清算
        POSITION,          // 持仓清算
        PROFIT_LOSS,       // 盈亏清算
        COMMISSION,        // 佣金费用
        INTEREST          // 利息
    }
}
