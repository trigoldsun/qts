package com.qts.biz.trade.repository;

import com.qts.biz.trade.entity.OrderStateLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Order State Log Repository
 */
@Repository
public interface OrderStateLogRepository extends JpaRepository<OrderStateLog, Long> {

    /**
     * Find state logs by orderId ordered by created time
     */
    List<OrderStateLog> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
