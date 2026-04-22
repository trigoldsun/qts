package com.qts.biz.trade.repository;

import com.qts.biz.trade.entity.AssetEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AssetRepository
 * Tests repository methods with mocked JPA repository
 */
@ExtendWith(MockitoExtension.class)
public class AssetRepositoryTest {

    @Mock
    private AssetRepository assetRepository;

    private AssetEntity createTestAsset(Long accountId) {
        AssetEntity entity = new AssetEntity();
        entity.setAccountId(accountId);
        entity.setCurrency("CNY");
        entity.setTotalAssets(new BigDecimal("100000"));
        entity.setAvailableCash(new BigDecimal("80000"));
        entity.setFrozenCash(new BigDecimal("10000"));
        entity.setMarketValue(new BigDecimal("10000"));
        return entity;
    }

    @Nested
    @DisplayName("findByAccountId tests")
    class FindByAccountIdTests {

        @Test
        @DisplayName("Should return asset when found")
        void testFindByAccountId_Found() {
            // Given
            Long accountId = 1001L;
            AssetEntity entity = createTestAsset(accountId);
            when(assetRepository.findByAccountId(accountId)).thenReturn(Optional.of(entity));

            // When
            Optional<AssetEntity> result = assetRepository.findByAccountId(accountId);

            // Then
            assertTrue(result.isPresent());
            assertEquals(accountId, result.get().getAccountId());
            verify(assetRepository).findByAccountId(accountId);
        }

        @Test
        @DisplayName("Should return empty when not found")
        void testFindByAccountId_NotFound() {
            // Given
            Long accountId = 9999L;
            when(assetRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

            // When
            Optional<AssetEntity> result = assetRepository.findByAccountId(accountId);

            // Then
            assertFalse(result.isPresent());
            verify(assetRepository).findByAccountId(accountId);
        }
    }

    @Nested
    @DisplayName("existsByAccountId tests")
    class ExistsByAccountIdTests {

        @Test
        @DisplayName("Should return true when asset exists")
        void testExistsByAccountId_True() {
            // Given
            Long accountId = 1001L;
            when(assetRepository.existsByAccountId(accountId)).thenReturn(true);

            // When
            boolean result = assetRepository.existsByAccountId(accountId);

            // Then
            assertTrue(result);
            verify(assetRepository).existsByAccountId(accountId);
        }

        @Test
        @DisplayName("Should return false when asset does not exist")
        void testExistsByAccountId_False() {
            // Given
            Long accountId = 9999L;
            when(assetRepository.existsByAccountId(accountId)).thenReturn(false);

            // When
            boolean result = assetRepository.existsByAccountId(accountId);

            // Then
            assertFalse(result);
            verify(assetRepository).existsByAccountId(accountId);
        }
    }

    @Nested
    @DisplayName("save tests")
    class SaveTests {

        @Test
        @DisplayName("Should save asset successfully")
        void testSave_Success() {
            // Given
            AssetEntity entity = createTestAsset(1001L);
            when(assetRepository.save(entity)).thenReturn(entity);

            // When
            AssetEntity result = assetRepository.save(entity);

            // Then
            assertNotNull(result);
            assertEquals(1001L, result.getAccountId());
            verify(assetRepository).save(entity);
        }
    }
}
