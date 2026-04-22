package com.qts.biz.trade.repository;

import com.qts.biz.trade.dto.PositionQuery;
import com.qts.biz.trade.entity.PositionEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for InMemoryPositionRepository
 * Tests repository behavior with actual data storage
 */
class InMemoryPositionRepositoryTest {

    private InMemoryPositionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryPositionRepository();
    }

    @Nested
    @DisplayName("save tests")
    class SaveTests {

        @Test
        @DisplayName("Should save new position successfully")
        void testSave_NewPosition() {
            PositionEntity entity = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            
            PositionEntity saved = repository.save(entity);
            
            assertNotNull(saved.getId());
            assertNotNull(saved.getCreateTime());
            assertNotNull(saved.getUpdateTime());
        }

        @Test
        @DisplayName("Should update existing position")
        void testSave_UpdatePosition() {
            PositionEntity entity = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            
            PositionEntity saved1 = repository.save(entity);
            Long originalId = saved1.getId();
            
            saved1.setQuantity(new BigDecimal("2.0"));
            PositionEntity saved2 = repository.save(saved1);
            
            assertEquals(originalId, saved2.getId());
            assertEquals(new BigDecimal("2.0"), saved2.getQuantity());
        }

        @Test
        @DisplayName("Should throw exception for null position")
        void testSave_NullPosition() {
            assertThrows(IllegalArgumentException.class, () -> repository.save(null));
        }

        @Test
        @DisplayName("Should throw exception when accountId is null")
        void testSave_NullAccountId() {
            PositionEntity entity = createPositionEntity(null, "BTC-USDT", 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            
            assertThrows(IllegalArgumentException.class, () -> repository.save(entity));
        }

        @Test
        @DisplayName("Should throw exception when symbol is null")
        void testSave_NullSymbol() {
            PositionEntity entity = createPositionEntity(1001L, null, 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            
            assertThrows(IllegalArgumentException.class, () -> repository.save(entity));
        }
    }

    @Nested
    @DisplayName("findByAccountIdAndSymbol tests")
    class FindByAccountIdAndSymbolTests {

        @Test
        @DisplayName("Should find existing position")
        void testFindByAccountIdAndSymbol_Found() {
            PositionEntity entity = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            repository.save(entity);
            
            Optional<PositionEntity> result = repository.findByAccountIdAndSymbol(1001L, "BTC-USDT");
            
            assertTrue(result.isPresent());
            assertEquals("BTC-USDT", result.get().getSymbol());
            assertEquals(new BigDecimal("1.5"), result.get().getQuantity());
        }

        @Test
        @DisplayName("Should return empty for non-existent position")
        void testFindByAccountIdAndSymbol_NotFound() {
            Optional<PositionEntity> result = repository.findByAccountIdAndSymbol(9999L, "NONEXISTENT");
            
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("findByAccountId tests")
    class FindByAccountIdTests {

        @Test
        @DisplayName("Should find all positions for account")
        void testFindByAccountId_All() {
            repository.save(createPositionEntity(1001L, "BTC-USDT", new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000")));
            repository.save(createPositionEntity(1001L, "ETH-USDT", new BigDecimal("2.0"), BigDecimal.ZERO, new BigDecimal("3000"), new BigDecimal("3100")));
            repository.save(createPositionEntity(1002L, "BTC-USDT", new BigDecimal("0.5"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000")));
            
            List<PositionEntity> results = repository.findByAccountId(1001L);
            
            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Should return empty list when no positions")
        void testFindByAccountId_Empty() {
            List<PositionEntity> results = repository.findByAccountId(9999L);
            
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByAccountId with query tests")
    class FindByAccountIdWithQueryTests {

        @Test
        @DisplayName("Should filter by symbol query")
        void testFindByAccountId_WithSymbolQuery() {
            repository.save(createPositionEntity(1001L, "BTC-USDT", new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000")));
            repository.save(createPositionEntity(1001L, "ETH-USDT", new BigDecimal("2.0"), BigDecimal.ZERO, new BigDecimal("3000"), new BigDecimal("3100")));
            
            PositionQuery query = new PositionQuery();
            query.setSymbol("BTC");
            
            List<PositionEntity> results = repository.findByAccountId(1001L, query);
            
            assertEquals(1, results.size());
            assertEquals("BTC-USDT", results.get(0).getSymbol());
        }

        @Test
        @DisplayName("Should filter by symbolName query")
        void testFindByAccountId_WithSymbolNameQuery() {
            repository.save(createPositionEntity(1001L, "BTC-USDT", new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000")));
            repository.save(createPositionEntity(1001L, "ETH-USDT", new BigDecimal("2.0"), BigDecimal.ZERO, new BigDecimal("3000"), new BigDecimal("3100")));
            
            PositionQuery query = new PositionQuery();
            query.setSymbolName("Bitcoin");
            
            List<PositionEntity> results = repository.findByAccountId(1001L, query);
            
            assertEquals(1, results.size());
        }

        @Test
        @DisplayName("Should return all when query is null")
        void testFindByAccountId_NullQuery() {
            repository.save(createPositionEntity(1001L, "BTC-USDT", new BigDecimal("1.0"), BigDecimal.ZERO, new BigDecimal("50000"), new BigDecimal("51000")));
            repository.save(createPositionEntity(1001L, "ETH-USDT", new BigDecimal("2.0"), BigDecimal.ZERO, new BigDecimal("3000"), new BigDecimal("3100")));
            
            List<PositionEntity> results = repository.findByAccountId(1001L, null);
            
            assertEquals(2, results.size());
        }
    }

    @Nested
    @DisplayName("delete tests")
    class DeleteTests {

        @Test
        @DisplayName("Should delete existing position")
        void testDelete_Exists() {
            PositionEntity entity = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            repository.save(entity);
            
            repository.delete(1001L, "BTC-USDT");
            
            assertFalse(repository.exists(1001L, "BTC-USDT"));
        }

        @Test
        @DisplayName("Should not throw when deleting non-existent")
        void testDelete_NotExists() {
            assertDoesNotThrow(() -> repository.delete(9999L, "NONEXISTENT"));
        }
    }

    @Nested
    @DisplayName("exists tests")
    class ExistsTests {

        @Test
        @DisplayName("Should return true for existing position")
        void testExists_True() {
            PositionEntity entity = createPositionEntity(1001L, "BTC-USDT", 
                    new BigDecimal("1.5"), new BigDecimal("0.5"), new BigDecimal("50000"), new BigDecimal("51000"));
            repository.save(entity);
            
            assertTrue(repository.exists(1001L, "BTC-USDT"));
        }

        @Test
        @DisplayName("Should return false for non-existent position")
        void testExists_False() {
            assertFalse(repository.exists(9999L, "NONEXISTENT"));
        }
    }

    // Helper method

    private PositionEntity createPositionEntity(Long accountId, String symbol, 
            BigDecimal quantity, BigDecimal frozen, BigDecimal costPrice, BigDecimal marketPrice) {
        PositionEntity entity = new PositionEntity();
        entity.setAccountId(accountId);
        entity.setSymbol(symbol);
        entity.setSymbolName(symbol != null ? symbol.replace("-USDT", "") : null);
        entity.setQuantity(quantity);
        entity.setAvailableQuantity(quantity.subtract(frozen));
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
}