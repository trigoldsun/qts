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
 * Net Buy Limit Rule
 * 
 * Default: Daily net buy limit is +10% of total asset value
 * Configurable via parameters: maxNetBuyPercent
 * 
 * Net Buy = Total Buy Amount - Total Sell Amount (daily)
 * Only applies to BUY orders (as selling reduces net buy)
 * 
 * Reject Code: NET_BUY_LIMIT_EXCEEDED
 */
public class NetBuyLimitRule implements RiskRule {

    private static final Logger logger = LoggerFactory.getLogger(NetBuyLimitRule.class);

    private static final String RULE_ID = "NET_BUY_LIMIT";
    private static final String RULE_NAME = "Daily Net Buy Limit";
    private static final String RULE_TYPE = "NET_BUY_LIMIT";
    private static final int PRIORITY = 40;
    private static final double DEFAULT_MAX_NET_BUY_PERCENT = 10.0; // +10%
    private static final String REJECT_CODE = "NET_BUY_LIMIT_EXCEEDED";

    private final double maxNetBuyPercent; // positive number, e.g., 10.0 means +10%
    private final boolean enabled;

    public NetBuyLimitRule(double maxNetBuyPercent) {
        this.maxNetBuyPercent = maxNetBuyPercent;
        this.enabled = true;
    }

    public NetBuyLimitRule(Map<String, Object> parameters) {
        this.maxNetBuyPercent = getDoubleParam(parameters, "maxNetBuyPercent", DEFAULT_MAX_NET_BUY_PERCENT);
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
        logger.debug("Evaluating net buy limit for accountId={}, symbol={}", 
                request.getAccountId(), request.getSymbol());

        // Only check BUY orders - selling doesn't increase net buy
        if ("SELL".equalsIgnoreCase(request.getSide())) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "Sell order - net buy limit not applicable");
        }

        BigDecimal totalAssetValue = request.getTotalAssetValue();
        if (totalAssetValue == null || totalAssetValue.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Total asset value not available for accountId={}", request.getAccountId());
            // Fail safe - reject if we can't verify
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE,
                    "Cannot verify net buy limit: total asset value unknown");
        }

        // Get current daily buy and sell amounts
        BigDecimal dailyBuyAmount = request.getDailyBuyAmount();
        if (dailyBuyAmount == null) {
            dailyBuyAmount = BigDecimal.ZERO;
        }

        BigDecimal dailySellAmount = request.getDailySellAmount();
        if (dailySellAmount == null) {
            dailySellAmount = BigDecimal.ZERO;
        }

        // Calculate current net buy
        BigDecimal currentNetBuy = dailyBuyAmount.subtract(dailySellAmount);
        BigDecimal currentNetBuyPercent = currentNetBuy
                .divide(totalAssetValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));

        // Calculate proposed order amount
        BigDecimal orderAmount = request.computeOrderAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "No order amount to evaluate");
        }

        // New net buy after this order
        BigDecimal newNetBuy = currentNetBuy.add(orderAmount);
        BigDecimal newNetBuyPercent = newNetBuy
                .divide(totalAssetValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal(100));

        if (newNetBuyPercent.doubleValue() > maxNetBuyPercent) {
            String detail = String.format(
                    "New net buy %.2f%% exceeds limit %.2f%% (current net buy: %.2f%%, order: %.2f, total asset: %.2f)",
                    newNetBuyPercent.doubleValue(), maxNetBuyPercent,
                    currentNetBuyPercent.doubleValue(),
                    orderAmount.doubleValue(), totalAssetValue.doubleValue());
            logger.info("Net buy limit exceeded for accountId={}: {}", request.getAccountId(), detail);
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE, detail);
        }

        return RuleResult.pass(RULE_ID, RULE_NAME, 
                String.format("Net buy %.2f%% is within limit %.2f%%", 
                        newNetBuyPercent.doubleValue(), maxNetBuyPercent));
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
