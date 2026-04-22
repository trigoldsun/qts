package com.qts.biz.settle.scheduler;

import com.qts.biz.settle.dto.DailySettlementRequest;
import com.qts.biz.settle.dto.DailySettlementResponse;
import com.qts.biz.settle.service.DailySettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 日终清算调度器
 * 
 * 清算时间：T+1 00:30（券商结算完成后）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailySettlementScheduler {

    private final DailySettlementService dailySettlementService;

    /**
     * T+1 00:30 执行日终清算
     * 
     * cron表达式: second minute hour day month weekday
     * 00:30 = 0 30 0
     * 实际生产应为 T+1 00:30，这里简化处理
     */
    @Scheduled(cron = "0 30 0 * * *")  // 每天凌晨0:30执行
    public void triggerDailySettlement() {
        // 计算T+1日期（即前一交易日）
        LocalDate tradingDate = LocalDate.now().minusDays(1);
        
        log.info("Triggering scheduled daily settlement for date: {}", tradingDate);
        
        try {
            DailySettlementRequest request = DailySettlementRequest.builder()
                    .settleDate(tradingDate)
                    .build();
            
DailySettlementResponse response = dailySettlementService.triggerDailySettlement(request);
            log.info("Scheduled settlement triggered: settleId={}, status={}", 
                    response.getSettleId(), response.getStatus());
        } catch (Exception e) {
            log.error("Failed to trigger scheduled settlement: {}", e.getMessage(), e);
        }
    }
}
