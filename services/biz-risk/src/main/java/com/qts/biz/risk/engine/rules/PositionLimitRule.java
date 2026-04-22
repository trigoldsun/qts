package com.qts.biz.risk.engine.rules;

import com.qts.biz.risk.engine.core.RiskRule;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Position Limit Rule
 * 
 * Default: Single stock position limit is 20% of total asset value
 * Configurable via parameters: maxPositionPercent
 * 
 * Reject Code: POSITION_LIMIT_EXCEEDED
 */
public class PositionLimitRule implements RiskRule {

    private static final Logger logger = LoggerFactory.getLogger(PositionLimitRule.class);

    private static final String RULE_ID = "POSITION_LIMIT";
    private static final String RULE_NAME = "Single Position Limit";
    private static final String RULE_TYPE = "POSITION_LIMIT";
    private static final int PRIORITY = 10;
    private static final double DEFAULT_MAX_POSITION_PERCENT = 20.0;
    private static final String REJECT_CODE = "POSITION_LIMIT_EXCEEDED";

    private final double maxPositionPercent;
    private final boolean enabled;

    public PositionLimitRule(double maxPositionPercent) {
        this.maxPositionPercent = maxPositionPercent;
        this.enabled = true;
    }

    public PositionLimitRule(Map<String, Object> parameters) {
        this.maxPositionPercent = getDoubleParam(parameters, "maxPositionPercent", DEFAULT_MAX_POSITION_PERCENT);
        this.enabled = getBooleanParam(parameters, "enabled", true);
    }

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public String getRuleType() {
        return RULE_TYPE;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public RuleResult evaluate(RiskCheckRequest request) {
        logger.debug("Evaluating position limit for accountId={}, symbol={}", 
                request.getAccountId(), request.getSymbol());

        // Only check BUY orders for position limit
        if ("SELL".equalsIgnoreCase(request.getSide())) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "Sell order - position limit not applicable");
        }

        BigDecimal totalAssetValue = request.getTotalAssetValue();
        if (totalAssetValue == null || totalAssetValue.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Total asset value not available for accountId={}", request.getAccountId());
            // Fail safe - reject if we can't verify
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE, 
                    "Cannot verify position limit: total asset value unknown");
        }

        BigDecimal orderAmount = request.computeOrderAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "No order amount to evaluate");
        }

        // Calculate current position for this symbol
        BigDecimal currentPosition = BigDecimal.ZERO;
        Map<String, BigDecimal> positions = request.getPositions();
        if (positions != null && request.getSymbol() != null) {
            currentPosition = positions.getOrDefault(request.getSymbol(), BigDecimal.ZERO);
        }

        // New total position after this order
        BigDecimal newPositionValue = currentPosition.add(orderAmount);
        BigDecimal newPositionPercent = newPositionValue
                .divide(totalAssetValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));

        if (newPositionPercent.doubleValue() > maxPositionPercent) {
            String detail = String.format(
                    "New position %.2f%% exceeds limit %.2f%% (current: %.2f%%, order: %.2f, total asset: %.2f)",
                    newPositionPercent.doubleValue(), maxPositionPercent,
                    currentPosition.divide(totalAssetValue, 4, RoundingMode.HALF_UP)
                            .multiply(new BigDecimal(100)).doubleValue(),
                    orderAmount.doubleValue(), totalAssetValue.doubleValue());
            logger.info("Position limit exceeded for accountId={}: {}", request.getAccountId(), detail);
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE, detail);
        }

        return RuleResult.pass(RULE_ID, RULE_NAME, 
                String.format("Position %.2f%% is within limit %.2f%%", 
                        newPositionPercent.doubleValue(), maxPositionPercent));
    }

    private double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        if (params == null) return defaultValue;
        Object value = params.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
}
