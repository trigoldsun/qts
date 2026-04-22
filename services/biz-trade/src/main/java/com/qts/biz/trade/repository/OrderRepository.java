package com.qts.biz.trade.repository;

import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Order Repository using Spring Data JPA
 */
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String>, JpaSpecificationExecutor<OrderEntity> {

    /**
     * Find order by orderId
     */
    Optional<OrderEntity> findByOrderId(String orderId);

    /**
     * Find order by clientOrderId
     */
    Optional<OrderEntity> findByClientOrderId(String clientOrderId);

    /**
     * Find orders by accountId
     */
    List<OrderEntity> findByAccountId(String accountId);

    /**
     * Find orders by accountId with pagination
     */
    Page<OrderEntity> findByAccountId(String accountId, Pageable pageable);

    /**
     * Find orders by accountId and status
     */
    List<OrderEntity> findByAccountIdAndStatus(String accountId, OrderStatus status);

    /**
     * Find orders with dynamic conditions
     */
    @Query("SELECT o FROM OrderEntity o WHERE " +
           "(:accountId IS NULL OR o.accountId = :accountId) AND " +
           "(:symbol IS NULL OR o.symbol = :symbol) AND " +
           "(:status IS NULL OR o.status = :status) AND " +
           "(:startTime IS NULL OR o.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR o.createdAt <= :endTime)")
    Page<OrderEntity> findByConditions(
            @Param("accountId") String accountId,
            @Param("symbol") String symbol,
            @Param("status") OrderStatus status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    /**
     * Check if clientOrderId exists for account
     */
    boolean existsByClientOrderIdAndAccountId(String clientOrderId, String accountId);

    /**
     * Count orders by accountId and status
     */
    long countByAccountIdAndStatus(String accountId, OrderStatus status);
}
