package com.qts.biz.trade.aggregate;

import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.entity.AssetEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AssetAggregate
 * Tests aggregate root business rules
 */
public class AssetAggregateTest {

    private AssetEntity createTestEntity() {
        AssetEntity entity = new AssetEntity();
        entity.setAccountId(1001L);
        entity.setCurrency("CNY");
        entity.setTotalAssets(new BigDecimal("100000"));
        entity.setAvailableCash(new BigDecimal("80000"));
        entity.setFrozenCash(new BigDecimal("10000"));
        entity.setMarketValue(new BigDecimal("10000"));
        entity.setTotalProfitLoss(BigDecimal.ZERO);
        entity.setTodayProfitLoss(BigDecimal.ZERO);
        entity.setMargin(new BigDecimal("1000"));
        entity.setMaintenanceMargin(new BigDecimal("500"));
        entity.setRiskLevel("NORMAL");
        return entity;
    }

    @Nested
    @DisplayName("freeze tests")
    class FreezeTests {

        @Test
        @DisplayName("Should freeze cash successfully")
        void testFreeze_Success() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            aggregate.freeze(new BigDecimal("5000"));

            // Then
            assertEquals(new BigDecimal("75000"), entity.getAvailableCash());
            assertEquals(new BigDecimal("15000"), entity.getFrozenCash());
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void testFreeze_NegativeAmount() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> aggregate.freeze(new BigDecimal("-100")));
        }

        @Test
        @DisplayName("Should throw exception for null amount")
        void testFreeze_NullAmount() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> aggregate.freeze(null));
        }

        @Test
        @DisplayName("Should throw exception when amount exceeds available cash")
        void testFreeze_InsufficientCash() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> aggregate.freeze(new BigDecimal("100000")));
        }
    }

    @Nested
    @DisplayName("unfreeze tests")
    class UnfreezeTests {

        @Test
        @DisplayName("Should unfreeze cash successfully")
        void testUnfreeze_Success() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            aggregate.unfreeze(new BigDecimal("5000"));

            // Then
            assertEquals(new BigDecimal("85000"), entity.getAvailableCash());
            assertEquals(new BigDecimal("5000"), entity.getFrozenCash());
        }

        @Test
        @DisplayName("Should throw exception for negative amount")
        void testUnfreeze_NegativeAmount() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> aggregate.unfreeze(new BigDecimal("-100")));
        }

        @Test
        @DisplayName("Should throw exception when amount exceeds frozen cash")
        void testUnfreeze_InsufficientFrozenCash() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> aggregate.unfreeze(new BigDecimal("20000")));
        }
    }

    @Nested
    @DisplayName("applyTrade tests")
    class ApplyTradeTests {

        @Test
        @DisplayName("Should apply BUY trade correctly")
        void testApplyTrade_Buy() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            TradeDTO trade = new TradeDTO();
            trade.setTradeId("TRADE001");
            trade.setAccountId(1001L);
            trade.setSymbol("BTC-USDT");
            trade.setSide("BUY");
            trade.setPrice(new BigDecimal("50000"));
            trade.setQuantity(new BigDecimal("1"));
            trade.setAmount(new BigDecimal("50000"));
            trade.setTradeTime(LocalDateTime.now());

            // When
            aggregate.applyTrade(trade);

            // Then
            assertEquals(new BigDecimal("30000"), entity.getAvailableCash());
            assertEquals(new BigDecimal("60000"), entity.getMarketValue());
        }

        @Test
        @DisplayName("Should apply SELL trade correctly")
        void testApplyTrade_Sell() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            TradeDTO trade = new TradeDTO();
            trade.setTradeId("TRADE002");
            trade.setAccountId(1001L);
            trade.setSymbol("BTC-USDT");
            trade.setSide("SELL");
            trade.setPrice(new BigDecimal("50000"));
            trade.setQuantity(new BigDecimal("1"));
            trade.setAmount(new BigDecimal("50000"));
            trade.setTradeTime(LocalDateTime.now());

            // When
            aggregate.applyTrade(trade);

            // Then
            assertEquals(new BigDecimal("130000"), entity.getAvailableCash());
            assertEquals(new BigDecimal("-40000"), entity.getMarketValue());
        }

        @Test
        @DisplayName("Should throw exception for null trade")
        void testApplyTrade_NullTrade() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> aggregate.applyTrade(null));
        }
    }

    @Nested
    @DisplayName("recalculate tests")
    class RecalculateTests {

        @Test
        @DisplayName("Should recalculate totals correctly")
        void testRecalculate_Success() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            aggregate.recalculate(new BigDecimal("20000"));

            // Then
            assertEquals(new BigDecimal("20000"), entity.getMarketValue());
            assertEquals(new BigDecimal("110000"), entity.getTotalAssets());
            assertEquals(new BigDecimal("2000"), entity.getMargin());
            assertEquals(new BigDecimal("1000"), entity.getMaintenanceMargin());
        }

        @Test
        @DisplayName("Should handle null market value")
        void testRecalculate_NullMarketValue() {
            // Given
            AssetEntity entity = createTestEntity();
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            aggregate.recalculate(null);

            // Then
            assertEquals(BigDecimal.ZERO, entity.getMarketValue());
        }
    }

    @Nested
    @DisplayName("calculateRiskLevel tests")
    class CalculateRiskLevelTests {

        @Test
        @DisplayName("Should return NORMAL when all conditions are met")
        void testCalculateRiskLevel_Normal() {
            // Given
            AssetEntity entity = createTestEntity();
            entity.setAvailableCash(new BigDecimal("80000"));
            entity.setTotalAssets(new BigDecimal("100000"));
            entity.setMaintenanceMargin(new BigDecimal("500"));
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            String riskLevel = aggregate.calculateRiskLevel();

            // Then
            assertEquals("NORMAL", riskLevel);
        }

        @Test
        @DisplayName("Should return DANGER when available cash is negative")
        void testCalculateRiskLevel_Danger_NegativeCash() {
            // Given
            AssetEntity entity = createTestEntity();
            entity.setAvailableCash(new BigDecimal("-1000"));
            entity.setMaintenanceMargin(new BigDecimal("500"));
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            String riskLevel = aggregate.calculateRiskLevel();

            // Then
            assertEquals("DANGER", riskLevel);
        }

        @Test
        @DisplayName("Should return DANGER when maintenance margin rate below 130%")
        void testCalculateRiskLevel_Danger_MarginRate() {
            // Given
            AssetEntity entity = createTestEntity();
            entity.setAvailableCash(new BigDecimal("80000"));
            entity.setTotalAssets(new BigDecimal("600")); // Total assets low
            entity.setMaintenanceMargin(new BigDecimal("500")); // Maintenance margin high
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            String riskLevel = aggregate.calculateRiskLevel();

            // Then
            assertEquals("DANGER", riskLevel);
        }

        @Test
        @DisplayName("Should return WARNING when maintenance margin rate below 150%")
        void testCalculateRiskLevel_Warning_MarginRate() {
            // Given
            AssetEntity entity = createTestEntity();
            entity.setAvailableCash(new BigDecimal("80000"));
            entity.setTotalAssets(new BigDecimal("700"));
            entity.setMaintenanceMargin(new BigDecimal("500"));
            AssetAggregate aggregate = new AssetAggregate(entity);

            // When
            String riskLevel = aggregate.calculateRiskLevel();

            // Then
            assertEquals("WARNING", riskLevel);
        }
    }
}
