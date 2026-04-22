package com.qts.biz.risk.monitor;

import com.qts.biz.risk.monitor.collector.RiskMetricsCollector;
import com.qts.biz.risk.monitor.collector.SystemHealthMetricsCollector;
import com.qts.biz.risk.monitor.collector.TradingChannelMetricsCollector;
import com.qts.biz.risk.monitor.dto.RiskMonitoringMetrics;
import com.qts.biz.risk.monitor.dto.RiskMetricsDTO;
import com.qts.biz.risk.monitor.dto.TradingChannelMetricsDTO;
import com.qts.biz.risk.monitor.websocket.RiskMetricsWebSocketPublisher;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitor Service - Real-time Risk Monitoring Service
 * 
 * Responsibilities:
 * 1. Collect trading channel metrics (order count, amount, cancel rate, latency P50/P95/P99)
 * 2. Collect risk metrics (positions, capital, margin ratio)
 * 3. Push metrics via WebSocket at ≤1s frequency
 * 4. Export metrics via Micrometer + Prometheus
 * 
 * Compliance: ESD-MANDATORY-001 L2 Architecture Design Standards
 */
@Service
public class MonitorService {

    private static final Logger logger = LoggerFactory.getLogger(MonitorService.class);
    
    private final TradingChannelMetricsCollector tradingChannelCollector;
    private final RiskMetricsCollector riskCollector;
    private final SystemHealthMetricsCollector systemHealthCollector;
    private final RiskMetricsWebSocketPublisher wsPublisher;
    private final MeterRegistry meterRegistry;
    
    // Active channels being monitored
    private final Set<String> activeChannels = ConcurrentHashMap.newKeySet();
    
    // Active accounts being monitored
    private final Set<String> activeAccounts = ConcurrentHashMap.newKeySet();
    
    // Service status
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Track last push time for metrics
    private volatile long lastPushTimestamp = 0;

    @Autowired
    public MonitorService(
            TradingChannelMetricsCollector tradingChannelCollector,
            RiskMetricsCollector riskCollector,
            SystemHealthMetricsCollector systemHealthCollector,
            RiskMetricsWebSocketPublisher wsPublisher,
            MeterRegistry meterRegistry) {
        this.tradingChannelCollector = tradingChannelCollector;
        this.riskCollector = riskCollector;
        this.systemHealthCollector = systemHealthCollector;
        this.wsPublisher = wsPublisher;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        logger.info("MonitorService initializing...");
        running.set(true);
        
        // Register service-level metrics
        registerServiceMetrics();
        
        logger.info("MonitorService initialized successfully");
    }

    /**
     * Scheduled task to push metrics every second
     * Push frequency: ≤1s (1000ms)
     */
    @Scheduled(fixedRate = 1000)
    public void pushMetrics() {
        if (!running.get()) {
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Build composite metrics
            RiskMonitoringMetrics metrics = buildMetrics();
            
            // Push to WebSocket
            wsPublisher.publishMetrics(metrics);
            
            // Push per-channel and per-account metrics
            for (String channelId : activeChannels) {
                TradingChannelMetricsDTO channelMetrics = tradingChannelCollector.getMetrics(channelId);
                RiskMonitoringMetrics channelComposite = RiskMonitoringMetrics.builder()
                    .tradingChannel(channelMetrics)
                    .build();
                wsPublisher.publishChannelMetrics(channelId, channelComposite);
            }
            
            for (String accountId : activeAccounts) {
                RiskMetricsDTO accountMetrics = riskCollector.getMetrics(accountId);
                RiskMonitoringMetrics accountComposite = RiskMonitoringMetrics.builder()
                    .riskMetrics(accountMetrics)
                    .build();
                wsPublisher.publishAccountMetrics(accountId, accountComposite);
            }
            
            lastPushTimestamp = System.currentTimeMillis();
            
            // Record push duration
            meterRegistry.timer("monitor.push.duration").record(
                System.currentTimeMillis() - startTime,
                java.util.concurrent.TimeUnit.MILLISECONDS
            );
            
        } catch (Exception e) {
            logger.error("Error pushing metrics: {}", e.getMessage(), e);
            meterRegistry.counter("monitor.push.errors", Tags.of("error", e.getClass().getSimpleName())).increment();
        }
    }

    /**
     * Record a trading event
     */
    public void recordTradingEvent(String channelId, String status, BigDecimal amount, 
                                   boolean isBuy, long latencyMs) {
        tradingChannelCollector.recordOrder(channelId, status);
        if (amount != null) {
            tradingChannelCollector.recordTrade(channelId, amount, isBuy);
        }
        if (latencyMs >= 0) {
            tradingChannelCollector.recordLatency(channelId, latencyMs);
        }
        activeChannels.add(channelId);
        
        // Record custom metrics
        meterRegistry.counter("trading.events", 
            Tags.of("channel", channelId, "status", status)).increment();
    }

    /**
     * Update risk metrics for an account
     */
    public void updateRiskMetrics(String accountId, List<PositionUpdate> positions, 
                                  CapitalUpdate capital) {
        // Update positions
        if (positions != null) {
            for (PositionUpdate pos : positions) {
                riskCollector.updatePosition(
                    accountId, 
                    pos.symbol(), 
                    pos.quantity(), 
                    pos.marketValue(), 
                    pos.costPrice()
                );
            }
        }
        
        // Update capital
        if (capital != null) {
            riskCollector.updateCapital(
                accountId,
                capital.totalAssets(),
                capital.availableCash(),
                capital.frozenCash(),
                capital.marginOccupied()
            );
        }
        
        activeAccounts.add(accountId);
    }

    /**
     * Get current trading channel metrics
     */
    public TradingChannelMetricsDTO getChannelMetrics(String channelId) {
        return tradingChannelCollector.getMetrics(channelId);
    }

    /**
     * Get current risk metrics for an account
     */
    public RiskMetricsDTO getRiskMetrics(String accountId) {
        return riskCollector.getMetrics(accountId);
    }

    /**
     * Get service health status
     */
    public ServiceHealth getServiceHealth() {
        return new ServiceHealth(
            running.get(),
            activeChannels.size(),
            activeAccounts.size(),
            lastPushTimestamp,
            System.currentTimeMillis() - lastPushTimestamp
        );
    }

    /**
     * Stop the monitoring service
     */
    public void shutdown() {
        logger.info("MonitorService shutting down...");
        running.set(false);
    }

    private RiskMonitoringMetrics buildMetrics() {
        // Build composite metrics for global push
        RiskMonitoringMetrics.RiskMonitoringMetricsBuilder builder = RiskMonitoringMetrics.builder();
        
        // Add trading channel metrics
        TradingChannelMetricsDTO tradingMetrics = TradingChannelMetricsDTO.builder()
            .channelId("ALL")
            .build();
        
        // Aggregate all channels
        long totalOrders = 0, totalFilled = 0, totalCancelled = 0, totalRejected = 0;
        double totalBuyAmount = 0, totalSellAmount = 0;
        double maxP99 = 0;
        
        for (String channelId : activeChannels) {
            TradingChannelMetricsDTO ch = tradingChannelCollector.getMetrics(channelId);
            totalOrders += ch.getOrderCountPerMin() != null ? ch.getOrderCountPerMin() : 0;
            totalFilled += ch.getFilledCountPerMin() != null ? ch.getFilledCountPerMin() : 0;
            totalCancelled += ch.getCancelledCountPerMin() != null ? ch.getCancelledCountPerMin() : 0;
            totalRejected += ch.getRejectedCountPerMin() != null ? ch.getRejectedCountPerMin() : 0;
            totalBuyAmount += ch.getBuyAmountPerMin() != null ? ch.getBuyAmountPerMin().doubleValue() : 0;
            totalSellAmount += ch.getSellAmountPerMin() != null ? ch.getSellAmountPerMin().doubleValue() : 0;
            if (ch.getLatencyP99() != null && ch.getLatencyP99() > maxP99) {
                maxP99 = ch.getLatencyP99();
            }
        }
        
        builder.tradingChannel(tradingMetrics);
        
        // Build aggregated trading metrics
        TradingChannelMetricsDTO aggregatedTrading = TradingChannelMetricsDTO.builder()
            .channelId("AGGREGATED")
            .orderCountPerMin(totalOrders)
            .filledCountPerMin(totalFilled)
            .cancelledCountPerMin(totalCancelled)
            .rejectedCountPerMin(totalRejected)
            .buyAmountPerMin(BigDecimal.valueOf(totalBuyAmount))
            .sellAmountPerMin(BigDecimal.valueOf(totalSellAmount))
            .totalAmountPerMin(BigDecimal.valueOf(totalBuyAmount + totalSellAmount))
            .latencyP99(maxP99)
            .status(maxP99 > 1000 ? "DOWN" : maxP99 > 500 ? "DEGRADED" : "NORMAL")
            .build();
        
        builder.tradingChannel(aggregatedTrading);
        builder.systemHealth(systemHealthCollector.getMetrics());
        
        return builder.build();
    }

    private void registerServiceMetrics() {
        // Register service-level gauges
        meterRegistry.gauge("monitor.channels.active", activeChannels, Set::size);
        meterRegistry.gauge("monitor.accounts.active", activeAccounts, Set::size);
        meterRegistry.gauge("monitor.push.timestamp", this, m -> m.lastPushTimestamp);
    }

    /**
     * Service health status
     */
    public record ServiceHealth(
        boolean running,
        int activeChannels,
        int activeAccounts,
        long lastPushTimestamp,
        long pushIntervalMs
    ) {}

    /**
     * Position update record
     */
    public record PositionUpdate(
        String symbol,
        BigDecimal quantity,
        BigDecimal marketValue,
        BigDecimal costPrice
    ) {}

    /**
     * Capital update record
     */
    public record CapitalUpdate(
        BigDecimal totalAssets,
        BigDecimal availableCash,
        BigDecimal frozenCash,
        BigDecimal marginOccupied
    ) {}
}
