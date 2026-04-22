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
 * Daily Loss Limit Rule
 * 
 * Default: Daily loss limit is -5% (meaning total daily loss cannot exceed -5%)
 * Configurable via parameters: maxDailyLossPercent
 * 
 * This rule checks if the proposed order would cause the daily loss to exceed the limit.
 * Only applies to BUY orders (as SELL orders might reduce losses).
 * 
 * Reject Code: DAILY_LOSS_LIMIT_EXCEEDED
 */
public class DailyLossLimitRule implements RiskRule {

    private static final Logger logger = LoggerFactory.getLogger(DailyLossLimitRule.class);

    private static final String RULE_ID = "DAILY_LOSS_LIMIT";
    private static final String RULE_NAME = "Daily Loss Limit";
    private static final String RULE_TYPE = "DAILY_LOSS_LIMIT";
    private static final int PRIORITY = 30;
    private static final double DEFAULT_MAX_DAILY_LOSS_PERCENT = -5.0; // -5%
    private static final String REJECT_CODE = "DAILY_LOSS_LIMIT_EXCEEDED";

    private final double maxDailyLossPercent; // negative number, e.g., -5.0 means -5%
    private final boolean enabled;

    public DailyLossLimitRule(double maxDailyLossPercent) {
        this.maxDailyLossPercent = maxDailyLossPercent;
        this.enabled = true;
    }

    public DailyLossLimitRule(Map<String, Object> parameters) {
        this.maxDailyLossPercent = getDoubleParam(parameters, "maxDailyLossPercent", DEFAULT_MAX_DAILY_LOSS_PERCENT);
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
        logger.debug("Evaluating daily loss limit for accountId={}", request.getAccountId());

        // Only check BUY orders - selling doesn't increase daily loss
        if ("SELL".equalsIgnoreCase(request.getSide())) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "Sell order - daily loss limit not applicable");
        }

        BigDecimal totalAssetValue = request.getTotalAssetValue();
        if (totalAssetValue == null || totalAssetValue.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Total asset value not available for accountId={}", request.getAccountId());
            // Fail safe - reject if we can't verify
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE,
                    "Cannot verify daily loss limit: total asset value unknown");
        }

        // Get current daily P&L
        BigDecimal dailyProfitLoss = request.getDailyProfitLoss();
        if (dailyProfitLoss == null) {
            dailyProfitLoss = BigDecimal.ZERO;
        }

        // Calculate potential order cost (this would be a loss if order goes against)
        BigDecimal orderAmount = request.computeOrderAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "No order amount to evaluate");
        }

        // Calculate current daily loss percentage
        BigDecimal currentDailyLossPercent = dailyProfitLoss
                .divide(totalAssetValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));

        // The maxDailyLossPercent is negative (e.g., -5.0)
        // We check if currentDailyLossPercent < maxDailyLossPercent (more negative)
        if (currentDailyLossPercent.doubleValue() < maxDailyLossPercent) {
            String detail = String.format(
                    "Daily loss %.2f%% already exceeds limit %.2f%%",
                    currentDailyLossPercent.doubleValue(), maxDailyLossPercent);
            logger.info("Daily loss limit already exceeded for accountId={}: {}",
                    request.getAccountId(), detail);
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE, detail);
        }

        // Estimate worst case: assume order immediately causes loss equal to order amount
        // (This is conservative - in reality would use VaR or other risk models)
        BigDecimal worstCaseLoss = dailyProfitLoss.subtract(orderAmount);
        BigDecimal worstCaseLossPercent = worstCaseLoss
                .divide(totalAssetValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));

        if (worstCaseLossPercent.doubleValue() < maxDailyLossPercent) {
            String detail = String.format(
                    "Worst case daily loss %.2f%% would exceed limit %.2f%% (current: %.2f%%, order impact: %.2f)",
                    worstCaseLossPercent.doubleValue(), maxDailyLossPercent,
                    currentDailyLossPercent.doubleValue(), orderAmount.doubleValue());
            logger.info("Daily loss limit would be exceeded for accountId={}: {}",
                    request.getAccountId(), detail);
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE, detail);
        }

        return RuleResult.pass(RULE_ID, RULE_NAME,
                String.format("Daily loss %.2f%% is within limit %.2f%%",
                        currentDailyLossPercent.doubleValue(), maxDailyLossPercent));
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
