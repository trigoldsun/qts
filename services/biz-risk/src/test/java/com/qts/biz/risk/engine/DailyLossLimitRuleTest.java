package com.qts.biz.risk.engine;

import com.qts.biz.risk.engine.rules.DailyLossLimitRule;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for DailyLossLimitRule
 */
class DailyLossLimitRuleTest {

    private DailyLossLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new DailyLossLimitRule(-5.0); // -5% daily loss limit
    }

    @Test
    void testPassWhenDailyLossWithinLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100);
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万
        request.setDailyProfitLoss(new BigDecimal("-1000")); // -1% daily loss

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Daily loss should be within limit");
    }

    @Test
    void testFailWhenDailyLossExceedsLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100);
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万
        request.setDailyProfitLoss(new BigDecimal("-8000")); // -8% daily loss

        RuleResult result = rule.evaluate(request);
        
        assertFalse(result.isPassed(), "Daily loss should exceed limit");
        assertEquals("DAILY_LOSS_LIMIT_EXCEEDED", result.getRejectCode());
    }

    @Test
    void testPassForSellOrders() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("SELL");
        request.setDailyProfitLoss(new BigDecimal("-8000"));

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Sell orders should pass daily loss limit check");
    }

    @Test
    void testFailSafeWhenTotalAssetUnknown() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSide("BUY");
        request.setDailyProfitLoss(new BigDecimal("-1000"));
        request.setTotalAssetValue(null);

        RuleResult result = rule.evaluate(request);
        
        assertFalse(result.isPassed(), "Should fail safe when total asset unknown");
    }
}
