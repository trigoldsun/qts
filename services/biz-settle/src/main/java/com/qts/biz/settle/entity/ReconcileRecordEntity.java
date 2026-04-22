package com.qts.biz.settle.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 对账记录实体
 * 实时对账的结果
 */
@Entity
@Table(name = "reconcile_records", schema = "settle")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileRecordEntity {

    @Id
    @Column(name = "reconcile_id", length = 32)
    private String reconcileId;

    @Column(name = "account_id", nullable = false, length = 32)
    private String accountId;

    @Column(name = "reconcile_time", nullable = false)
    private LocalDateTime reconcileTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_status", nullable = false, length = 20)
    private ReconcileStatus assetStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "position_status", nullable = false, length = 20)
    private ReconcileStatus positionStatus;

    @Column(name = "system_available_cash", precision = 18, scale = 4)
    private BigDecimal systemAvailableCash;

    @Column(name = "broker_available_cash", precision = 18, scale = 4)
    private BigDecimal brokerAvailableCash;

    @Column(name = "cash_difference", precision = 18, scale = 4)
    private BigDecimal cashDifference;

    @Column(name = "system_market_value", precision = 18, scale = 4)
    private BigDecimal systemMarketValue;

    @Column(name = "broker_market_value", precision = 18, scale = 4)
    private BigDecimal brokerMarketValue;

    @Column(name = "market_value_difference", precision = 18, scale = 4)
    private BigDecimal marketValueDifference;

    @Column(name = "difference_count")
    private Integer differenceCount;

    @Column(name = "difference_details", columnDefinition = "TEXT")
    private String differenceDetails;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ReconcileStatus {
        MATCH,     // 匹配
        DISCARD,   // 差异
        PENDING    // 待核对
    }
}
