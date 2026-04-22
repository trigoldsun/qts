package com.qts.biz.trade.aggregate;

import com.qts.biz.trade.dto.TradeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PositionAggregate
 * Tests aggregate behavior and business rules
 */
class PositionAggregateTest {

    private PositionAggregate aggregate;

    @BeforeEach
    void setUp() {
        aggregate = new PositionAggregate(1001L, "BTC-USDT", "Bitcoin");
    }

    @Nested
    @DisplayName("Constructor tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should initialize with default values")
        void testDefaultValues() {
            assertEquals(1001L, aggregate.getAccountId());
            assertEquals("BTC-USDT", aggregate.getSymbol());
            assertEquals("Bitcoin", aggregate.getSymbolName());
            assertEquals(BigDecimal.ZERO, aggregate.getQuantity());
            assertEquals(BigDecimal.ZERO, aggregate.getFrozenQuantity());
            assertEquals(BigDecimal.ZERO, aggregate.getCostPrice());
            assertEquals(BigDecimal.ZERO, aggregate.getMarketPrice());
            assertEquals(BigDecimal.ZERO, aggregate.getTodayBuyQuantity());
            assertEquals(BigDecimal.ZERO, aggregate.getTodaySellQuantity());
            assertNotNull(aggregate.getCreateTime());
            assertNotNull(aggregate.getUpdateTime());
        }
    }

    @Nested
    @DisplayName("freeze tests")
    class FreezeTests {

        @Test
        @DisplayName("Should freeze quantity successfully")
        void testFreeze_Success() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            
            aggregate.freeze(new BigDecimal("0.5"));
            
            assertEquals(new BigDecimal("0.5"), aggregate.getFrozenQuantity());
            assertEquals(1, aggregate.getDomainEvents().size());
        }

        @Test
        @DisplayName("Should throw exception when insufficient available quantity")
        void testFreeze_InsufficientAvailable() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            aggregate.setFrozenQuantity(new BigDecimal("0.8")); // 0.2 available only
            
            assertThrows(IllegalStateException.class, () -> 
                    aggregate.freeze(new BigDecimal("0.5")));
        }

        @Test
        @DisplayName("Should throw exception for zero quantity")
        void testFreeze_ZeroQuantity() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.freeze(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        void testFreeze_NegativeQuantity() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.freeze(new BigDecimal("-1.0")));
        }

        @Test
        @DisplayName("Should throw exception for null quantity")
        void testFreeze_NullQuantity() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.freeze(null));
        }
    }

    @Nested
    @DisplayName("unfreeze tests")
    class UnfreezeTests {

        @Test
        @DisplayName("Should unfreeze quantity successfully")
        void testUnfreeze_Success() {
            aggregate.setFrozenQuantity(new BigDecimal("0.5"));
            
            aggregate.unfreeze(new BigDecimal("0.3"));
            
            assertEquals(new BigDecimal("0.2"), aggregate.getFrozenQuantity());
            assertEquals(1, aggregate.getDomainEvents().size());
        }

        @Test
        @DisplayName("Should throw exception when unfreezing more than frozen")
        void testUnfreeze_ExceedsFrozen() {
            aggregate.setFrozenQuantity(new BigDecimal("0.3"));
            
            assertThrows(IllegalStateException.class, () -> 
                    aggregate.unfreeze(new BigDecimal("0.5")));
        }

        @Test
        @DisplayName("Should throw exception for zero quantity")
        void testUnfreeze_ZeroQuantity() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.unfreeze(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        void testUnfreeze_NegativeQuantity() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.unfreeze(new BigDecimal("-1.0")));
        }
    }

    @Nested
    @DisplayName("applyTrade tests")
    class ApplyTradeTests {

        @Test
        @DisplayName("Should apply BUY trade correctly")
        void testApplyTrade_Buy() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            aggregate.setCostPrice(new BigDecimal("50000"));
            
            TradeDTO trade = createTrade("T100", 1001L, "BTC-USDT", "BUY", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));
            
            aggregate.applyTrade(trade);
            
            // (1.0 * 50000 + 0.5 * 51000) / 1.5 = 50333.33...
            BigDecimal expectedCost = new BigDecimal("50333.33333333");
            assertEquals(new BigDecimal("1.5"), aggregate.getQuantity());
            assertEquals(new BigDecimal("0.5"), aggregate.getTodayBuyQuantity());
            assertEquals(new BigDecimal("25500"), aggregate.getTodayBuyAmount());
        }

        @Test
        @DisplayName("Should apply SELL trade correctly")
        void testApplyTrade_Sell() {
            aggregate.setQuantity(new BigDecimal("1.5"));
            aggregate.setCostPrice(new BigDecimal("50000"));
            aggregate.setFrozenQuantity(new BigDecimal("0.5"));
            
            TradeDTO trade = createTrade("T101", 1001L, "BTC-USDT", "SELL", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));
            
            aggregate.applyTrade(trade);
            
            assertEquals(new BigDecimal("1.0"), aggregate.getQuantity());
            // Frozen should also be reduced when selling
            assertEquals(new BigDecimal("0.5"), aggregate.getTodaySellQuantity());
            assertEquals(new BigDecimal("25500"), aggregate.getTodaySellAmount());
        }

        @Test
        @DisplayName("Should throw exception when selling more than owned")
        void testApplyTrade_SellExceedsOwned() {
            aggregate.setQuantity(new BigDecimal("0.3"));
            
            TradeDTO trade = createTrade("T102", 1001L, "BTC-USDT", "SELL", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));
            
            assertThrows(IllegalStateException.class, () -> 
                    aggregate.applyTrade(trade));
        }

        @Test
        @DisplayName("Should throw exception for symbol mismatch")
        void testApplyTrade_SymbolMismatch() {
            TradeDTO trade = createTrade("T103", 1001L, "ETH-USDT", "BUY", 
                    new BigDecimal("0.5"), new BigDecimal("3000"));
            
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.applyTrade(trade));
        }

        @Test
        @DisplayName("Should throw exception for null trade")
        void testApplyTrade_NullTrade() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.applyTrade(null));
        }
    }

    @Nested
    @DisplayName("recalculate tests")
    class RecalculateTests {

        @Test
        @DisplayName("Should update market price")
        void testRecalculate_Success() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            aggregate.setCostPrice(new BigDecimal("50000"));
            aggregate.setMarketPrice(new BigDecimal("51000"));
            
            aggregate.recalculate(new BigDecimal("52000"));
            
            assertEquals(new BigDecimal("52000"), aggregate.getMarketPrice());
        }

        @Test
        @DisplayName("Should throw exception for zero market price")
        void testRecalculate_ZeroPrice() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.recalculate(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should throw exception for negative market price")
        void testRecalculate_NegativePrice() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.recalculate(new BigDecimal("-100")));
        }

        @Test
        @DisplayName("Should throw exception for null market price")
        void testRecalculate_NullPrice() {
            assertThrows(IllegalArgumentException.class, () -> 
                    aggregate.recalculate(null));
        }
    }

    @Nested
    @DisplayName("getAvailableQuantity tests")
    class GetAvailableQuantityTests {

        @Test
        @DisplayName("Should return quantity minus frozen")
        void testGetAvailableQuantity() {
            aggregate.setQuantity(new BigDecimal("1.5"));
            aggregate.setFrozenQuantity(new BigDecimal("0.5"));
            
            assertEquals(new BigDecimal("1.0"), aggregate.getAvailableQuantity());
        }
    }

    @Nested
    @DisplayName("getMarketValue tests")
    class GetMarketValueTests {

        @Test
        @DisplayName("Should return quantity times market price")
        void testGetMarketValue() {
            aggregate.setQuantity(new BigDecimal("2.0"));
            aggregate.setMarketPrice(new BigDecimal("50000"));
            
            assertEquals(new BigDecimal("100000"), aggregate.getMarketValue());
        }
    }

    @Nested
    @DisplayName("getProfitLoss tests")
    class GetProfitLossTests {

        @Test
        @DisplayName("Should calculate profit correctly")
        void testGetProfitLoss_Profit() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            aggregate.setCostPrice(new BigDecimal("50000"));
            aggregate.setMarketPrice(new BigDecimal("51000"));
            
            assertEquals(new BigDecimal("1000"), aggregate.getProfitLoss());
        }

        @Test
        @DisplayName("Should calculate loss correctly")
        void testGetProfitLoss_Loss() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            aggregate.setCostPrice(new BigDecimal("50000"));
            aggregate.setMarketPrice(new BigDecimal("49000"));
            
            assertEquals(new BigDecimal("-1000"), aggregate.getProfitLoss());
        }
    }

    @Nested
    @DisplayName("getProfitLossRatio tests")
    class GetProfitLossRatioTests {

        @Test
        @DisplayName("Should calculate ratio correctly")
        void testGetProfitLossRatio() {
            aggregate.setCostPrice(new BigDecimal("50000"));
            aggregate.setMarketPrice(new BigDecimal("51000"));
            
            // (51000 - 50000) / 50000 = 0.02
            assertEquals(new BigDecimal("0.0200"), aggregate.getProfitLossRatio());
        }

        @Test
        @DisplayName("Should return zero when cost price is zero")
        void testGetProfitLossRatio_ZeroCost() {
            aggregate.setCostPrice(BigDecimal.ZERO);
            aggregate.setMarketPrice(new BigDecimal("51000"));
            
            assertEquals(BigDecimal.ZERO, aggregate.getProfitLossRatio());
        }
    }

    @Nested
    @DisplayName("Domain events tests")
    class DomainEventsTests {

        @Test
        @DisplayName("Should clear events after getDomainEvents")
        void testGetDomainEvents_ClearAfterRead() {
            aggregate.setQuantity(new BigDecimal("1.0"));
            aggregate.freeze(new BigDecimal("0.1"));
            
            assertEquals(1, aggregate.getDomainEvents().size());
            assertEquals(0, aggregate.getDomainEvents().size()); // Cleared
        }
    }

    // Helper method

    private TradeDTO createTrade(String tradeId, Long accountId, String symbol, 
            String side, BigDecimal quantity, BigDecimal price) {
        TradeDTO trade = new TradeDTO();
        trade.setTradeId(tradeId);
        trade.setAccountId(accountId);
        trade.setSymbol(symbol);
        trade.setSide(side);
        trade.setQuantity(quantity);
        trade.setPrice(price);
        trade.setAmount(quantity.multiply(price));
        trade.setTradeTime(LocalDateTime.now());
        return trade;
    }
}