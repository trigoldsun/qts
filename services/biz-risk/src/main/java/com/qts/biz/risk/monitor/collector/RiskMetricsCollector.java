package com.qts.biz.risk.monitor.collector;

import com.qts.biz.risk.monitor.dto.RiskMetricsDTO;
import com.qts.biz.risk.monitor.dto.RiskMetricsDTO.PositionMetric;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Risk Metrics Collector
 * Collects and calculates risk metrics (positions, capital, margin ratios)
 */
@Component
public class RiskMetricsCollector {

    private final MeterRegistry meterRegistry;
    
    // Account risk data cache
    private final ConcurrentHashMap<String, AccountRiskData> accountRiskData = new ConcurrentHashMap<>();
    
    // Default thresholds
    private static final BigDecimal WARNING_MARGIN_RATIO_THRESHOLD = new BigDecimal("0.8");
    private static final BigDecimal DANGER_MARGIN_RATIO_THRESHOLD = new BigDecimal("0.9");

    /**
     * Account risk data
     */
    private record AccountRiskData(
        BigDecimal totalAssets,
        BigDecimal availableCash,
        BigDecimal frozenCash,
        BigDecimal marginOccupied,
        BigDecimal marketValue,
        List<PositionData> positions,
        BigDecimal maintenanceMargin
    ) {}
    
    private record PositionData(
        String symbol,
        BigDecimal quantity,
        BigDecimal marketValue,
        BigDecimal costPrice,
        BigDecimal profitLoss,
        BigDecimal profitLossRatio
    ) {}

    public RiskMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        registerGauges();
    }

    /**
     * Update account position data
     */
    public void updatePosition(String accountId, String symbol, BigDecimal quantity, 
                               BigDecimal marketValue, BigDecimal costPrice) {
        AccountRiskData current = accountRiskData.get(accountId);
        
        List<PositionData> positions = current != null ? new ArrayList<>(current.positions()) : new ArrayList<>();
        
        // Find or create position
        PositionData newPosition = new PositionData(
            symbol, quantity, marketValue, costPrice,
            marketValue.subtract(costPrice.multiply(quantity)),
            costPrice.compareTo(BigDecimal.ZERO) > 0 
                ? marketValue.subtract(costPrice.multiply(quantity))
                    .divide(costPrice.multiply(quantity), 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO
        );
        
        // Update or add position
        boolean found = false;
        for (int i = 0; i < positions.size(); i++) {
            if (positions.get(i).symbol().equals(symbol)) {
                positions.set(i, newPosition);
                found = true;
                break;
            }
        }
        if (!found) {
            positions.add(newPosition);
        }
        
        BigDecimal totalPositionValue = positions.stream()
            .map(PositionData::marketValue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal maintenanceMargin = totalPositionValue.multiply(new BigDecimal("0.15")); // 15% maintenance
        
        accountRiskData.put(accountId, new AccountRiskData(
            current != null ? current.totalAssets : BigDecimal.ZERO,
            current != null ? current.availableCash : BigDecimal.ZERO,
            current != null ? current.frozenCash : BigDecimal.ZERO,
            current != null ? current.marginOccupied : BigDecimal.ZERO,
            totalPositionValue,
            positions,
            maintenanceMargin
        ));
    }

    /**
     * Update account capital data
     */
    public void updateCapital(String accountId, BigDecimal totalAssets, BigDecimal availableCash,
                             BigDecimal frozenCash, BigDecimal marginOccupied) {
        AccountRiskData current = accountRiskData.get(accountId);
        
        accountRiskData.put(accountId, new AccountRiskData(
            totalAssets,
            availableCash,
            frozenCash,
            marginOccupied,
            current != null ? current.marketValue : BigDecimal.ZERO,
            current != null ? current.positions() : new ArrayList<>(),
            current != null ? current.maintenanceMargin() : BigDecimal.ZERO
        ));
    }

    /**
     * Get current risk metrics for an account
     */
    public RiskMetricsDTO getMetrics(String accountId) {
        AccountRiskData data = accountRiskData.get(accountId);
        if (data == null) {
            return RiskMetricsDTO.builder()
                .accountId(accountId)
                .riskLevel("UNKNOWN")
                .build();
        }
        
        // Calculate occupied capital
        BigDecimal occupiedCapital = data.frozenCash().add(data.marginOccupied());
        BigDecimal availableCapital = data.availableCash();
        
        // Calculate margin ratio (margin / total assets)
        BigDecimal marginRatio = data.totalAssets().compareTo(BigDecimal.ZERO) > 0
            ? data.marginOccupied().divide(data.totalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;
        
        // Calculate maintenance margin ratio
        BigDecimal maintenanceMarginRatio = data.totalAssets().compareTo(BigDecimal.ZERO) > 0
            ? data.maintenanceMargin().divide(data.totalAssets(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
            : BigDecimal.ZERO;
        
        // Determine risk level
        String riskLevel = determineRiskLevel(marginRatio);
        
        // Calculate leverage
        BigDecimal leverage = data.totalAssets().compareTo(BigDecimal.ZERO) > 0
            ? data.totalAssets().add(data.marketValue()).divide(data.totalAssets(), 2, RoundingMode.HALF_UP)
            : BigDecimal.ONE;
        
        // Build position metrics
        List<PositionMetric> positionMetrics = data.positions().stream()
            .map(p -> PositionMetric.builder()
                .symbol(p.symbol())
                .quantity(p.quantity())
                .marketValue(p.marketValue())
                .costPrice(p.costPrice())
                .profitLoss(p.profitLoss())
                .profitLossRatio(p.profitLossRatio())
                .build())
            .toList();
        
        return RiskMetricsDTO.builder()
            .accountId(accountId)
            .positions(positionMetrics)
            .totalPositionValue(data.marketValue())
            .totalPositionCount(data.positions().size())
            .totalAssets(data.totalAssets())
            .occupiedCapital(occupiedCapital)
            .availableCapital(availableCapital)
            .cashOccupied(data.frozenCash())
            .marginOccupied(data.marginOccupied())
            .marginRatio(marginRatio)
            .maintenanceMarginRatio(maintenanceMarginRatio)
            .warningMarginRatioThreshold(WARNING_MARGIN_RATIO_THRESHOLD)
            .dangerMarginRatioThreshold(DANGER_MARGIN_RATIO_THRESHOLD)
            .riskLevel(riskLevel)
            .leverage(leverage)
            .build();
    }
    
    /**
     * Clear account data (for testing)
     */
    public void clearAccountData(String accountId) {
        accountRiskData.remove(accountId);
    }
    
    private void registerGauges() {
        // Register gauge for total risk metrics
        Gauge.builder("risk.metrics.active.accounts", accountRiskData, Map::size)
            .description("Number of accounts being monitored")
            .register(meterRegistry);
    }
    
    private String determineRiskLevel(BigDecimal marginRatio) {
        if (marginRatio.compareTo(DANGER_MARGIN_RATIO_THRESHOLD) >= 0) {
            return "DANGER";
        } else if (marginRatio.compareTo(WARNING_MARGIN_RATIO_THRESHOLD) >= 0) {
            return "WARNING";
        }
        return "NORMAL";
    }
}
