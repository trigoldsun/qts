package com.qts.biz.settle.repository;

import com.qts.biz.settle.entity.ReconcileRecordEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReconcileRecordRepository extends JpaRepository<ReconcileRecordEntity, String> {

    Page<ReconcileRecordEntity> findByAccountId(String accountId, Pageable pageable);

    List<ReconcileRecordEntity> findByReconcileTimeBetween(LocalDateTime start, LocalDateTime end);

    Page<ReconcileRecordEntity> findByAccountIdAndReconcileTimeBetween(
            String accountId, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
