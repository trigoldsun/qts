package com.qts.biz.settle.controller;

import com.qts.biz.settle.dto.*;
import com.qts.biz.settle.entity.ReconcileRecordEntity;
import com.qts.biz.settle.entity.StatementEntity;
import com.qts.biz.settle.service.DailySettlementService;
import com.qts.biz.settle.service.ReconcileService;
import com.qts.biz.settle.service.StatementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettleControllerTest {

    @Mock
    private DailySettlementService dailySettlementService;

    @Mock
    private ReconcileService reconcileService;

    @Mock
    private StatementService statementService;

    @InjectMocks
    private SettleController settleController;

    private DailySettlementRequest validRequest;
    private DailySettlementResponse settlementResponse;
    private ReconcileRecordEntity reconcileRecord;
    private StatementEntity statementEntity;

    @BeforeEach
    void setUp() {
        validRequest = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .accountIds(List.of("ACC-001", "ACC-002"))
                .build();

        settlementResponse = DailySettlementResponse.builder()
                .settleId("Settle-2024-01-15-test")
                .settleDate(LocalDate.of(2024, 1, 15))
                .status("COMPLETED")
                .startedAt(LocalDateTime.of(2024, 1, 15, 18, 0, 0))
                .accountsCount(2)
                .build();

        reconcileRecord = ReconcileRecordEntity.builder()
                .reconcileId("REC-001")
                .accountId("ACC-001")
                .reconcileTime(LocalDateTime.of(2024, 1, 15, 18, 30, 0))
                .assetStatus(ReconcileRecordEntity.ReconcileStatus.MATCH)
                .positionStatus(ReconcileRecordEntity.ReconcileStatus.MATCH)
                .differenceCount(0)
                .build();

        statementEntity = StatementEntity.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .settleDate(LocalDate.of(2024, 1, 15))
                .statementType(StatementEntity.StatementType.DAILY)
                .totalAssetsEnd(new BigDecimal("100000.0000"))
                .build();
    }

    @Test
    void triggerDailySettlement_Success() {
        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(settlementResponse);

        ResponseEntity<ApiResponse<DailySettlementResponse>> response = 
                settleController.triggerDailySettlement(validRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        
        ApiResponse<DailySettlementResponse> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.getCode());
        assertEquals("success", body.getMessage());
        assertNotNull(body.getData());
        assertEquals("Settle-2024-01-15-test", body.getData().getSettleId());

        verify(dailySettlementService).triggerDailySettlement(validRequest);
    }

    @Test
    void triggerDailySettlement_NullAccountIds() {
        DailySettlementRequest requestWithNullAccounts = DailySettlementRequest.builder()
                .settleDate(LocalDate.of(2024, 1, 15))
                .accountIds(null)
                .build();

        DailySettlementResponse responseWithNullAccounts = DailySettlementResponse.builder()
                .settleId("Settle-2024-01-15-null")
                .settleDate(LocalDate.of(2024, 1, 15))
                .status("COMPLETED")
                .accountsCount(0)
                .build();

        when(dailySettlementService.triggerDailySettlement(any(DailySettlementRequest.class)))
                .thenReturn(responseWithNullAccounts);

        ResponseEntity<ApiResponse<DailySettlementResponse>> response = 
                settleController.triggerDailySettlement(requestWithNullAccounts);

        assertNotNull(response);
        assertEquals(0, response.getBody().getCode());
        verify(dailySettlementService).triggerDailySettlement(requestWithNullAccounts);
    }

    @Test
    void queryReconcileRecords_Success() {
        Page<ReconcileRecordEntity> page = new PageImpl<>(List.of(reconcileRecord));
        when(reconcileService.queryReconcileRecords(anyString(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryReconcileRecords("ACC-001", null, 1, 20);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        
        ApiResponse<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.getCode());
        
        Map<String, Object> data = body.getData();
        assertNotNull(data);
        assertTrue(data.containsKey("reconcile_results"));
        assertTrue(data.containsKey("pagination"));
        
        @SuppressWarnings("unchecked")
        List<ReconcileRecordEntity> results = (List<ReconcileRecordEntity>) data.get("reconcile_results");
        assertEquals(1, results.size());
        assertEquals("REC-001", results.get(0).getReconcileId());
    }

    @Test
    void queryReconcileRecords_Empty() {
        Page<ReconcileRecordEntity> emptyPage = new PageImpl<>(List.of());
        when(reconcileService.queryReconcileRecords(any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryReconcileRecords(null, null, 1, 10);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        
        @SuppressWarnings("unchecked")
        List<ReconcileRecordEntity> results = (List<ReconcileRecordEntity>) response.getBody().getData().get("reconcile_results");
        assertTrue(results.isEmpty());
    }

    @Test
    void queryReconcileRecords_WithReconcileTime() {
        LocalDateTime reconcileTime = LocalDateTime.of(2024, 1, 15, 18, 30, 0);
        Page<ReconcileRecordEntity> page = new PageImpl<>(List.of(reconcileRecord));
        when(reconcileService.queryReconcileRecords(anyString(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryReconcileRecords("ACC-001", reconcileTime, 1, 20);

        assertNotNull(response);
        verify(reconcileService).queryReconcileRecords(eq("ACC-001"), eq(reconcileTime), any(Pageable.class));
    }

    @Test
    void queryReconcileRecords_Pagination() {
        Page<ReconcileRecordEntity> page = new PageImpl<>(List.of(reconcileRecord));
        when(reconcileService.queryReconcileRecords(any(), any(), any(Pageable.class)))
                .thenReturn(page);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryReconcileRecords(null, null, 2, 50);

        assertNotNull(response);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) response.getBody().getData().get("pagination");
        assertEquals(2, pagination.get("page"));
        assertEquals(50, pagination.get("page_size"));
    }

    @Test
    void queryStatements_Success() {
        Page<StatementEntity> page = new PageImpl<>(List.of(statementEntity));
        StatementDTO statementDTO = StatementDTO.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        when(statementService.queryStatements(anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(statementService.toDTO(any(StatementEntity.class))).thenReturn(statementDTO);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryStatements("ACC-001", LocalDate.of(2024, 1, 15), "DAILY", 1, 20);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        
        ApiResponse<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertEquals(0, body.getCode());
        
        @SuppressWarnings("unchecked")
        List<StatementDTO> statements = (List<StatementDTO>) body.getData().get("statements");
        assertEquals(1, statements.size());
        assertEquals("STMT-001", statements.get(0).getStatementId());
    }

    @Test
    void queryStatements_Empty() {
        Page<StatementEntity> emptyPage = new PageImpl<>(List.of());
        when(statementService.queryStatements(any(), any(), any(), any(Pageable.class)))
                .thenReturn(emptyPage);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryStatements(null, null, null, 1, 10);

        assertNotNull(response);
        
        @SuppressWarnings("unchecked")
        List<StatementDTO> statements = (List<StatementDTO>) response.getBody().getData().get("statements");
        assertTrue(statements.isEmpty());
    }

    @Test
    void queryStatements_WithAllParams() {
        Page<StatementEntity> page = new PageImpl<>(List.of(statementEntity));
        StatementDTO statementDTO = StatementDTO.builder()
                .statementId("STMT-001")
                .accountId("ACC-001")
                .build();
        
        when(statementService.queryStatements(anyString(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(statementService.toDTO(any(StatementEntity.class))).thenReturn(statementDTO);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryStatements("ACC-001", LocalDate.of(2024, 1, 15), "MONTHLY", 1, 50);

        assertNotNull(response);
        verify(statementService).queryStatements(
                eq("ACC-001"), 
                eq(LocalDate.of(2024, 1, 15)), 
                eq(StatementEntity.StatementType.MONTHLY), 
                any(Pageable.class));
    }

    @Test
    void queryStatements_Pagination() {
        Page<StatementEntity> page = new PageImpl<>(List.of(statementEntity));
        StatementDTO statementDTO = StatementDTO.builder()
                .statementId("STMT-001")
                .build();
        
        when(statementService.queryStatements(any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        when(statementService.toDTO(any(StatementEntity.class))).thenReturn(statementDTO);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryStatements(null, null, null, 3, 100);

        assertNotNull(response);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> pagination = (Map<String, Object>) response.getBody().getData().get("pagination");
        assertEquals(3, pagination.get("page"));
        assertEquals(100, pagination.get("page_size"));
    }

    @Test
    void queryStatements_NullStatementType() {
        Page<StatementEntity> page = new PageImpl<>(List.of(statementEntity));
        StatementDTO statementDTO = StatementDTO.builder()
                .statementId("STMT-001")
                .build();
        
        when(statementService.queryStatements(any(), any(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(statementService.toDTO(any(StatementEntity.class))).thenReturn(statementDTO);

        ResponseEntity<ApiResponse<Map<String, Object>>> response = 
                settleController.queryStatements("ACC-001", null, null, 1, 20);

        assertNotNull(response);
        verify(statementService).queryStatements(eq("ACC-001"), any(), isNull(), any(Pageable.class));
    }
}