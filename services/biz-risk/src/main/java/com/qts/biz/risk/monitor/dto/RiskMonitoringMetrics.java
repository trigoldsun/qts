package com.qts.biz.risk.monitor.dto;

import java.time.LocalDateTime;

/**
 * Composite monitoring metrics DTO
 * Contains all risk monitoring metrics for WebSocket push
 */
public class RiskMonitoringMetrics {

    private String metricsId;
    private LocalDateTime timestamp;
    private String accountId;
    private TradingChannelMetricsDTO tradingChannel;
    private RiskMetricsDTO riskMetrics;
    private SystemHealthMetricsDTO systemHealth;

    public RiskMonitoringMetrics() {
        this.metricsId = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public RiskMonitoringMetrics(TradingChannelMetricsDTO tradingChannel, RiskMetricsDTO riskMetrics) {
        this();
        this.tradingChannel = tradingChannel;
        this.riskMetrics = riskMetrics;
    }

    public String getMetricsId() {
        return metricsId;
    }

    public void setMetricsId(String metricsId) {
        this.metricsId = metricsId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public TradingChannelMetricsDTO getTradingChannel() {
        return tradingChannel;
    }

    public void setTradingChannel(TradingChannelMetricsDTO tradingChannel) {
        this.tradingChannel = tradingChannel;
    }

    public RiskMetricsDTO getRiskMetrics() {
        return riskMetrics;
    }

    public void setRiskMetrics(RiskMetricsDTO riskMetrics) {
        this.riskMetrics = riskMetrics;
    }

    public SystemHealthMetricsDTO getSystemHealth() {
        return systemHealth;
    }

    public void setSystemHealth(SystemHealthMetricsDTO systemHealth) {
        this.systemHealth = systemHealth;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final RiskMonitoringMetrics m = new RiskMonitoringMetrics();

        public Builder accountId(String val) { m.accountId = val; return this; }
        public Builder tradingChannel(TradingChannelMetricsDTO val) { m.tradingChannel = val; return this; }
        public Builder riskMetrics(RiskMetricsDTO val) { m.riskMetrics = val; return this; }
        public Builder systemHealth(SystemHealthMetricsDTO val) { m.systemHealth = val; return this; }

        public RiskMonitoringMetrics build() {
            return m;
        }
    }
}
