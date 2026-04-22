package com.qts.biz.settle.controller;

import com.qts.biz.settle.dto.*;
import com.qts.biz.settle.entity.ReconcileRecordEntity;
import com.qts.biz.settle.entity.StatementEntity;
import com.qts.biz.settle.service.DailySettlementService;
import com.qts.biz.settle.service.ReconcileService;
import com.qts.biz.settle.service.StatementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 清结算API控制器
 * 
 * API清单：
 * - F-SETTLE-001: POST /v1/settle/daily - 日终清算触发
 * - F-SETTLE-002: GET /v1/settle/reconcile - 对账结果查询
 * - F-SETTLE-003: GET /v1/settle/statements - 结算单查询
 */
@Slf4j
@RestController
@RequestMapping("/v1/settle")
@RequiredArgsConstructor
public class SettleController {

    private final DailySettlementService dailySettlementService;
    private final ReconcileService reconcileService;
    private final StatementService statementService;

    /**
     * F-SETTLE-001: 日终清算触发
     * POST /v1/settle/daily
     */
    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<DailySettlementResponse>> triggerDailySettlement(
            @Valid @RequestBody DailySettlementRequest request) {
        
        log.info("Received daily settlement request: settleDate={}", request.getSettleDate());
        
        DailySettlementResponse response = dailySettlementService.triggerDailySettlement(request);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * F-SETTLE-002: 对账结果查询
     * GET /v1/settle/reconcile
     */
    @GetMapping("/reconcile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryReconcileRecords(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime reconcileTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        log.info("Querying reconcile records: accountId={}, reconcileTime={}", accountId, reconcileTime);
        
        Pageable pageable = PageRequest.of(page - 1, pageSize);  // page从1开始
        Page<ReconcileRecordEntity> records = reconcileService.queryReconcileRecords(
                accountId, reconcileTime, pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("reconcile_results", records.getContent());
        result.put("pagination", Map.of(
                "page", page,
                "page_size", pageSize,
                "total", records.getTotalElements(),
                "total_pages", records.getTotalPages()
        ));
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * F-SETTLE-003: 结算单查询
     * GET /v1/settle/statements
     */
    @GetMapping("/statements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> queryStatements(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate settleDate,
            @RequestParam(required = false) String statementType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        log.info("Querying statements: accountId={}, settleDate={}, type={}", accountId, settleDate, statementType);
        
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        StatementEntity.StatementType type = statementType != null ? 
                StatementEntity.StatementType.valueOf(statementType.toUpperCase()) : null;
        
        Page<StatementEntity> statements = statementService.queryStatements(
                accountId, settleDate, type, pageable);
        
        Map<String, Object> result = new HashMap<>();
        result.put("statements", statements.getContent().stream()
                .map(stmt -> statementService.toDTO(stmt))
                .toList());
        result.put("pagination", Map.of(
                "page", page,
                "page_size", pageSize,
                "total", statements.getTotalElements(),
                "total_pages", statements.getTotalPages()
        ));
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
