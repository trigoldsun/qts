package com.qts.biz.risk.engine;

import com.qts.biz.risk.engine.core.DefaultRuleEngine;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import com.qts.biz.risk.engine.model.dto.RiskCheckResponse;
import com.qts.biz.risk.engine.rules.PositionLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for PositionLimitRule
 */
class PositionLimitRuleTest {

    private PositionLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new PositionLimitRule(20.0); // 20% limit
    }

    @Test
    void testPassWhenPositionWithinLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100);
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万 total asset
        request.setPositions(new HashMap<>()); // No existing positions

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Position should be within limit");
    }

    @Test
    void testFailWhenPositionExceedsLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("100.0"));
        request.setQuantity(300); // 30000 order amount
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万 total asset, 30% would exceed 20%
        request.setPositions(new HashMap<>()); // No existing positions

        RuleResult result = rule.evaluate(request);
        
        assertFalse(result.isPassed(), "Position should exceed limit");
        assertEquals("POSITION_LIMIT_EXCEEDED", result.getRejectCode());
    }

    @Test
    void testPassForSellOrders() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("SELL");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(1000);
        request.setTotalAssetValue(new BigDecimal("100000"));

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Sell orders should pass position limit check");
    }

    @Test
    void testFailSafeWhenTotalAssetUnknown() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100);
        request.setTotalAssetValue(null); // Unknown total asset

        RuleResult result = rule.evaluate(request);
        
        // Fail safe - reject when we can't verify
        assertFalse(result.isPassed(), "Should fail safe when total asset unknown");
    }

    @Test
    void testPassWithExistingPosition() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100); // 1000 order amount
        request.setTotalAssetValue(new BigDecimal("100000")); // 10万
        
        // Already have 10% position
        Map<String, BigDecimal> positions = new HashMap<>();
        positions.put("000001", new BigDecimal("10000")); // 10% existing
        request.setPositions(positions);

        RuleResult result = rule.evaluate(request);
        
        // 10% existing + 1% new = 11% < 20%, should pass
        assertTrue(result.isPassed(), "Total position should be within limit");
    }
}
