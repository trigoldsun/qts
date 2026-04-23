package com.qts.biz.risk.precheck;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;
import com.qts.biz.risk.precheck.dto.PreCheckResponse;
import com.qts.biz.risk.precheck.service.AssetService;
import com.qts.biz.risk.precheck.service.MarketDataService;
import com.qts.biz.risk.precheck.service.PositionService;
import com.qts.biz.risk.precheck.validator.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PreCheckService
 * Tests risk pre-trade validation including:
 * - Position limit (single stock <= 20% of total assets, based on quantity)
 * - Fund check (available funds >= order amount * margin rate)
 * - Trading session (9:15-9:25, 9:30-11:30, 13:00-14:57)
 * - Price limit (±10% of previous close)
 */
class PreCheckServiceTest {

    private PreCheckService preCheckService;

    @Mock
    private PositionService positionService;

    @Mock
    private AssetService assetService;

    @Mock
    private MarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        preCheckService = new PreCheckService(positionService, assetService, marketDataService);
    }

    /**
     * Helper to create a validation context for trading hours (10:30)
     */
    private ValidationContext createTradingHoursContext() {
        Map<String, BigDecimal> positionMap = new HashMap<>();
        positionMap.put("600000", BigDecimal.valueOf(1000)); // 1000 shares

        Map<String, BigDecimal> priceMap = new HashMap<>();
        priceMap.put("600000", BigDecimal.valueOf(10.0)); // previous close 10.0

        return new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(positionMap)
                .priceMap(priceMap)
                .totalAssets(BigDecimal.valueOf(1000000)) // 100万
                .availableCash(BigDecimal.valueOf(500000)) // 50万
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30)) // trading hours
                .build();
    }

    @Test
    void testCheck_AllValid_ShouldPass() {
        ValidationContext context = createTradingHoursContext();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(1000)); // Small order - 10000 amount
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().isEmpty());
    }

    @Test
    void testCheck_TradingSessionValid_MorningSession() {
        ValidationContext context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30)) // 10:30 - valid trading hours
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
    }

    @Test
    void testCheck_TradingSessionValid_CallAuction() {
        // Test 9:15-9:25 call auction session
        ValidationContext contextAuction = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 9, 20)) // 9:20 - call auction
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextAuction);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
    }

    @Test
    void testCheck_TradingSessionValid_AfternoonSession() {
        // Test 13:00-14:57 afternoon session
        ValidationContext contextAfternoon = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 14, 30)) // 14:30 - afternoon session
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextAfternoon);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
    }

    @Test
    void testCheck_TradingSessionInvalid_BeforeOpen() {
        // Set time before 9:15
        ValidationContext contextOffHours = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 9, 10)) // 9:10 - before trading
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextOffHours);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("TRADING_SESSION")));
    }

    @Test
    void testCheck_TradingSessionInvalid_BetweenAuctionAndSession() {
        // 9:25-9:30 is between call auction and continuous trading
        ValidationContext contextBetween = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 9, 27)) // 9:27 - between auction and session
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextBetween);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("TRADING_SESSION")));
    }

    @Test
    void testCheck_TradingSessionInvalid_LunchBreak() {
        // 11:30-13:00 is lunch break
        ValidationContext contextLunch = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 12, 0)) // 12:00 - lunch break
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextLunch);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("TRADING_SESSION")));
    }

    @Test
    void testCheck_TradingSessionInvalid_AfterClose() {
        // After 14:57 is after trading hours
        ValidationContext contextAfter = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 15, 0)) // 15:00 - after close
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextAfter);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("TRADING_SESSION")));
    }

    @Test
    void testCheck_PositionLimitExceeded() {
        // Position limit: 20% of total assets based on quantity ratio
        // totalAssets=1M, position limit = 20% = 200,000 shares (at price 10)
        // Existing: 0 shares, Order: 25000 shares
        // newPositionRatio = (0 + 25000) / 1000000 = 0.025 = 2.5% < 20% - PASSES
        // Actually need 200001 or more to exceed 20%
        ValidationContext contextLowLimit = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>()) // 0 existing shares
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000)) // 100万
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(250000)); // 250000 shares = 25% of 1M - EXCEEDS 20%
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextLowLimit);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("POSITION_LIMIT")));
    }

    @Test
    void testCheck_PositionLimit_WithExistingPosition() {
        // Existing position 180000 shares (18% of 1M)
        // Adding 25000 shares would be 205000 total = 20.5% > 20% limit
        ValidationContext context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(Map.of("600000", BigDecimal.valueOf(180000)))
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(25000)); // Would make total 20.5%
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("POSITION_LIMIT")));
    }

    @Test
    void testCheck_PositionLimit_SellOrder_Passes() {
        // Sell orders don't affect position limit - should pass
        ValidationContext context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(Map.of("600000", BigDecimal.valueOf(100)))
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(100)) // Very low cash
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("SELL");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(50));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        // Sell should pass because position limit doesn't apply to sells
        assertTrue(response.getData().isCanTrade() || 
                   !response.getData().getReasons().stream().anyMatch(r -> r.contains("POSITION_LIMIT")));
    }

    @Test
    void testCheck_InsufficientFunds() {
        // Order: 1000 shares * 10.0 = 10000 amount
        // Margin required: 10000 * 0.1 = 1000
        // Available cash: 100 - insufficient
        ValidationContext contextLowFunds = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(100)) // Very low cash
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(1000)); // 10000 amount, needs 1000 margin
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextLowFunds);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("FUND_CHECK")));
    }

    @Test
    void testCheck_FundsSufficient() {
        // Order: 1000 shares * 10.0 = 10000 amount
        // Margin required: 10000 * 0.1 = 1000
        // Available cash: 5000 - sufficient
        ValidationContext context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(5000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(1000));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
    }

    @Test
    void testCheck_SellOrder_NoFundCheck() {
        // Sell orders don't require fund check for margin
        ValidationContext contextLowFunds = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(Map.of("600000", BigDecimal.valueOf(100)))
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(10)) // Very low, but selling doesn't need funds
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("SELL");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(50));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextLowFunds);

        // Should pass - sell doesn't require fund check
        assertTrue(response.getData().isCanTrade() || 
                   !response.getData().getReasons().stream().anyMatch(r -> r.contains("FUND_CHECK")));
    }

    @Test
    void testCheck_PriceLimitExceeded_TooHigh() {
        // Previous close 10.0, max allowed 11.0 (10% up)
        // Order price 12.0 exceeds limit
        ValidationContext contextWithPrice = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(12.0)); // 20% above previous close - exceeds 10% limit
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextWithPrice);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("PRICE_LIMIT")));
    }

    @Test
    void testCheck_PriceLimitExceeded_TooLow() {
        // Previous close 10.0, min allowed 9.0 (10% down)
        // Order price 8.0 exceeds limit
        ValidationContext contextWithPrice = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(8.0)); // 20% below previous close - exceeds 10% limit
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextWithPrice);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("PRICE_LIMIT")));
    }

    @Test
    void testCheck_PriceLimit_AtBoundary() {
        // Previous close 10.0, max allowed 11.0 (10% up)
        // Order price 11.0 at boundary - should pass
        ValidationContext contextWithPrice = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(11.0)); // Exactly 10% above - at boundary
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextWithPrice);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
    }

    @Test
    void testCheck_NoPreviousPrice_Passes() {
        // No previous close price - should skip price validation
        ValidationContext contextNoPrice = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>()) // No previous close
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30))
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(100.0)); // High price but no reference
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextNoPrice);

        // Should pass because no previous price to validate against
        assertTrue(response.getData().isCanTrade() ||
                   !response.getData().getReasons().stream().anyMatch(r -> r.contains("PRICE_LIMIT")));
    }

    @Test
    void testCheck_MultipleRulesFailed() {
        // Time outside trading AND insufficient funds
        ValidationContext context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0)))
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(100)) // Low funds
                .currentTime(LocalDateTime.of(2026, 4, 22, 15, 0)) // After hours
                .build();

        PreCheckRequest request = new PreCheckRequest();
        request.setAccountId(12345L);
        request.setSymbol("600000");
        request.setSide("BUY");
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(1000));
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        // Should have multiple failure reasons
        assertTrue(response.getData().getReasons().size() >= 2);
    }
}