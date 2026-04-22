package com.qts.market.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 标的信息实体
 */
@Entity
@Table(name = "symbol_info", indexes = {
    @Index(name = "idx_symbol_exchange", columnList = "exchange"),
    @Index(name = "idx_symbol_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SymbolInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 标的代码，如 BTC-USDT, SH600000
     */
    @Column(name = "symbol", nullable = false, unique = true, length = 32)
    private String symbol;

    /**
     * 标的名称
     */
    @Column(name = "symbol_name", length = 64)
    private String symbolName;

    /**
     * 交易所：SH, SZ, BINANCE, OKX
     */
    @Column(name = "exchange", nullable = false, length = 16)
    private String exchange;

    /**
     * 上市日期
     */
    @Column(name = "listing_date")
    private LocalDate listingDate;

    /**
     * 状态：ACTIVE, SUSPENDED, DELISTED
     */
    @Column(name = "status", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private SymbolStatus status;

    /**
     * 标的类型：STOCK, CRYPTO, FUTURES
     */
    @Column(name = "symbol_type", length = 16)
    @Enumerated(EnumType.STRING)
    private SymbolType symbolType;

    @Column(name = "created_at", nullable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    public enum SymbolStatus {
        ACTIVE, SUSPENDED, DELISTED
    }

    public enum SymbolType {
        STOCK, CRYPTO, FUTURES
    }
}
