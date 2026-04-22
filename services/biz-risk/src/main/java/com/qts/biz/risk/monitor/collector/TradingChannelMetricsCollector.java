package com.qts.biz.risk.monitor.collector;

import com.qts.biz.risk.monitor.dto.TradingChannelMetricsDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trading Channel Metrics Collector
 * Collects and calculates trading channel metrics using Micrometer
 */
@Component
public class TradingChannelMetricsCollector {

    private final MeterRegistry meterRegistry;
    
    // Per-channel metrics storage
    private final ConcurrentHashMap<String, ChannelMetrics> channelMetrics = new ConcurrentHashMap<>();
    
    // Time window for metrics calculation (60 seconds)
    private static final long WINDOW_MS = 60_000;
    
    // Order counters per channel
    private record ChannelMetrics(
        Counter orderCounter,
        Counter filledCounter,
        Counter cancelledCounter,
        Counter rejectedCounter,
        Counter buyAmountCounter,
        Counter sellAmountCounter,
        Timer latencyTimer
    ) {}

    public TradingChannelMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Record an order event
     */
    public void recordOrder(String channelId, String status) {
        ChannelMetrics metrics = getOrCreateMetrics(channelId);
        metrics.orderCounter().increment();
        
        switch (status.toUpperCase()) {
            case "FILLED" -> metrics.filledCounter().increment();
            case "CANCELLED" -> metrics.cancelledCounter().increment();
            case "REJECTED" -> metrics.rejectedCounter().increment();
        }
    }

    /**
     * Record a trade execution
     */
    public void recordTrade(String channelId, BigDecimal amount, boolean isBuy) {
        ChannelMetrics metrics = getOrCreateMetrics(channelId);
        if (isBuy) {
            metrics.buyAmountCounter().increment(amount.doubleValue());
        } else {
            metrics.sellAmountCounter().increment(amount.doubleValue());
        }
    }

    /**
     * Record response latency
     */
    public void recordLatency(String channelId, long durationMs) {
        ChannelMetrics metrics = getOrCreateMetrics(channelId);
        metrics.latencyTimer().record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Get current metrics for a channel
     */
    public TradingChannelMetricsDTO getMetrics(String channelId) {
        ChannelMetrics metrics = channelMetrics.get(channelId);
        if (metrics == null) {
            return TradingChannelMetricsDTO.builder()
                .channelId(channelId)
                .status("UNKNOWN")
                .build();
        }
        
        // Calculate cancel rate
        double total = metrics.orderCounter().count();
        double cancelled = metrics.cancelledCounter().count();
        BigDecimal cancelRate = total > 0 
            ? BigDecimal.valueOf(cancelled / total * 100).setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
        
        // Get latency percentiles
        double p50 = getPercentileValue(metrics.latencyTimer(), 0.50);
        double p95 = getPercentileValue(metrics.latencyTimer(), 0.95);
        double p99 = getPercentileValue(metrics.latencyTimer(), 0.99);
        
        // Determine status
        String status = determineStatus(p99);
        
        return TradingChannelMetricsDTO.builder()
            .channelId(channelId)
            .orderCountPerMin((long) metrics.orderCounter().count())
            .filledCountPerMin((long) metrics.filledCounter().count())
            .cancelledCountPerMin((long) metrics.cancelledCounter().count())
            .rejectedCountPerMin((long) metrics.rejectedCounter().count())
            .buyAmountPerMin(BigDecimal.valueOf(metrics.buyAmountCounter().count()))
            .sellAmountPerMin(BigDecimal.valueOf(metrics.sellAmountCounter().count()))
            .totalAmountPerMin(
                BigDecimal.valueOf(metrics.buyAmountCounter().count())
                    .add(BigDecimal.valueOf(metrics.sellAmountCounter().count()))
            )
            .cancelRate(cancelRate)
            .latencyP50(p50)
            .latencyP95(p95)
            .latencyP99(p99)
            .status(status)
            .build();
    }
    
    /**
     * Reset metrics for a channel (for testing)
     */
    public void resetMetrics(String channelId) {
        channelMetrics.remove(channelId);
    }
    
    private ChannelMetrics getOrCreateMetrics(String channelId) {
        return channelMetrics.computeIfAbsent(channelId, id -> {
            Counter orderCounter = Counter.builder("trading.channel.orders")
                .tag("channel", channelId)
                .tag("status", "total")
                .description("Total orders per channel")
                .register(meterRegistry);
                
            Counter filledCounter = Counter.builder("trading.channel.orders")
                .tag("channel", channelId)
                .tag("status", "filled")
                .description("Filled orders per channel")
                .register(meterRegistry);
                
            Counter cancelledCounter = Counter.builder("trading.channel.orders")
                .tag("channel", channelId)
                .tag("status", "cancelled")
                .description("Cancelled orders per channel")
                .register(meterRegistry);
                
            Counter rejectedCounter = Counter.builder("trading.channel.orders")
                .tag("channel", channelId)
                .tag("status", "rejected")
                .description("Rejected orders per channel")
                .register(meterRegistry);
                
            Counter buyAmountCounter = Counter.builder("trading.channel.amount")
                .tag("channel", channelId)
                .tag("side", "buy")
                .description("Buy amount per channel")
                .register(meterRegistry);
                
            Counter sellAmountCounter = Counter.builder("trading.channel.amount")
                .tag("channel", channelId)
                .tag("side", "sell")
                .description("Sell amount per channel")
                .register(meterRegistry);
                
            Timer latencyTimer = Timer.builder("trading.channel.latency")
                .tag("channel", channelId)
                .description("Response latency per channel")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
                
            return new ChannelMetrics(
                orderCounter, filledCounter, cancelledCounter, 
                rejectedCounter, buyAmountCounter, sellAmountCounter, latencyTimer
            );
        });
    }
    
    private double getPercentileValue(Timer timer, double quantile) {
        // Micrometer stores percentiles in the timer snapshot
        // Access via percentileValues() array
        io.micrometer.core.instrument.distribution.HistogramSnapshot snapshot = timer.takeSnapshot();
        io.micrometer.core.instrument.distribution.ValueAtPercentile[] percentileValues = snapshot.percentileValues();
        for (io.micrometer.core.instrument.distribution.ValueAtPercentile vap : percentileValues) {
            if (Math.abs(vap.percentile() - quantile) < 0.001) {
                return vap.value();
            }
        }
        // Fallback to 0 if not found
        return 0.0;
    }
    
    private String determineStatus(double p99Latency) {
        if (p99Latency > 1000) {
            return "DOWN";
        } else if (p99Latency > 500) {
            return "DEGRADED";
        }
        return "NORMAL";
    }
}
