package com.qts.biz.settle.repository;

import com.qts.biz.settle.entity.SettlementLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SettlementLogRepository extends JpaRepository<SettlementLogEntity, String> {

    List<SettlementLogEntity> findBySettleId(String settleId);

    List<SettlementLogEntity> findByAccountIdAndSettleDate(String accountId, LocalDate settleDate);

    List<SettlementLogEntity> findBySettleIdAndAccountId(String settleId, String accountId);
}
