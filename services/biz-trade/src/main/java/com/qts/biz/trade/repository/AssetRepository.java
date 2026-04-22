package com.qts.biz.trade.repository;

import com.qts.biz.trade.entity.AssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

/**
 * Asset Repository
 * Provides data access for account assets in PostgreSQL
 */
@Repository
public interface AssetRepository extends JpaRepository<AssetEntity, Long> {

    /**
     * Find asset by account ID
     * @param accountId Account ID
     * @return Optional asset entity
     */
    Optional<AssetEntity> findByAccountId(Long accountId);

    /**
     * Find asset by account ID with pessimistic lock for update
     * @param accountId Account ID
     * @return Optional asset entity
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AssetEntity a WHERE a.accountId = :accountId")
    Optional<AssetEntity> findByAccountIdForUpdate(@Param("accountId") Long accountId);

    /**
     * Check if asset exists for account
     * @param accountId Account ID
     * @return true if exists
     */
    boolean existsByAccountId(Long accountId);
}
