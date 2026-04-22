package com.qts.biz.risk.engine;

import com.qts.biz.risk.engine.rules.SingleOrderLimitRule;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests for SingleOrderLimitRule
 */
class SingleOrderLimitRuleTest {

    private SingleOrderLimitRule rule;

    @BeforeEach
    void setUp() {
        rule = new SingleOrderLimitRule(1000000.0); // 100万 limit
    }

    @Test
    void testPassWhenOrderWithinLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(100); // 1000 total amount

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Order should be within limit");
    }

    @Test
    void testFailWhenOrderExceedsLimit() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10000.0"));
        request.setQuantity(200); // 2,000,000 total amount

        RuleResult result = rule.evaluate(request);
        
        assertFalse(result.isPassed(), "Order should exceed limit");
        assertEquals("SINGLE_ORDER_AMOUNT_EXCEEDED", result.getRejectCode());
    }

    @Test
    void testPassWhenNoOrderAmount() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        // No price or quantity set

        RuleResult result = rule.evaluate(request);
        
        assertTrue(result.isPassed(), "Should pass when no order amount to evaluate");
    }
}
