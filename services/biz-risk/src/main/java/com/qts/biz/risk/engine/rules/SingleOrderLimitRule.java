package com.qts.biz.risk.engine.rules;

import com.qts.biz.risk.engine.core.RiskRule;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Single Order Limit Rule
 * 
 * Default: Single order amount limit is 1,000,000 (100万)
 * Configurable via parameters: maxOrderAmount
 * 
 * Reject Code: SINGLE_ORDER_AMOUNT_EXCEEDED
 */
public class SingleOrderLimitRule implements RiskRule {

    private static final Logger logger = LoggerFactory.getLogger(SingleOrderLimitRule.class);

    private static final String RULE_ID = "SINGLE_ORDER_LIMIT";
    private static final String RULE_NAME = "Single Order Amount Limit";
    private static final String RULE_TYPE = "SINGLE_ORDER_LIMIT";
    private static final int PRIORITY = 20;
    private static final double DEFAULT_MAX_ORDER_AMOUNT = 1000000.0; // 100万
    private static final String REJECT_CODE = "SINGLE_ORDER_AMOUNT_EXCEEDED";

    private final double maxOrderAmount;
    private final boolean enabled;

    public SingleOrderLimitRule(double maxOrderAmount) {
        this.maxOrderAmount = maxOrderAmount;
        this.enabled = true;
    }

    public SingleOrderLimitRule(Map<String, Object> parameters) {
        this.maxOrderAmount = getDoubleParam(parameters, "maxOrderAmount", DEFAULT_MAX_ORDER_AMOUNT);
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
        logger.debug("Evaluating single order limit for accountId={}, symbol={}", 
                request.getAccountId(), request.getSymbol());

        BigDecimal orderAmount = request.computeOrderAmount();
        if (orderAmount == null || orderAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return RuleResult.pass(RULE_ID, RULE_NAME, "No order amount to evaluate");
        }

        double orderAmountValue = orderAmount.doubleValue();
        if (orderAmountValue > maxOrderAmount) {
            String detail = String.format(
                    "Order amount %.2f exceeds limit %.2f (100万)",
                    orderAmountValue, maxOrderAmount);
            logger.info("Single order limit exceeded for accountId={}: {}", 
                    request.getAccountId(), detail);
            return RuleResult.fail(RULE_ID, RULE_NAME, REJECT_CODE, detail);
        }

        return RuleResult.pass(RULE_ID, RULE_NAME, 
                String.format("Order amount %.2f is within limit %.2f", 
                        orderAmountValue, maxOrderAmount));
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
