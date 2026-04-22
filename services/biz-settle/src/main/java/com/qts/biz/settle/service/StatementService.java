package com.qts.biz.settle.service;

import com.qts.biz.settle.dto.StatementDTO;
import com.qts.biz.settle.entity.StatementEntity;
import com.qts.biz.settle.entity.StatementEntity.StatementType;
import com.qts.biz.settle.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

/**
 * 结算单服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatementService {

    private final StatementRepository statementRepository;

    /**
     * 查询结算单列表
     */
    public Page<StatementEntity> queryStatements(String accountId, LocalDate settleDate, 
            StatementType statementType, Pageable pageable) {
        
        if (accountId != null && settleDate != null && statementType != null) {
            Optional<StatementEntity> stmt = statementRepository
                    .findByAccountIdAndSettleDateAndStatementType(accountId, settleDate, statementType);
            if (stmt.isPresent()) {
                return new PageImpl<>(Collections.singletonList(stmt.get()), pageable, 1);
            } else {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        } else if (accountId != null && settleDate != null) {
            return statementRepository.findByAccountIdAndSettleDateBetween(
                    accountId, settleDate, settleDate.plusDays(1), pageable);
        } else if (accountId != null) {
            return statementRepository.findByAccountId(accountId, pageable);
        } else {
            return statementRepository.findAll(pageable);
        }
    }

    /**
     * 生成结算单（通常在日终清算后自动生成）
     */
    public StatementEntity generateDailyStatement(String accountId, LocalDate settleDate) {
        log.info("Generating daily statement for account: {}, date: {}", accountId, settleDate);

        // 检查是否已存在
        Optional<StatementEntity> existing = statementRepository
                .findByAccountIdAndSettleDateAndStatementType(accountId, settleDate, StatementType.DAILY);
        if (existing.isPresent()) {
            log.warn("Daily statement already exists for account: {}, date: {}", accountId, settleDate);
            return existing.get();
        }

        // 获取当日清算数据（实际应从清算日志汇总）
        // 这里简化处理
        StatementEntity statement = StatementEntity.builder()
                .statementId("Stmt-" + accountId + "-" + settleDate)
                .accountId(accountId)
                .settleDate(settleDate)
                .statementType(StatementType.DAILY)
                .totalAssetsStart(java.math.BigDecimal.valueOf(100000))
                .totalAssetsEnd(java.math.BigDecimal.valueOf(105000))
                .availableCash(java.math.BigDecimal.valueOf(50000))
                .frozenCash(java.math.BigDecimal.ZERO)
                .marketValue(java.math.BigDecimal.valueOf(55000))
                .totalProfitLoss(java.math.BigDecimal.valueOf(5000))
                .todayProfitLoss(java.math.BigDecimal.valueOf(500))
                .totalCommission(java.math.BigDecimal.valueOf(80))
                .totalStampDuty(java.math.BigDecimal.valueOf(30))
                .totalExchangeFee(java.math.BigDecimal.valueOf(10))
                .netProfitLoss(java.math.BigDecimal.valueOf(380))
                .build();

        return statementRepository.save(statement);
    }

    /**
     * 转换为DTO
     */
    public StatementDTO toDTO(StatementEntity entity) {
        return StatementDTO.builder()
                .statementId(entity.getStatementId())
                .accountId(entity.getAccountId())
                .settleDate(entity.getSettleDate())
                .statementType(entity.getStatementType().name())
                .totalAssetsStart(entity.getTotalAssetsStart())
                .totalAssetsEnd(entity.getTotalAssetsEnd())
                .availableCash(entity.getAvailableCash())
                .frozenCash(entity.getFrozenCash())
                .marketValue(entity.getMarketValue())
                .totalProfitLoss(entity.getTotalProfitLoss())
                .todayProfitLoss(entity.getTodayProfitLoss())
                .totalCommission(entity.getTotalCommission())
                .totalStampDuty(entity.getTotalStampDuty())
                .totalExchangeFee(entity.getTotalExchangeFee())
                .netProfitLoss(entity.getNetProfitLoss())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
