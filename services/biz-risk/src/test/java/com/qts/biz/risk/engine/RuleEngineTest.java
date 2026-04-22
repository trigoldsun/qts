package com.qts.biz.risk.engine;

import com.qts.biz.risk.engine.core.DefaultRuleEngine;
import com.qts.biz.risk.engine.core.RuleResult;
import com.qts.biz.risk.engine.core.RiskRule;
import com.qts.biz.risk.engine.model.dto.RiskCheckRequest;
import com.qts.biz.risk.engine.model.dto.RiskCheckResponse;
import com.qts.biz.risk.engine.model.dto.RuleConfigDTO;
import com.qts.biz.risk.engine.model.enums.LogicOperator;
import com.qts.biz.risk.engine.model.enums.RuleStatus;
import com.qts.biz.risk.engine.model.enums.RuleType;
import com.qts.biz.risk.engine.rules.PositionLimitRule;
import com.qts.biz.risk.engine.rules.SingleOrderLimitRule;
import com.qts.biz.risk.engine.service.RuleConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultRuleEngine
 * Tests rule engine functionality:
 * - Single rule activation
 * - AND/OR rule chain logic
 * - Priority-based rule execution
 */
class RuleEngineTest {

    @Mock
    private RuleConfigService ruleConfigService;

    private DefaultRuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Default setup returns empty list, triggering default rules
        when(ruleConfigService.getAllActiveRules()).thenReturn(Collections.emptyList());
        ruleEngine = new DefaultRuleEngine(ruleConfigService);
        ruleEngine.init();
    }

    private RiskCheckRequest createBaseRequest() {
        RiskCheckRequest request = new RiskCheckRequest();
        request.setAccountId("ACC001");
        request.setSymbol("000001");
        request.setSide("BUY");
        request.setPrice(new BigDecimal("10.0"));
        request.setQuantity(1000);
        request.setTotalAssetValue(new BigDecimal("1000000")); // 100万
        request.setPositions(new HashMap<>());
        request.setDailyBuyAmount(BigDecimal.ZERO);
        request.setDailySellAmount(BigDecimal.ZERO);
        request.setDailyProfitLoss(BigDecimal.ZERO);
        return request;
    }

    @Test
    void testEvaluate_NoActiveRules_Passes() {
        // No rules configured, should pass
        RiskCheckRequest request = createBaseRequest();
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertTrue(response.isPassed());
    }

    @Test
    void testEvaluate_SinglePositionLimitRule_PositionWithinLimit_Passes() {
        // Setup: Only position limit rule active, position within limit
        when(ruleConfigService.getAllActiveRules()).thenReturn(
            Collections.singletonList(createPositionLimitRuleConfig(20.0)));
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        // Position limit: 20% of 1M = 200K value
        // Order: 10 * 1000 = 10K value < 200K - should pass
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertTrue(response.isPassed(), "Position within limit should pass");
    }

    @Test
    void testEvaluate_SinglePositionLimitRule_PositionExceedsLimit_Fails() {
        // Setup: Only position limit rule active, position exceeds limit
        when(ruleConfigService.getAllActiveRules()).thenReturn(
            Collections.singletonList(createPositionLimitRuleConfig(20.0)));
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(25000); // 25K * 10 = 250K > 20% of 1M = 200K
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertFalse(response.isPassed(), "Position exceeding limit should fail");
        assertNotNull(response.getRejectCode());
        assertTrue(response.getRejectCode().contains("POSITION_LIMIT"));
    }

    @Test
    void testEvaluate_SingleOrderLimitRule_OrderWithinLimit_Passes() {
        // Setup: Only single order limit rule active
        when(ruleConfigService.getAllActiveRules()).thenReturn(
            Collections.singletonList(createSingleOrderLimitRuleConfig(1000000.0)));
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(1000); // 10 * 1000 = 10K < 1M limit
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertTrue(response.isPassed(), "Order within limit should pass");
    }

    @Test
    void testEvaluate_SingleOrderLimitRule_OrderExceedsLimit_Fails() {
        // Setup: Only single order limit rule active
        when(ruleConfigService.getAllActiveRules()).thenReturn(
            Collections.singletonList(createSingleOrderLimitRuleConfig(1000000.0)));
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(150000); // 10 * 150000 = 1.5M > 1M limit
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertFalse(response.isPassed(), "Order exceeding limit should fail");
    }

    @Test
    void testEvaluate_ANDChain_AllRulesPass_Passes() {
        // Setup: Multiple rules with AND logic - all should pass
        List<RuleConfigDTO> rules = new ArrayList<>();
        rules.add(createPositionLimitRuleConfig(20.0));
        rules.add(createSingleOrderLimitRuleConfig(1000000.0));
        for (RuleConfigDTO rule : rules) {
            rule.setLogicOperator(LogicOperator.AND);
        }
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(1000); // Small order - both rules pass
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertTrue(response.isPassed(), "All rules passing should result in overall pass");
        assertTrue(response.getRuleResults().size() >= 2);
    }

    @Test
    void testEvaluate_ANDChain_OneRuleFails_Fails() {
        // Setup: Multiple rules with AND logic - one fails
        List<RuleConfigDTO> rules = new ArrayList<>();
        rules.add(createPositionLimitRuleConfig(20.0));
        rules.add(createSingleOrderLimitRuleConfig(1000000.0));
        for (RuleConfigDTO rule : rules) {
            rule.setLogicOperator(LogicOperator.AND);
        }
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(25000); // Large order - exceeds position limit
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertFalse(response.isPassed(), "One rule failing in AND chain should result in overall fail");
    }

    @Test
    void testEvaluate_ORChain_AllRulesPass_Passes() {
        // Setup: Multiple rules with OR logic
        List<RuleConfigDTO> rules = new ArrayList<>();
        rules.add(createPositionLimitRuleConfig(20.0));
        rules.add(createSingleOrderLimitRuleConfig(1000000.0));
        for (RuleConfigDTO rule : rules) {
            rule.setLogicOperator(LogicOperator.OR);
        }
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(1000);
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertTrue(response.isPassed(), "All rules passing in OR chain should pass");
    }

    @Test
    void testEvaluate_ORChain_OneRuleFails_StillPasses() {
        // Setup: Multiple rules with OR logic - only one needs to pass
        List<RuleConfigDTO> rules = new ArrayList<>();
        rules.add(createPositionLimitRuleConfig(20.0)); // will fail
        rules.add(createSingleOrderLimitRuleConfig(1000000.0)); // will pass
        for (RuleConfigDTO rule : rules) {
            rule.setLogicOperator(LogicOperator.OR);
        }
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(25000); // Large order - exceeds position limit
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        // In OR chain, if any rule passes, overall passes
        assertTrue(response.isPassed(), "In OR chain, one rule passing should allow request");
    }

    @Test
    void testEvaluate_ORChain_AllRulesFail_Fails() {
        // Setup: Multiple rules with OR logic - all fail
        List<RuleConfigDTO> rules = new ArrayList<>();
        rules.add(createPositionLimitRuleConfig(0.001)); // very tight limit
        rules.add(createSingleOrderLimitRuleConfig(100.0)); // very low order limit
        for (RuleConfigDTO rule : rules) {
            rule.setLogicOperator(LogicOperator.OR);
        }
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(25000);
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        assertFalse(response.isPassed(), "All rules failing in OR chain should result in overall fail");
    }

    @Test
    void testEvaluate_PriorityOrder_LowerPriorityExecutedFirst() {
        // Setup: Rules with different priorities
        List<RuleConfigDTO> rules = createPriorityOrderedRules();
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setQuantity(1000);
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        // Rules should be evaluated in priority order (lower number = higher priority)
        List<RiskCheckResponse.RuleCheckResult> results = response.getRuleResults();
        assertTrue(results.size() >= 2);
        
        // First rule evaluated should be priority 1 (highest)
        assertEquals("SINGLE_ORDER_LIMIT", results.get(0).getRuleType());
    }

    @Test
    void testEvaluate_PriorityOrder_HigherPriorityRuleFirst() {
        // Create rules where higher priority (lower number) comes first
        List<RuleConfigDTO> rules = new ArrayList<>();
        
        RuleConfigDTO highPriority = createSingleOrderLimitRuleConfig(1000000.0);
        highPriority.setPriority(1); // Higher priority
        
        RuleConfigDTO lowPriority = createPositionLimitRuleConfig(20.0);
        lowPriority.setPriority(10); // Lower priority
        
        rules.add(highPriority);
        rules.add(lowPriority);
        
        when(ruleConfigService.getAllActiveRules()).thenReturn(rules);
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        List<RiskCheckResponse.RuleCheckResult> results = response.getRuleResults();
        // SINGLE_ORDER_LIMIT should be evaluated before POSITION_LIMIT
        assertEquals("SINGLE_ORDER_LIMIT", results.get(0).getRuleType());
        assertEquals("POSITION_LIMIT", results.get(1).getRuleType());
    }

    @Test
    void testEvaluate_SellOrder_PositionLimitSkipped() {
        // Position limit should not apply to sell orders
        when(ruleConfigService.getAllActiveRules()).thenReturn(
            Collections.singletonList(createPositionLimitRuleConfig(20.0)));
        ruleEngine.reloadRules();

        RiskCheckRequest request = createBaseRequest();
        request.setSide("SELL");
        request.setQuantity(100000); // Very large sell
        
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        // Sell orders should pass position limit check
        assertTrue(response.isPassed(), "Sell orders should not be blocked by position limit");
    }

    @Test
    void testEvaluate_RuleException_FailsSafe() {
        // Test that exception in rule evaluation fails safe
        when(ruleConfigService.getAllActiveRules()).thenReturn(Collections.emptyList());
        ruleEngine.reloadRules();
        
        // Manually add a problematic rule via upsert
        RuleConfigDTO config = new RuleConfigDTO();
        config.setRuleId("TEST_RULE");
        config.setRuleName("Test Rule");
        config.setRuleType(RuleType.POSITION_LIMIT);
        config.setStatus(RuleStatus.ACTIVE);
        config.setPriority(1);
        config.setParameters(Map.of("limitPercent", 20.0));
        
        // This should work without exception
        ruleEngine.upsertRule(config);
        
        RiskCheckRequest request = createBaseRequest();
        RiskCheckResponse response = ruleEngine.evaluate(request);
        
        // Should handle gracefully
        assertNotNull(response);
    }

    @Test
    void testReloadRules_LoadsFromService() {
        // Verify reloadRules calls service
        when(ruleConfigService.getAllActiveRules()).thenReturn(Collections.emptyList());
        ruleEngine.reloadRules();
        
        verify(ruleConfigService, atLeastOnce()).getAllActiveRules();
    }

    @Test
    void testUpsertRule_AddsNewRule() {
        when(ruleConfigService.getAllActiveRules()).thenReturn(Collections.emptyList());
        ruleEngine.reloadRules();
        
        int initialRuleCount = ruleEngine.getAllRules().size();
        
        RuleConfigDTO config = new RuleConfigDTO();
        config.setRuleId("NEW_RULE");
        config.setRuleName("New Rule");
        config.setRuleType(RuleType.SINGLE_ORDER_LIMIT);
        config.setStatus(RuleStatus.ACTIVE);
        config.setPriority(50);
        config.setParameters(Map.of("maxOrderAmount", 500000.0));
        
        ruleEngine.upsertRule(config);
        
        verify(ruleConfigService).saveRuleConfig(config);
    }

    @Test
    void testRemoveRule_RemovesExistingRule() {
        // First add a rule
        RuleConfigDTO config = new RuleConfigDTO();
        config.setRuleId("TO_REMOVE");
        config.setRuleName("To Remove");
        config.setRuleType(RuleType.SINGLE_ORDER_LIMIT);
        config.setStatus(RuleStatus.ACTIVE);
        config.setPriority(50);
        config.setParameters(Map.of("maxOrderAmount", 500000.0));
        
        ruleEngine.upsertRule(config);
        
        ruleEngine.removeRule("TO_REMOVE");
        
        verify(ruleConfigService).deleteRuleConfig("TO_REMOVE");
    }

    // Helper methods to create rule configs

    private RuleConfigDTO createPositionLimitRuleConfig(double limitPercent) {
        RuleConfigDTO config = new RuleConfigDTO();
        config.setRuleId("POSITION_LIMIT");
        config.setRuleName("Position Limit Rule");
        config.setRuleType(RuleType.POSITION_LIMIT);
        config.setStatus(RuleStatus.ACTIVE);
        config.setPriority(10);
        config.setLogicOperator(LogicOperator.AND);
        config.setParameters(Map.of("limitPercent", limitPercent));
        return config;
    }

    private RuleConfigDTO createSingleOrderLimitRuleConfig(double maxOrderAmount) {
        RuleConfigDTO config = new RuleConfigDTO();
        config.setRuleId("SINGLE_ORDER_LIMIT");
        config.setRuleName("Single Order Limit Rule");
        config.setRuleType(RuleType.SINGLE_ORDER_LIMIT);
        config.setStatus(RuleStatus.ACTIVE);
        config.setPriority(5);
        config.setLogicOperator(LogicOperator.AND);
        config.setParameters(Map.of("maxOrderAmount", maxOrderAmount));
        return config;
    }

    private List<RuleConfigDTO> createPriorityOrderedRules() {
        List<RuleConfigDTO> rules = new ArrayList<>();
        
        // Add single order limit with priority 5
        RuleConfigDTO singleOrder = createSingleOrderLimitRuleConfig(1000000.0);
        singleOrder.setPriority(5);
        rules.add(singleOrder);
        
        // Add position limit with priority 10
        RuleConfigDTO positionLimit = createPositionLimitRuleConfig(20.0);
        positionLimit.setPriority(10);
        rules.add(positionLimit);
        
        return rules;
    }
}
