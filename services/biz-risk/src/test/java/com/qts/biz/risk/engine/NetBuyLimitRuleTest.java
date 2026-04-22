package com.qts.biz.risk.engine;

import com.qts.biz.risk.engine.rules.NetBuyLimitRule;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for NetBuyLimitRule
 */
class NetBuyLimitRuleTest {

    private NetBuyLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new NetBuyLimitRule(10.0); // +10% net buy limit
    }

    @Test
    void testPassWhenNetBuyWithinLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100);
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万
        request.setDailyBuyAmount(new BigDecimal("3000")); // 3% already bought
        request.setDailySellAmount(new BigDecimal("1000")); // 1% sold

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Net buy should be within limit");
    }

    @Test
    void testFailWhenNetBuyExceedsLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100);
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万
        request.setDailyBuyAmount(new BigDecimal("8000")); // 8% already bought
        request.setDailySellAmount(new BigDecimal("1000")); // 1% sold

        RuleResult result = rule.evaluate(request);
        
        assertFalse(result.isPassed(), "Net buy should exceed limit");
        assertEquals("NET_BUY_LIMIT_EXCEEDED", result.getRejectCode());
    }

    @Test
    void testPassForSellOrders() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("SELL");
        request.setDailyBuyAmount(new BigDecimal("8000"));
        request.setDailySellAmount(new BigDecimal("1000"));

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Sell orders should pass net buy limit check");
    }

    @Test
    void testFailSafeWhenTotalAssetUnknown() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSide("BUY");
        request.setDailyBuyAmount(new BigDecimal("3000"));
        request.setDailySellAmount(new BigDecimal("1000"));
        request.setTotalAssetValue(null);

        RuleResult result = rule.evaluate(request);
        
        assertFalse(result.isPassed(), "Should fail safe when total asset unknown");
    }
}
