package com.qts.biz.trade.service;

import com.qts.biz.trade.BaseTest;
import com.qts.biz.trade.aggregate.AssetAggregate;
import com.qts.biz.trade.dto.AssetDTO;
import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.entity.AssetEntity;
import com.qts.biz.trade.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssetManager
 * Tests fund management operations with mocked repository
 */
public class AssetManagerTest extends BaseTest {

    @Mock
    private AssetRepository assetRepository;

    private AssetManager assetManager;

    private AssetEntity createTestAsset(Long accountId) {
        AssetEntity entity = new AssetEntity();
        entity.setAccountId(accountId);
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

    @Override
    protected void onSetUp() {
        super.onSetUp();
        assetManager = new AssetManager(assetRepository);
    }

    @Nested
    @DisplayName("getAsset tests")
    class GetAssetTests {

        @Test
        @DisplayName("Should return asset DTO when account exists")
        void testGetAsset_Success() {
            // Given
            Long accountId = 1001L;
            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));

            // When
            AssetDTO result = assetManager.getAsset(accountId);

            // Then
            assertNotNull(result);
            assertEquals(accountId, result.getAccountId());
            assertEquals("CNY", result.getCurrency());
            assertEquals(new BigDecimal("100000"), result.getTotalAssets());
            assertEquals(new BigDecimal("80000"), result.getAvailableCash());
            assertEquals(new BigDecimal("10000"), result.getFrozenCash());
            assertEquals(AssetDTO.RiskLevel.NORMAL, result.getRiskLevel());
            verify(assetRepository).findByAccountId(accountId);
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void testGetAsset_NotFound() {
            // Given
            Long accountId = 9999L;
            when(assetRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

            // When / Then
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> assetManager.getAsset(accountId)
            );
            assertTrue(exception.getMessage().contains("Account not found"));
            verify(assetRepository).findByAccountId(accountId);
        }

        @Test
        @DisplayName("Should throw exception when account ID is null")
        void testGetAsset_NullAccountId() {
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.getAsset(null));
            verify(assetRepository, never()).findByAccountId(any());
        }
    }

    @Nested
    @DisplayName("freezeCash tests")
    class FreezeCashTests {

        @Test
        @DisplayName("Should freeze cash successfully")
        void testFreezeCash_Success() {
            // Given
            Long accountId = 1001L;
            BigDecimal amount = new BigDecimal("5000");
            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(entity));
            when(assetRepository.save(any(AssetEntity.class))).thenReturn(entity);

            // When
            assetManager.freezeCash(accountId, amount);

            // Then
            ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
            verify(assetRepository).save(captor.capture());

            AssetEntity savedEntity = captor.getValue();
            assertEquals(new BigDecimal("75000"), savedEntity.getAvailableCash());
            assertEquals(new BigDecimal("15000"), savedEntity.getFrozenCash());
        }

        @Test
        @DisplayName("Should throw exception when amount exceeds available cash")
        void testFreezeCash_InsufficientCash() {
            // Given
            Long accountId = 1001L;
            BigDecimal amount = new BigDecimal("100000"); // More than available
            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(entity));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.freezeCash(accountId, amount));
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void testFreezeCash_AccountNotFound() {
            // Given
            Long accountId = 9999L;
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.freezeCash(accountId, new BigDecimal("1000")));
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for null or negative amount")
        void testFreezeCash_InvalidAmount() {
            // Given
            Long accountId = 1001L;

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.freezeCash(accountId, null));
            assertThrows(IllegalArgumentException.class, () -> assetManager.freezeCash(accountId, BigDecimal.ZERO));
            assertThrows(IllegalArgumentException.class, () -> assetManager.freezeCash(accountId, new BigDecimal("-100")));
        }
    }

    @Nested
    @DisplayName("unfreezeCash tests")
    class UnfreezeCashTests {

        @Test
        @DisplayName("Should unfreeze cash successfully")
        void testUnfreezeCash_Success() {
            // Given
            Long accountId = 1001L;
            BigDecimal amount = new BigDecimal("5000");
            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(entity));
            when(assetRepository.save(any(AssetEntity.class))).thenReturn(entity);

            // When
            assetManager.unfreezeCash(accountId, amount);

            // Then
            ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
            verify(assetRepository).save(captor.capture());

            AssetEntity savedEntity = captor.getValue();
            assertEquals(new BigDecimal("85000"), savedEntity.getAvailableCash());
            assertEquals(new BigDecimal("5000"), savedEntity.getFrozenCash());
        }

        @Test
        @DisplayName("Should throw exception when amount exceeds frozen cash")
        void testUnfreezeCash_InsufficientFrozenCash() {
            // Given
            Long accountId = 1001L;
            BigDecimal amount = new BigDecimal("20000"); // More than frozen
            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(entity));

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.unfreezeCash(accountId, amount));
            verify(assetRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateAssetFromTrade tests")
    class UpdateAssetFromTradeTests {

        @Test
        @DisplayName("Should update asset for BUY trade")
        void testUpdateAssetFromTrade_Buy() {
            // Given
            Long accountId = 1001L;
            TradeDTO trade = new TradeDTO();
            trade.setTradeId("TRADE001");
            trade.setAccountId(accountId);
            trade.setSymbol("BTC-USDT");
            trade.setSide("BUY");
            trade.setPrice(new BigDecimal("50000"));
            trade.setQuantity(new BigDecimal("1"));
            trade.setAmount(new BigDecimal("50000"));
            trade.setTradeTime(LocalDateTime.now());

            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(entity));
            when(assetRepository.save(any(AssetEntity.class))).thenReturn(entity);

            // When
            assetManager.updateAssetFromTrade(trade);

            // Then
            ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
            verify(assetRepository).save(captor.capture());

            AssetEntity savedEntity = captor.getValue();
            assertEquals(new BigDecimal("30000"), savedEntity.getAvailableCash());
            assertEquals(new BigDecimal("60000"), savedEntity.getMarketValue());
        }

        @Test
        @DisplayName("Should update asset for SELL trade")
        void testUpdateAssetFromTrade_Sell() {
            // Given
            Long accountId = 1001L;
            TradeDTO trade = new TradeDTO();
            trade.setTradeId("TRADE002");
            trade.setAccountId(accountId);
            trade.setSymbol("BTC-USDT");
            trade.setSide("SELL");
            trade.setPrice(new BigDecimal("50000"));
            trade.setQuantity(new BigDecimal("1"));
            trade.setAmount(new BigDecimal("50000"));
            trade.setTradeTime(LocalDateTime.now());

            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(entity));
            when(assetRepository.save(any(AssetEntity.class))).thenReturn(entity);

            // When
            assetManager.updateAssetFromTrade(trade);

            // Then
            ArgumentCaptor<AssetEntity> captor = ArgumentCaptor.forClass(AssetEntity.class);
            verify(assetRepository).save(captor.capture());

            AssetEntity savedEntity = captor.getValue();
            assertEquals(new BigDecimal("130000"), savedEntity.getAvailableCash());
            assertEquals(new BigDecimal("-40000"), savedEntity.getMarketValue());
        }

        @Test
        @DisplayName("Should throw exception for null trade")
        void testUpdateAssetFromTrade_NullTrade() {
            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.updateAssetFromTrade(null));
            verify(assetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when account not found")
        void testUpdateAssetFromTrade_AccountNotFound() {
            // Given
            TradeDTO trade = new TradeDTO();
            trade.setAccountId(9999L);
            when(assetRepository.findByAccountIdForUpdate(9999L)).thenReturn(Optional.empty());

            // When / Then
            assertThrows(IllegalArgumentException.class, () -> assetManager.updateAssetFromTrade(trade));
            verify(assetRepository, never()).save(any());
        }
    }
}
