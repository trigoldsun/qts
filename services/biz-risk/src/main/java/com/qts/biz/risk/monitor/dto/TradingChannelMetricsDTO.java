package com.qts.biz.risk.monitor.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Trading Channel Metrics DTO
 * Real-time trading channel monitoring metrics
 */
public class TradingChannelMetricsDTO {

    private String channelId;
    private LocalDateTime timestamp;
    
    // Order metrics (count/min)
    private Long orderCountPerMin;
    private Long filledCountPerMin;
    private Long cancelledCountPerMin;
    private Long rejectedCountPerMin;
    
    // Amount metrics (amount/min)
    private BigDecimal buyAmountPerMin;
    private BigDecimal sellAmountPerMin;
    private BigDecimal totalAmountPerMin;
    
    // Cancel rate
    private BigDecimal cancelRate; // cancel/total %
    
    // Response latency percentiles (ms)
    private Double latencyP50;
    private Double latencyP95;
    private Double latencyP99;
    
    // Status
    private String status; // NORMAL, DEGRADED, DOWN

    public TradingChannelMetricsDTO() {
        this.timestamp = LocalDateTime.now();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getOrderCountPerMin() {
        return orderCountPerMin;
    }

    public void setOrderCountPerMin(Long orderCountPerMin) {
        this.orderCountPerMin = orderCountPerMin;
    }

    public Long getFilledCountPerMin() {
        return filledCountPerMin;
    }

    public void setFilledCountPerMin(Long filledCountPerMin) {
        this.filledCountPerMin = filledCountPerMin;
    }

    public Long getCancelledCountPerMin() {
        return cancelledCountPerMin;
    }

    public void setCancelledCountPerMin(Long cancelledCountPerMin) {
        this.cancelledCountPerMin = cancelledCountPerMin;
    }

    public Long getRejectedCountPerMin() {
        return rejectedCountPerMin;
    }

    public void setRejectedCountPerMin(Long rejectedCountPerMin) {
        this.rejectedCountPerMin = rejectedCountPerMin;
    }

    public BigDecimal getBuyAmountPerMin() {
        return buyAmountPerMin;
    }

    public void setBuyAmountPerMin(BigDecimal buyAmountPerMin) {
        this.buyAmountPerMin = buyAmountPerMin;
    }

    public BigDecimal getSellAmountPerMin() {
        return sellAmountPerMin;
    }

    public void setSellAmountPerMin(BigDecimal sellAmountPerMin) {
        this.sellAmountPerMin = sellAmountPerMin;
    }

    public BigDecimal getTotalAmountPerMin() {
        return totalAmountPerMin;
    }

    public void setTotalAmountPerMin(BigDecimal totalAmountPerMin) {
        this.totalAmountPerMin = totalAmountPerMin;
    }

    public BigDecimal getCancelRate() {
        return cancelRate;
    }

    public void setCancelRate(BigDecimal cancelRate) {
        this.cancelRate = cancelRate;
    }

    public Double getLatencyP50() {
        return latencyP50;
    }

    public void setLatencyP50(Double latencyP50) {
        this.latencyP50 = latencyP50;
    }

    public Double getLatencyP95() {
        return latencyP95;
    }

    public void setLatencyP95(Double latencyP95) {
        this.latencyP95 = latencyP95;
    }

    public Double getLatencyP99() {
        return latencyP99;
    }

    public void setLatencyP99(Double latencyP99) {
        this.latencyP99 = latencyP99;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TradingChannelMetricsDTO dto = new TradingChannelMetricsDTO();

        public Builder channelId(String channelId) { dto.channelId = channelId; return this; }
        public Builder orderCountPerMin(Long val) { dto.orderCountPerMin = val; return this; }
        public Builder filledCountPerMin(Long val) { dto.filledCountPerMin = val; return this; }
        public Builder cancelledCountPerMin(Long val) { dto.cancelledCountPerMin = val; return this; }
        public Builder rejectedCountPerMin(Long val) { dto.rejectedCountPerMin = val; return this; }
        public Builder buyAmountPerMin(BigDecimal val) { dto.buyAmountPerMin = val; return this; }
        public Builder sellAmountPerMin(BigDecimal val) { dto.sellAmountPerMin = val; return this; }
        public Builder totalAmountPerMin(BigDecimal val) { dto.totalAmountPerMin = val; return this; }
        public Builder cancelRate(BigDecimal val) { dto.cancelRate = val; return this; }
        public Builder latencyP50(Double val) { dto.latencyP50 = val; return this; }
        public Builder latencyP95(Double val) { dto.latencyP95 = val; return this; }
        public Builder latencyP99(Double val) { dto.latencyP99 = val; return this; }
        public Builder status(String val) { dto.status = val; return this; }

        public TradingChannelMetricsDTO build() {
            return dto;
        }
    }
}
