package com.qts.biz.settle.service;

import com.qts.biz.settle.dto.ReconcileResult;
import com.qts.biz.settle.entity.ReconcileRecordEntity;
import com.qts.biz.settle.entity.ReconcileRecordEntity.ReconcileStatus;
import com.qts.biz.settle.repository.ReconcileRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 实时对账服务
 * 
 * 对账频率：每分钟
 * 对账范围：
 * - 核心账户资金
 * - 核心持仓
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconcileService {

    private final ReconcileRecordRepository reconcileRecordRepository;
    private final ObjectMapper objectMapper;

    // 模拟券商网关（实际应调用券商API）
    // private final BrokerGateway brokerGateway;

    /**
     * 执行实时对账（每分钟由调度器触发）
     */
    @Scheduled(cron = "0 * * * * *")  // 每分钟执行
    public void scheduledReconcile() {
        log.info("Starting scheduled reconciliation...");
        
        // 获取所有活跃账户（实际应从账户服务获取）
        // List<String> activeAccounts = accountService.getActiveAccountIds();
        List<String> activeAccounts = Collections.singletonList("ACC-001"); // Mock
        
        for (String accountId : activeAccounts) {
            try {
                reconcile(accountId);
            } catch (Exception e) {
                log.error("Failed to reconcile account {}: {}", accountId, e.getMessage());
            }
        }
        
        log.info("Scheduled reconciliation completed");
    }

    /**
     * 对账单个账户
     */
    public ReconcileResult reconcile(String accountId) {
        log.info("Reconciling account: {}", accountId);

        // 1. 获取系统内账户数据
        BigDecimal systemAvailableCash = getSystemAvailableCash(accountId);
        BigDecimal systemMarketValue = getSystemMarketValue(accountId);

        // 2. 获取券商柜台数据（实际应调用券商API）
        BigDecimal brokerAvailableCash = getBrokerAvailableCash(accountId);
        BigDecimal brokerMarketValue = getBrokerMarketValue(accountId);

        // 3. 计算差异
        BigDecimal cashDiff = systemAvailableCash.subtract(brokerAvailableCash).abs();
        BigDecimal marketValueDiff = systemMarketValue.subtract(brokerMarketValue).abs();
        boolean cashMatch = cashDiff.compareTo(new BigDecimal("0.01")) < 0;  // 误差小于0.01
        boolean marketValueMatch = marketValueDiff.compareTo(new BigDecimal("0.01")) < 0;

        // 4. 检测持仓差异
        List<Map<String, Object>> positionDiffs = detectPositionDifferences(accountId);

        ReconcileStatus assetStatus = cashMatch ? ReconcileStatus.MATCH : ReconcileStatus.DISCARD;
        ReconcileStatus positionStatus = positionDiffs.isEmpty() ? ReconcileStatus.MATCH : ReconcileStatus.DISCARD;

        // 5. 保存对账记录
        int accountIdEnd = Math.min(8, accountId.length());
        String reconcileId = "Recon-" + LocalDateTime.now().toString() + "-" + accountId.substring(0, accountIdEnd);
        String diffDetails = null;
        if (!positionDiffs.isEmpty()) {
            try {
                diffDetails = objectMapper.writeValueAsString(positionDiffs);
            } catch (JsonProcessingException e) {
                diffDetails = positionDiffs.toString();
            }
        }

        ReconcileRecordEntity record = ReconcileRecordEntity.builder()
                .reconcileId(reconcileId)
                .accountId(accountId)
                .reconcileTime(LocalDateTime.now())
                .assetStatus(assetStatus)
                .positionStatus(positionStatus)
                .systemAvailableCash(systemAvailableCash)
                .brokerAvailableCash(brokerAvailableCash)
                .cashDifference(cashDiff)
                .systemMarketValue(systemMarketValue)
                .brokerMarketValue(brokerMarketValue)
                .marketValueDifference(marketValueDiff)
                .differenceCount(positionDiffs.size())
                .differenceDetails(diffDetails)
                .build();

        reconcileRecordRepository.save(record);

        // 6. 差异告警
        if (assetStatus == ReconcileStatus.DISCARD || positionStatus == ReconcileStatus.DISCARD) {
            sendReconcileAlert(accountId, assetStatus, positionStatus, cashDiff, marketValueDiff);
        }

        log.info("Reconciliation completed for account: {}, assetStatus: {}, positionStatus: {}", 
                accountId, assetStatus, positionStatus);

        return ReconcileResult.builder()
                .reconcileId(reconcileId)
                .accountId(accountId)
                .reconcileTime(record.getReconcileTime())
                .assetStatus(assetStatus.name())
                .positionStatus(positionStatus.name())
                .differenceCount(positionDiffs.size())
                .cashDifference(cashDiff)
                .marketValueDifference(marketValueDiff)
                .build();
    }

    /**
     * 检测持仓差异
     */
    private List<Map<String, Object>> detectPositionDifferences(String accountId) {
        List<Map<String, Object>> differences = new ArrayList<>();
        
        // 获取系统持仓（实际应从BIZ-TRADE获取）
        // List<Position> systemPositions = positionService.getPositions(accountId);
        
        // 获取券商持仓
        // List<Position> brokerPositions = brokerGateway.getPositions(accountId);
        
        // 比较差异...
        // 简化处理
        
        return differences;
    }

    /**
     * 发送对账差异告警
     */
    private void sendReconcileAlert(String accountId, ReconcileStatus assetStatus, 
            ReconcileStatus positionStatus, BigDecimal cashDiff, BigDecimal marketValueDiff) {
        
        String alertLevel = "P2"; // 默认P2告警
        if (cashDiff.compareTo(new BigDecimal("10000")) > 0 || 
            marketValueDiff.compareTo(new BigDecimal("10000")) > 0) {
            alertLevel = "P0"; // 重大差异
        } else if (cashDiff.compareTo(new BigDecimal("1000")) > 0 ||
                   marketValueDiff.compareTo(new BigDecimal("1000")) > 0) {
            alertLevel = "P1";
        }
        
        log.warn("RECONCILE_ALERT [{}] account={}, assetStatus={}, positionStatus={}, cashDiff={}, marketValueDiff={}",
                alertLevel, accountId, assetStatus, positionStatus, cashDiff, marketValueDiff);
        
        // 实际应发送告警到监控系统
        // alertService.sendAlert(alertLevel, "对账差异", "account=" + accountId + ",差异=" + ...);
    }

    /**
     * 查询对账结果
     */
    public Page<ReconcileRecordEntity> queryReconcileRecords(String accountId, 
            LocalDateTime reconcileTime, Pageable pageable) {
        
        if (accountId != null && reconcileTime != null) {
            return reconcileRecordRepository.findByAccountIdAndReconcileTimeBetween(
                    accountId, reconcileTime, reconcileTime.plusHours(1), pageable);
        } else if (accountId != null) {
            return reconcileRecordRepository.findByAccountId(accountId, pageable);
        } else if (reconcileTime != null) {
            return reconcileRecordRepository.findByAccountIdAndReconcileTimeBetween(
                    null, reconcileTime, reconcileTime.plusHours(1), pageable);
        } else {
            return reconcileRecordRepository.findAll(pageable);
        }
    }

    // ========== Mock methods - 实际应调用外部服务 ==========

    private BigDecimal getSystemAvailableCash(String accountId) {
        return BigDecimal.valueOf(100000); // Mock
    }

    private BigDecimal getSystemMarketValue(String accountId) {
        return BigDecimal.valueOf(500000); // Mock
    }

    private BigDecimal getBrokerAvailableCash(String accountId) {
        return BigDecimal.valueOf(100000); // Mock - 假设匹配
    }

    private BigDecimal getBrokerMarketValue(String accountId) {
        return BigDecimal.valueOf(500000); // Mock - 假设匹配
    }
}
