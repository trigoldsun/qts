package com.qts.biz.settle.repository;

import com.qts.biz.settle.entity.StatementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface StatementRepository extends JpaRepository<StatementEntity, String> {

    Page<StatementEntity> findByAccountId(String accountId, Pageable pageable);

    Optional<StatementEntity> findByAccountIdAndSettleDateAndStatementType(
            String accountId, LocalDate settleDate, StatementEntity.StatementType statementType);

    Page<StatementEntity> findByAccountIdAndSettleDateBetween(
            String accountId, LocalDate startDate, LocalDate endDate, Pageable pageable);
}
