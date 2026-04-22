package com.qts.biz.risk.precheck;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;
import com.qts.biz.risk.precheck.dto.PreCheckResponse;
import com.qts.biz.risk.precheck.validator.ValidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PreCheckService
 */
class PreCheckServiceTest {

    private PreCheckService preCheckService;
    private ValidationContext context;

    @BeforeEach
    void setUp() {
        preCheckService = new PreCheckService();
        
        Map<String, BigDecimal> positionMap = new HashMap<>();
        positionMap.put("600000", BigDecimal.valueOf(1000)); // 1000 shares

        Map<String, BigDecimal> priceMap = new HashMap<>();
        priceMap.put("600000", BigDecimal.valueOf(10.0)); // previous close 10.0

        context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(positionMap)
                .priceMap(priceMap)
                .totalAssets(BigDecimal.valueOf(1000000)) // 100万
                .availableCash(BigDecimal.valueOf(500000)) // 50万
                .currentTime(LocalDateTime.of(2026, 4, 22, 10, 30)) // trading hours
                .build();
    }

    @Test
    void testCheck_TradingSessionValid() {
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
        assertTrue(response.getData().getReasons().isEmpty());
    }

    @Test
    void testCheck_TradingSessionInvalid() {
        // Set time outside trading session
        ValidationContext contextOffHours = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 15, 0)) // outside trading hours
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
        assertFalse(response.getData().getReasons().isEmpty());
        assertTrue(response.getMessage().contains("precheck failed"));
    }

    @Test
    void testCheck_CallAuctionSession() {
        // Test 9:15-9:25 call auction session
        ValidationContext contextAuction = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(new HashMap<>())
                .totalAssets(BigDecimal.valueOf(1000000))
                .availableCash(BigDecimal.valueOf(500000))
                .currentTime(LocalDateTime.of(2026, 4, 22, 9, 20))
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
    void testCheck_PositionLimitExceeded() {
        // Position limit: 20% of 1,000,000 = 200,000 shares at price 10 = 2,000,000
        // Current position 1000 + order 30000 = 31000 > 20000 (0.02 * 1000000 / 10)
        ValidationContext contextLowLimit = new ValidationContext.Builder()
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
        request.setPrice(BigDecimal.valueOf(10.0));
        request.setQuantity(BigDecimal.valueOf(30000)); // Large order
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, contextLowLimit);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("POSITION_LIMIT")));
    }

    @Test
    void testCheck_InsufficientFunds() {
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
        request.setQuantity(BigDecimal.valueOf(100));
        request.setMarginRate(BigDecimal.valueOf(0.1)); // 10% margin

        PreCheckResponse response = preCheckService.check(request, contextLowFunds);

        assertEquals(1, response.getCode());
        assertFalse(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().stream()
                .anyMatch(r -> r.contains("FUND_CHECK")));
    }

    @Test
    void testCheck_PriceLimitExceeded() {
        ValidationContext contextWithPrice = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(new HashMap<>())
                .priceMap(Map.of("600000", BigDecimal.valueOf(10.0))) // previous close 10.0
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
    void testCheck_SellOrder_NoFundCheck() {
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
    void testCheck_AllValid() {
        ValidationContext context = new ValidationContext.Builder()
                .accountId(12345L)
                .positionMap(Map.of("600000", BigDecimal.valueOf(1000)))
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
        request.setQuantity(BigDecimal.valueOf(1000)); // Small order
        request.setMarginRate(BigDecimal.valueOf(0.1));

        PreCheckResponse response = preCheckService.check(request, context);

        assertEquals(0, response.getCode());
        assertTrue(response.getData().isCanTrade());
        assertTrue(response.getData().getReasons().isEmpty());
    }
}