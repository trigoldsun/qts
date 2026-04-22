package com.qts.biz.settle.repository;

import com.qts.biz.settle.entity.SettlementTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SettlementTaskRepository extends JpaRepository<SettlementTaskEntity, String> {

    Optional<SettlementTaskEntity> findBySettleDate(LocalDate settleDate);

    List<SettlementTaskEntity> findByStatus(SettlementTaskEntity.SettlementStatus status);

    List<SettlementTaskEntity> findBySettleDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsBySettleDateAndStatus(LocalDate settleDate, SettlementTaskEntity.SettlementStatus status);
}
