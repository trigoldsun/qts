package com.qts.biz.settle.service;

import com.qts.biz.settle.dto.DailySettlementRequest;
import com.qts.biz.settle.dto.DailySettlementResponse;
import com.qts.biz.settle.entity.SettlementTaskEntity;
import com.qts.biz.settle.entity.SettlementTaskEntity.SettlementStatus;
import com.qts.biz.settle.entity.SettlementLogEntity;
import com.qts.biz.settle.entity.SettlementLogEntity.SettlementType;
import com.qts.biz.settle.repository.SettlementTaskRepository;
import com.qts.biz.settle.repository.SettlementLogRepository;
import com.qts.biz.settle.event.SettlementCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 日终清算服务
 * 
 * 清算时间：T+1 00:30执行
 * 清算内容：
 * - 账户余额对账
 * - 持仓对账
 * - 佣金/费用计算
 * - 盈利/亏损结算
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailySettlementService {

    private static final String TOPIC_SETTLEMENT_COMPLETED = "qts.settle.tasks";

    private final SettlementTaskRepository settlementTaskRepository;
    private final SettlementLogRepository settlementLogRepository;
    private final KafkaTemplate<String, SettlementCompletedEvent> kafkaTemplate;

    // 模拟券商柜台接口（实际应调用券商API）
    // private final BrokerGateway brokerGateway;

    /**
     * 触发日终清算
     */
    @Transactional
    public DailySettlementResponse triggerDailySettlement(DailySettlementRequest request) {
        LocalDate settleDate = request.getSettleDate();
        log.info("Triggering daily settlement for date: {}, accounts: {}", 
                settleDate, request.getAccountIds() == null ? "ALL" : request.getAccountIds().size());

        // 1. 检查是否已有正在执行的清算任务
        if (settlementTaskRepository.existsBySettleDateAndStatus(settleDate, SettlementStatus.PROCESSING)) {
            log.warn("Settlement task already running for date: {}", settleDate);
            SettlementTaskEntity existing = settlementTaskRepository.findBySettleDate(settleDate).orElseThrow();
            return DailySettlementResponse.builder()
                    .settleId(existing.getSettleId())
                    .settleDate(existing.getSettleDate())
                    .status(existing.getStatus().name())
                    .startedAt(existing.getStartedAt())
                    .accountsCount(existing.getAccountsCount())
                    .build();
        }

        // 2. 创建清算任务
        String settleId = "Settle-" + settleDate + "-" + UUID.randomUUID().toString().substring(0, 8);
        List<String> accountIds = request.getAccountIds();
        int accountsCount = accountIds != null ? accountIds.size() : 0; // 实际应查询所有账户

        SettlementTaskEntity task = SettlementTaskEntity.builder()
                .settleId(settleId)
                .settleDate(settleDate)
                .status(SettlementStatus.PROCESSING)
                .startedAt(LocalDateTime.now())
                .accountsCount(accountsCount)
                .processedCount(0)
                .failedCount(0)
                .createdBy("system")
                .build();

        settlementTaskRepository.save(task);
        log.info("Created settlement task: {}", settleId);

        // 3. 执行清算（异步或同步，取决于规模）
        executeSettlement(task, accountIds);

        return DailySettlementResponse.builder()
                .settleId(task.getSettleId())
                .settleDate(task.getSettleDate())
                .status(task.getStatus().name())
                .startedAt(task.getStartedAt())
                .accountsCount(task.getAccountsCount())
                .build();
    }

    /**
     * 执行清算逻辑
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeSettlement(SettlementTaskEntity task, List<String> accountIds) {
        log.info("Executing settlement: {}", task.getSettleId());

        // 获取所有需要清算的账户（实际应从BIZ-TRADE获取）
        // List<String> accountsToSettle = accountIds != null ? accountIds : accountService.getAllAccountIds();
        List<String> accountsToSettle = accountIds; // 简化处理

        int processed = 0;
        int failed = 0;

        try {
            if (accountsToSettle != null) {
                for (String accountId : accountsToSettle) {
                    try {
                        settleAccount(task.getSettleId(), accountId, task.getSettleDate());
                        processed++;
                    } catch (Exception e) {
                        log.error("Failed to settle account {}: {}", accountId, e.getMessage());
                        failed++;
                    }
                }
            }

            // 更新任务状态
            task.setProcessedCount(processed);
            task.setFailedCount(failed);
            task.setStatus(failed > 0 && processed == 0 ? SettlementStatus.FAILED : SettlementStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            settlementTaskRepository.save(task);

            log.info("Settlement completed: {}, processed: {}, failed: {}",
                    task.getSettleId(), processed, failed);
        } catch (Exception e) {
            task.setStatus(SettlementStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            settlementTaskRepository.save(task);
            throw e;
        }
    }

    /**
     * 清算单个账户
     */
    @Transactional
    public void settleAccount(String settleId, String accountId, LocalDate settleDate) {
        log.info("Settling account: {}, date: {}", accountId, settleDate);

        // 1. 获取昨日资金余额（实际应从数据库/Redis获取）
        BigDecimal yesterdayAvailableCash = getYesterdayAvailableCash(accountId);
        BigDecimal yesterdayFrozenCash = getYesterdayFrozenCash(accountId);

        // 2. 汇总当日资金变动
        BigDecimal todayDeposit = getTodayDeposit(accountId, settleDate);
        BigDecimal todayWithdraw = getTodayWithdraw(accountId, settleDate);
        BigDecimal todayBuyAmount = getTodayBuyAmount(accountId, settleDate);
        BigDecimal todaySellAmount = getTodaySellAmount(accountId, settleDate);
        BigDecimal todayCommission = getTodayCommission(accountId, settleDate);
        BigDecimal todayStampDuty = getTodayStampDuty(accountId, settleDate);
        BigDecimal todayExchangeFee = getTodayExchangeFee(accountId, settleDate);

        // 3. 计算当日资金余额
        // 可用 = 昨日可用 + 当日入金 - 当日出金 - 当日买入 + 当日卖出 - 当日佣金 - 当日印花税 - 当日过户费
        BigDecimal todayAvailableCash = yesterdayAvailableCash
                .add(todayDeposit)
                .subtract(todayWithdraw)
                .subtract(todayBuyAmount)
                .add(todaySellAmount)
                .subtract(todayCommission)
                .subtract(todayStampDuty)
                .subtract(todayExchangeFee);

        BigDecimal todayFrozenCash = yesterdayFrozenCash;

        // 4. 记录清算日志
        String logId = "Log-" + UUID.randomUUID().toString().substring(0, 8);
        
        // 入金
        if (todayDeposit.compareTo(BigDecimal.ZERO) != 0) {
            settlementLogRepository.save(SettlementLogEntity.builder()
                    .logId(logId + "-DEP")
                    .settleId(settleId)
                    .accountId(accountId)
                    .settleDate(settleDate)
                    .settleType(SettlementType.ACCOUNT_BALANCE)
                    .changeType("DEPOSIT")
                    .amountBefore(yesterdayAvailableCash)
                    .amountAfter(yesterdayAvailableCash.add(todayDeposit))
                    .description("当日入金")
                    .build());
        }

        // 出金
        if (todayWithdraw.compareTo(BigDecimal.ZERO) != 0) {
            settlementLogRepository.save(SettlementLogEntity.builder()
                    .logId(logId + "-WDL")
                    .settleId(settleId)
                    .accountId(accountId)
                    .settleDate(settleDate)
                    .settleType(SettlementType.ACCOUNT_BALANCE)
                    .changeType("WITHDRAW")
                    .amountBefore(yesterdayAvailableCash.add(todayDeposit))
                    .amountAfter(yesterdayAvailableCash.add(todayDeposit).subtract(todayWithdraw))
                    .description("当日出金")
                    .build());
        }

        // 5. 持仓清算
        settlePositions(settleId, accountId, settleDate);

        // 6. 发布清算完成事件
        publishSettlementCompleted(settleId, accountId, settleDate, todayAvailableCash, todayFrozenCash,
                todayCommission, todayStampDuty, todayExchangeFee);

        log.info("Account settled: {}, available: {}, frozen: {}", 
                accountId, todayAvailableCash, todayFrozenCash);
    }

    /**
     * 持仓清算
     */
    private void settlePositions(String settleId, String accountId, LocalDate settleDate) {
        // 获取账户持仓（实际应从BIZ-TRADE获取）
        // List<Position> positions = positionService.getPositionsByAccount(accountId);
        
        // 简化：模拟获取持仓数据
        // for (Position pos : positions) {
        //     BigDecimal profitLoss = calculateProfitLoss(pos);
        //     settlementLogRepository.save(...);
        // }
    }

    /**
     * 发布清算完成事件
     */
    private void publishSettlementCompleted(String settleId, String accountId, LocalDate settleDate,
            BigDecimal availableCash, BigDecimal frozenCash,
            BigDecimal commission, BigDecimal stampDuty, BigDecimal exchangeFee) {
        
        BigDecimal marketValue = BigDecimal.ZERO; // 实际应计算持仓市值
        BigDecimal profitLoss = BigDecimal.ZERO;  // 实际应计算盈亏
        BigDecimal netProfitLoss = profitLoss.subtract(commission).subtract(stampDuty).subtract(exchangeFee);

        SettlementCompletedEvent event = SettlementCompletedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(SettlementCompletedEvent.EVENT_TYPE)
                .occurredAt(LocalDateTime.now())
                .version("1.0")
                .payload(SettlementCompletedEvent.Payload.builder()
                        .settleId(settleId)
                        .accountId(accountId)
                        .settleDate(settleDate)
                        .totalAssets(availableCash.add(frozenCash).add(marketValue))
                        .availableCash(availableCash)
                        .frozenCash(frozenCash)
                        .marketValue(marketValue)
                        .profitLoss(profitLoss)
                        .commission(commission)
                        .stampDuty(stampDuty)
                        .exchangeFee(exchangeFee)
                        .netProfitLoss(netProfitLoss)
                        .completedAt(LocalDateTime.now())
                        .build())
                .build();

        kafkaTemplate.send(TOPIC_SETTLEMENT_COMPLETED, accountId, event);
        log.debug("Published settlement completed event for account: {}", accountId);
    }

    // ========== Mock methods - 实际应调用外部服务 ==========

    private BigDecimal getYesterdayAvailableCash(String accountId) {
        return BigDecimal.valueOf(100000); // Mock
    }

    private BigDecimal getYesterdayFrozenCash(String accountId) {
        return BigDecimal.ZERO; // Mock
    }

    private BigDecimal getTodayDeposit(String accountId, LocalDate date) {
        return BigDecimal.ZERO; // Mock
    }

    private BigDecimal getTodayWithdraw(String accountId, LocalDate date) {
        return BigDecimal.ZERO; // Mock
    }

    private BigDecimal getTodayBuyAmount(String accountId, LocalDate date) {
        return BigDecimal.valueOf(50000); // Mock
    }

    private BigDecimal getTodaySellAmount(String accountId, LocalDate date) {
        return BigDecimal.valueOf(30000); // Mock
    }

    private BigDecimal getTodayCommission(String accountId, LocalDate date) {
        return BigDecimal.valueOf(80); // Mock
    }

    private BigDecimal getTodayStampDuty(String accountId, LocalDate date) {
        return BigDecimal.valueOf(30); // Mock (卖出收取)
    }

    private BigDecimal getTodayExchangeFee(String accountId, LocalDate date) {
        return BigDecimal.valueOf(10); // Mock
    }

    /**
     * 查询清算状态
     */
    public SettlementTaskEntity getSettlementStatus(String settleId) {
        return settlementTaskRepository.findById(settleId).orElse(null);
    }

    /**
     * 查询清算结果
     */
    public List<SettlementLogEntity> getSettlementLogs(String settleId) {
        return settlementLogRepository.findBySettleId(settleId);
    }
}
