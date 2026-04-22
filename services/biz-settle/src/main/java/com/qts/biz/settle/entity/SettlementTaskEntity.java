package com.qts.biz.settle.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 清算任务实体
 * 管理日终清算批次的状态
 */
@Entity
@Table(name = "settlement_tasks", schema = "settle")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementTaskEntity {

    @Id
    @Column(name = "settle_id", length = 32)
    private String settleId;

    @Column(name = "settle_date", nullable = false)
    private LocalDate settleDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "accounts_count")
    private Integer accountsCount;

    @Column(name = "processed_count")
    private Integer processedCount;

    @Column(name = "failed_count")
    private Integer failedCount;

    @Column(name = "total_assets_start", precision = 18, scale = 4)
    private BigDecimal totalAssetsStart;

    @Column(name = "total_assets_end", precision = 18, scale = 4)
    private BigDecimal totalAssetsEnd;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum SettlementStatus {
        PENDING,      // 待开始
        PROCESSING,   // 处理中
        COMPLETED,    // 已完成
        FAILED        // 失败
    }
}
