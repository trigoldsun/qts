package com.qts.biz.trade.service;

import com.qts.biz.trade.BaseTest;
import com.qts.biz.trade.dto.PagedResult;
import com.qts.biz.trade.dto.PositionDTO;
import com.qts.biz.trade.dto.PositionQuery;
import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.entity.PositionEntity;
import com.qts.biz.trade.repository.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PositionManager
 * Tests position management operations with mocked repository
 */
class PositionManagerTest extends BaseTest {

    @Mock
    private PositionRepository positionRepository;

    private PositionManager positionManager;

    @BeforeEach
    void setUp() {
        super.onSetUp();
        positionManager = new PositionManager(positionRepository);
    }

    @Nested
    @DisplayName("getPosition tests")
    class GetPositionTests {

        @Test
        @DisplayName("Should return position when exists")
        void testGetPosition_Exists() {
            // Given
            Long accountId = 1001L;
            String symbol = "BTC-USDT";
            PositionEntity entity = createPositionEntity(accountId, symbol, 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.of(entity));

            // When
            PositionDTO result = positionManager.getPosition(accountId, symbol);

            // Then
            assertNotNull(result);
            assertEquals(symbol, result.getSymbol());
            assertEquals(new BigDecimal("1.5"), result.getQuantity());
            assertEquals(new BigDecimal("1.0"), result.getAvailableQuantity()); // 1.5 - 0.5
            assertEquals(new BigDecimal("0.5"), result.getFrozenQuantity());
            assertEquals(new BigDecimal("50000"), result.getCostPrice());
            assertEquals(new BigDecimal("51000"), result.getMarketPrice());
            
            BigDecimal expectedMarketValue = new BigDecimal("1.5").multiply(new BigDecimal("51000"));
            assertEquals(expectedMarketValue, result.getMarketValue());
            
            BigDecimal expectedProfitLoss = new BigDecimal("51000").subtract(new BigDecimal("50000")).multiply(new BigDecimal("1.5"));
            assertEquals(expectedProfitLoss, result.getProfitLoss());
        }

        @Test
        @DisplayName("Should return null when position does not exist")
        void testGetPosition_NotFound() {
            // Given
            Long accountId = 1001L;
            String symbol = "ETH-USDT";
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.empty());

            // When
            PositionDTO result = positionManager.getPosition(accountId, symbol);

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("Should throw exception when accountId is null")
        void testGetPosition_NullAccountId() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.getPosition(null, "BTC-USDT"));
        }

        @Test
        @DisplayName("Should throw exception when symbol is null")
        void testGetPosition_NullSymbol() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.getPosition(1001L, null));
        }

        @Test
        @DisplayName("Should throw exception when symbol is empty")
        void testGetPosition_EmptySymbol() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.getPosition(1001L, ""));
        }
    }

    @Nested
    @DisplayName("listPositions tests")
    class ListPositionsTests {

        @Test
        @DisplayName("Should return paginated positions")
        void testListPositions_Paginated() {
            // Given
            Long accountId = 1001L;
            PositionQuery query = new PositionQuery();
            
            List<PositionEntity> entities = List.of(
                    createPositionEntity(accountId, "BTC-USDT", new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000")),
                    createPositionEntity(accountId, "ETH-USDT", new BigDecimal("2.0"), BigDecimal.ZERO, new BigDecimal("3000"), new BigDecimal("3100"))
            );
            when(positionRepository.findByAccountId(accountId, query)).thenReturn(entities);

            // When
            PagedResult<PositionDTO> result = positionManager.listPositions(accountId, query, 1, 10);

            // Then
            assertNotNull(result);
            assertEquals(2, result.getItems().size());
            assertEquals(1, result.getPage());
            assertEquals(10, result.getPageSize());
            assertEquals(2, result.getTotalItems());
        }

        @Test
        @DisplayName("Should return empty result when no positions")
        void testListPositions_Empty() {
            // Given
            Long accountId = 9999L;
            when(positionRepository.findByAccountId(eq(accountId), any())).thenReturn(List.of());

            // When
            PagedResult<PositionDTO> result = positionManager.listPositions(accountId, null, 1, 10);

            // Then
            assertNotNull(result);
            assertTrue(result.getItems().isEmpty());
            assertEquals(0, result.getTotalItems());
        }

        @Test
        @DisplayName("Should use default pagination when not specified")
        void testListPositions_DefaultPagination() {
            // Given
            Long accountId = 1001L;
            when(positionRepository.findByAccountId(accountId, null)).thenReturn(List.of());

            // When
            PagedResult<PositionDTO> result = positionManager.listPositions(accountId, null);

            // Then
            assertEquals(1, result.getPage());
            assertEquals(100, result.getPageSize());
        }

        @Test
        @DisplayName("Should throw exception when accountId is null")
        void testListPositions_NullAccountId() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.listPositions(null, null, 1, 10));
        }
    }

    @Nested
    @DisplayName("freezePosition tests")
    class FreezePositionTests {

        @Test
        @DisplayName("Should freeze position successfully when sufficient available quantity")
        void testFreezePosition_Success() {
            // Given
            Long accountId = 1001L;
            String symbol = "BTC-USDT";
            BigDecimal qty = new BigDecimal("0.5");
            
            PositionEntity entity = createPositionEntity(accountId, symbol, 
                    new BigDecimal("1.5"), new BigDecimal("0.0"), new BigDecimal("50000"), new BigDecimal("51000"));
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.of(entity));
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.freezePosition(accountId, symbol, qty);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            assertEquals(new BigDecimal("0.5"), saved.getFrozenQuantity());
        }

        @Test
        @DisplayName("Should throw exception when insufficient available quantity")
        void testFreezePosition_InsufficientQuantity() {
            // Given
            Long accountId = 1001L;
            String symbol = "BTC-USDT";
            BigDecimal qty = new BigDecimal("2.0"); // More than available (1.0)
            
            PositionEntity entity = createPositionEntity(accountId, symbol, 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.of(entity));

            // When/Then
            assertThrows(IllegalStateException.class, () -> 
                    positionManager.freezePosition(accountId, symbol, qty));
        }

        @Test
        @DisplayName("Should create new position when freezing for non-existent position")
        void testFreezePosition_NewPosition() {
            // Given
            Long accountId = 1001L;
            String symbol = "DOGE-USDT";
            BigDecimal qty = new BigDecimal("100");
            
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.empty());
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.freezePosition(accountId, symbol, qty);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            assertEquals(new BigDecimal("100"), saved.getFrozenQuantity());
            assertEquals(BigDecimal.ZERO, saved.getQuantity());
        }

        @Test
        @DisplayName("Should throw exception for zero quantity")
        void testFreezePosition_ZeroQuantity() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.freezePosition(1001L, "BTC-USDT", BigDecimal.ZERO));
        }

        @Test
        @DisplayName("Should throw exception for negative quantity")
        void testFreezePosition_NegativeQuantity() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.freezePosition(1001L, "BTC-USDT", new BigDecimal("-1.0")));
        }
    }

    @Nested
    @DisplayName("unfreezePosition tests")
    class UnfreezePositionTests {

        @Test
        @DisplayName("Should unfreeze position successfully")
        void testUnfreezePosition_Success() {
            // Given
            Long accountId = 1001L;
            String symbol = "BTC-USDT";
            BigDecimal qty = new BigDecimal("0.3");
            
            PositionEntity entity = createPositionEntity(accountId, symbol, 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.of(entity));
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.unfreezePosition(accountId, symbol, qty);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            assertEquals(new BigDecimal("0.2"), saved.getFrozenQuantity()); // 0.5 - 0.3
        }

        @Test
        @DisplayName("Should throw exception when position not found")
        void testUnfreezePosition_NotFound() {
            // Given
            Long accountId = 1001L;
            String symbol = "NONEXISTENT";
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalStateException.class, () -> 
                    positionManager.unfreezePosition(accountId, symbol, new BigDecimal("1.0")));
        }

        @Test
        @DisplayName("Should throw exception when unfreezing more than frozen")
        void testUnfreezePosition_ExceedsFrozen() {
            // Given
            Long accountId = 1001L;
            String symbol = "BTC-USDT";
            BigDecimal qty = new BigDecimal("1.0"); // More than frozen (0.5)
            
            PositionEntity entity = createPositionEntity(accountId, symbol, 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            when(positionRepository.findByAccountIdAndSymbol(accountId, symbol))
                    .thenReturn(Optional.of(entity));

            // When/Then
            assertThrows(IllegalStateException.class, () -> 
                    positionManager.unfreezePosition(accountId, symbol, qty));
        }
    }

    @Nested
    @DisplayName("addPosition tests")
    class AddPositionTests {

        @Test
        @DisplayName("Should add position on BUY trade")
        void testAddPosition_BuyTrade() {
            // Given
            TradeDTO trade = createTrade("T123", 1001L, "BTC-USDT", "BUY", 
                    new BigDecimal("0.5"), new BigDecimal("50000"));
            
            when(positionRepository.findByAccountIdAndSymbol(1001L, "BTC-USDT"))
                    .thenReturn(Optional.empty());
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.addPosition(trade);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            
            assertEquals(new BigDecimal("0.5"), saved.getQuantity());
            assertEquals(new BigDecimal("50000"), saved.getCostPrice());
            assertEquals(new BigDecimal("0.5"), saved.getTodayBuyQuantity());
            assertEquals(new BigDecimal("25000"), saved.getTodayBuyAmount());
        }

        @Test
        @DisplayName("Should update cost price using weighted average on additional BUY")
        void testAddPosition_WeightedAverage() {
            // Given - existing position with cost price 50000
            PositionEntity existing = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000"));
            
            TradeDTO trade = createTrade("T124", 1001L, "BTC-USDT", "BUY", 
                    new BigDecimal("1.0"), new BigDecimal("52000")); // Buy more at higher price
            
            when(positionRepository.findByAccountIdAndSymbol(1001L, "BTC-USDT"))
                    .thenReturn(Optional.of(existing));
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.addPosition(trade);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            
            // (1.0 * 50000 + 1.0 * 52000) / 2.0 = 51000
            assertEquals(new BigDecimal("2.0"), saved.getQuantity());
            assertEquals(new BigDecimal("51000"), saved.getCostPrice());
        }

        @Test
        @DisplayName("Should ignore non-BUY trades")
        void testAddPosition_SellTrade() {
            // Given
            TradeDTO trade = createTrade("T125", 1001L, "BTC-USDT", "SELL", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));

            // When
            positionManager.addPosition(trade);

            // Then
            verify(positionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when trade is null")
        void testAddPosition_NullTrade() {
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> 
                    positionManager.addPosition(null));
        }
    }

    @Nested
    @DisplayName("reducePosition tests")
    class ReducePositionTests {

        @Test
        @DisplayName("Should reduce position on SELL trade")
        void testReducePosition_SellTrade() {
            // Given
            PositionEntity existing = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.5"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000"));
            
            TradeDTO trade = createTrade("T126", 1001L, "BTC-USDT", "SELL", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));
            
            when(positionRepository.findByAccountIdAndSymbol(1001L, "BTC-USDT"))
                    .thenReturn(Optional.of(existing));
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.reducePosition(trade);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            
            assertEquals(new BigDecimal("1.0"), saved.getQuantity());
            assertEquals(new BigDecimal("0.5"), saved.getTodaySellQuantity());
            assertEquals(new BigDecimal("25500"), saved.getTodaySellAmount());
        }

        @Test
        @DisplayName("Should throw exception when position not found for reduce")
        void testReducePosition_NotFound() {
            // Given
            TradeDTO trade = createTrade("T127", 1001L, "NONEXISTENT", "SELL", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));
            
            when(positionRepository.findByAccountIdAndSymbol(1001L, "NONEXISTENT"))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThrows(IllegalStateException.class, () -> 
                    positionManager.reducePosition(trade));
        }

        @Test
        @DisplayName("Should throw exception when selling more than owned")
        void testReducePosition_InsufficientQuantity() {
            // Given
            PositionEntity existing = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("0.3"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000"));
            
            TradeDTO trade = createTrade("T128", 1001L, "BTC-USDT", "SELL", 
                    new BigDecimal("0.5"), new BigDecimal("51000"));
            
            when(positionRepository.findByAccountIdAndSymbol(1001L, "BTC-USDT"))
                    .thenReturn(Optional.of(existing));

            // When/Then
            assertThrows(IllegalStateException.class, () -> 
                    positionManager.reducePosition(trade));
        }

        @Test
        @DisplayName("Should ignore non-SELL trades")
        void testReducePosition_BuyTrade() {
            // Given
            TradeDTO trade = createTrade("T129", 1001L, "BTC-USDT", "BUY", 
                    new BigDecimal("0.5"), new BigDecimal("50000"));

            // When
            positionManager.reducePosition(trade);

            // Then
            verify(positionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateMarketPrice tests")
    class UpdateMarketPriceTests {

        @Test
        @DisplayName("Should update market price successfully")
        void testUpdateMarketPrice_Success() {
            // Given
            PositionEntity existing = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000"));
            BigDecimal newPrice = new BigDecimal("52000");
            
            when(positionRepository.findByAccountIdAndSymbol(1001L, "BTC-USDT"))
                    .thenReturn(Optional.of(existing));
            when(positionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            // When
            positionManager.updateMarketPrice(1001L, "BTC-USDT", newPrice);

            // Then
            ArgumentCaptor<PositionEntity> captor = ArgumentCaptor.forClass(PositionEntity.class);
            verify(positionRepository).save(captor.capture());
            PositionEntity saved = captor.getValue();
            assertEquals(newPrice, saved.getMarketPrice());
        }

        @Test
        @DisplayName("Should do nothing when position not found")
        void testUpdateMarketPrice_NotFound() {
            // Given
            when(positionRepository.findByAccountIdAndSymbol(1001L, "NONEXISTENT"))
                    .thenReturn(Optional.empty());

            // When
            positionManager.updateMarketPrice(1001L, "NONEXISTENT", new BigDecimal("52000"));

            // Then
            verify(positionRepository, never()).save(any());
        }
    }

    // Helper methods

    private PositionEntity createPositionEntity(Long accountId, String symbol, 
            BigDecimal quantity, BigDecimal frozen, BigDecimal costPrice, BigDecimal marketPrice) {
        PositionEntity entity = new PositionEntity();
        entity.setId(System.currentTimeMillis());
        entity.setAccountId(accountId);
        entity.setSymbol(symbol);
        entity.setSymbolName(symbol);
        entity.setQuantity(quantity);
        entity.setFrozenQuantity(frozen);
        entity.setCostPrice(costPrice);
        entity.setMarketPrice(marketPrice);
        entity.setTodayBuyQuantity(BigDecimal.ZERO);
        entity.setTodaySellQuantity(BigDecimal.ZERO);
        entity.setTodayBuyAmount(BigDecimal.ZERO);
        entity.setTodaySellAmount(BigDecimal.ZERO);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }

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