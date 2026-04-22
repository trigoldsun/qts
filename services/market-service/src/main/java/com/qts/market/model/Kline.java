package com.qts.market.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * K线数据实体
 * 支持1分钟/5分钟/1小时/日K线
 */
@Entity
@Table(name = "kline", indexes = {
    @Index(name = "idx_kline_symbol_period_time", columnList = "symbol, period, timestamp"),
    @Index(name = "idx_kline_timestamp", columnList = "timestamp")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Kline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标的代码，如 BTC-USDT, SH600000
     */
    @Column(name = "symbol", nullable = false, length = 32)
    private String symbol;

    /**
     * K线周期：1m, 5m, 1h, 1d
     */
    @Column(name = "period", nullable = false, length = 4)
    private String period;

    /**
     * 开盘时间
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /**
     * 开盘价
     */
    @Column(name = "open_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal openPrice;

    /**
     * 最高价
     */
    @Column(name = "high_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal highPrice;

    /**
     * 最低价
     */
    @Column(name = "low_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal lowPrice;

    /**
     * 收盘价
     */
    @Column(name = "close_price", nullable = false, precision = 18, scale = 8)
    private BigDecimal closePrice;

    /**
     * 成交量
     */
    @Column(name = "volume", nullable = false)
    private Long volume;

    /**
     * 成交额
     */
    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    /**
     * 复权类型：FORWARD, BACKWARD, NONE
     */
    @Column(name = "adjustment", length = 16)
    @Enumerated(EnumType.STRING)
    private AdjustmentType adjustment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
