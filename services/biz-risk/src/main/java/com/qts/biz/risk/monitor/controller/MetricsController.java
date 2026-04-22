package com.qts.biz.risk.monitor.controller;

import com.qts.biz.risk.monitor.MonitorService;
import com.qts.biz.risk.monitor.dto.RiskMetricsDTO;
import com.qts.biz.risk.monitor.dto.TradingChannelMetricsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Monitoring REST Controller
 * Provides HTTP endpoints for metrics retrieval
 */
@RestController
@RequestMapping("/api/v1/monitor")
public class MetricsController {

    private final MonitorService monitorService;

    @Autowired
    public MetricsController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    /**
     * Get service health status
     */
    @GetMapping("/health")
    public ResponseEntity<MonitorService.ServiceHealth> getHealth() {
        return ResponseEntity.ok(monitorService.getServiceHealth());
    }

    /**
     * Get trading channel metrics
     */
    @GetMapping("/channels/{channelId}/metrics")
    public ResponseEntity<TradingChannelMetricsDTO> getChannelMetrics(@PathVariable String channelId) {
        TradingChannelMetricsDTO metrics = monitorService.getChannelMetrics(channelId);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get risk metrics for an account
     */
    @GetMapping("/accounts/{accountId}/risk")
    public ResponseEntity<RiskMetricsDTO> getRiskMetrics(@PathVariable String accountId) {
        RiskMetricsDTO metrics = monitorService.getRiskMetrics(accountId);
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metrics);
    }

    /**
     * Record trading event (for testing or event-driven ingestion)
     */
    @PostMapping("/events/trading")
    public ResponseEntity<Void> recordTradingEvent(@RequestBody TradingEventRequest request) {
        monitorService.recordTradingEvent(
            request.channelId(),
            request.status(),
            request.amount(),
            request.isBuy(),
            request.latencyMs()
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Update risk metrics
     */
    @PostMapping("/accounts/{accountId}/risk")
    public ResponseEntity<Void> updateRiskMetrics(
            @PathVariable String accountId,
            @RequestBody RiskUpdateRequest request) {
        
        var positions = request.positions() != null
            ? request.positions().stream()
                .map(p -> new MonitorService.PositionUpdate(
                    p.symbol(),
                    p.quantity(),
                    p.marketValue(),
                    p.costPrice()
                ))
                .toList()
            : null;
        
        var capital = request.capital() != null
            ? new MonitorService.CapitalUpdate(
                request.capital().totalAssets(),
                request.capital().availableCash(),
                request.capital().frozenCash(),
                request.capital().marginOccupied()
            )
            : null;
        
        monitorService.updateRiskMetrics(accountId, positions, capital);
        return ResponseEntity.ok().build();
    }

    /**
     * Request body records
     */
    public record TradingEventRequest(
        String channelId,
        String status,
        java.math.BigDecimal amount,
        boolean isBuy,
        long latencyMs
    ) {}

    public record RiskUpdateRequest(
        java.util.List<PositionData> positions,
        CapitalData capital
    ) {}

    public record PositionData(
        String symbol,
        java.math.BigDecimal quantity,
        java.math.BigDecimal marketValue,
        java.math.BigDecimal costPrice
    ) {}

    public record CapitalData(
        java.math.BigDecimal totalAssets,
        java.math.BigDecimal availableCash,
        java.math.BigDecimal frozenCash,
        java.math.BigDecimal marginOccupied
    ) {}
}
