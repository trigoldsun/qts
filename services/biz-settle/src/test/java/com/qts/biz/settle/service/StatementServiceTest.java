package com.qts.biz.settle.service;

import com.qts.biz.settle.dto.StatementDTO;
import com.qts.biz.settle.entity.StatementEntity;
import com.qts.biz.settle.entity.StatementEntity.StatementType;
import com.qts.biz.settle.repository.StatementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatementServiceTest {

    @Mock
    private StatementRepository statementRepository;

    @InjectMocks
    private StatementService statementService;

    private StatementEntity sampleStatement;
    private LocalDate settleDate;

    @BeforeEach
    void setUp() {
        settleDate = LocalDate.of(2024, 1, 15);

        sampleStatement = StatementEntity.builder()
                .statementId("Stmt-ACC-001-2024-01-15")
                .accountId("ACC-001")
                .settleDate(settleDate)
                .statementType(StatementType.DAILY)
                .totalAssetsStart(BigDecimal.valueOf(100000))
                .totalAssetsEnd(BigDecimal.valueOf(105000))
                .availableCash(BigDecimal.valueOf(50000))
                .frozenCash(BigDecimal.ZERO)
                .marketValue(BigDecimal.valueOf(55000))
                .totalProfitLoss(BigDecimal.valueOf(5000))
                .todayProfitLoss(BigDecimal.valueOf(500))
                .totalCommission(BigDecimal.valueOf(80))
                .totalStampDuty(BigDecimal.valueOf(30))
                .totalExchangeFee(BigDecimal.valueOf(10))
                .netProfitLoss(BigDecimal.valueOf(380))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void queryStatements_AllParameters() {
        Pageable pageable = PageRequest.of(0, 10);

        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                "ACC-001", settleDate, StatementType.DAILY))
                .thenReturn(Optional.of(sampleStatement));

        Page<StatementEntity> result = statementService.queryStatements(
                "ACC-001", settleDate, StatementType.DAILY, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(sampleStatement.getStatementId(), result.getContent().get(0).getStatementId());
    }

    @Test
    void queryStatements_AccountIdAndDate() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StatementEntity> expectedPage = new PageImpl<>(
                Collections.singletonList(sampleStatement), pageable, 1);

        when(statementRepository.findByAccountIdAndSettleDateBetween(
                "ACC-001", settleDate, settleDate.plusDays(1), pageable))
                .thenReturn(expectedPage);

        Page<StatementEntity> result = statementService.queryStatements(
                "ACC-001", settleDate, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void queryStatements_AccountIdOnly() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StatementEntity> expectedPage = new PageImpl<>(
                Collections.singletonList(sampleStatement), pageable, 1);

        when(statementRepository.findByAccountId("ACC-001", pageable))
                .thenReturn(expectedPage);

        Page<StatementEntity> result = statementService.queryStatements(
                "ACC-001", null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void queryStatements_NoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StatementEntity> expectedPage = new PageImpl<>(
                Collections.singletonList(sampleStatement), pageable, 1);

        when(statementRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<StatementEntity> result = statementService.queryStatements(
                null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void queryStatements_NotFound() {
        Pageable pageable = PageRequest.of(0, 10);

        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                "ACC-NOTFOUND", settleDate, StatementType.DAILY))
                .thenReturn(Optional.empty());

        Page<StatementEntity> result = statementService.queryStatements(
                "ACC-NOTFOUND", settleDate, StatementType.DAILY, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void generateDailyStatement_Success() {
        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                "ACC-001", settleDate, StatementType.DAILY))
                .thenReturn(Optional.empty());
        when(statementRepository.save(any(StatementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatementEntity result = statementService.generateDailyStatement("ACC-001", settleDate);

        assertNotNull(result);
        assertTrue(result.getStatementId().contains("ACC-001"));
        assertEquals("ACC-001", result.getAccountId());
        assertEquals(settleDate, result.getSettleDate());
        assertEquals(StatementType.DAILY, result.getStatementType());
        assertNotNull(result.getTotalAssetsStart());
        assertNotNull(result.getTotalAssetsEnd());
        assertNotNull(result.getAvailableCash());

        verify(statementRepository, times(1)).save(any(StatementEntity.class));
    }

    @Test
    void generateDailyStatement_AlreadyExists() {
        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                "ACC-001", settleDate, StatementType.DAILY))
                .thenReturn(Optional.of(sampleStatement));

        StatementEntity result = statementService.generateDailyStatement("ACC-001", settleDate);

        assertNotNull(result);
        assertEquals(sampleStatement.getStatementId(), result.getStatementId());

        verify(statementRepository, never()).save(any(StatementEntity.class));
    }

    @Test
    void generateDailyStatement_MonthlyStatement() {
        // generateDailyStatement always queries for DAILY, not MONTHLY
        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                "ACC-001", settleDate, StatementType.DAILY))
                .thenReturn(Optional.empty());
        when(statementRepository.save(any(StatementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StatementEntity result = statementService.generateDailyStatement("ACC-001", settleDate);

        assertNotNull(result);
        assertEquals(StatementType.DAILY, result.getStatementType());
    }

    @Test
    void toDTO_Success() {
        StatementDTO dto = statementService.toDTO(sampleStatement);

        assertNotNull(dto);
        assertEquals(sampleStatement.getStatementId(), dto.getStatementId());
        assertEquals(sampleStatement.getAccountId(), dto.getAccountId());
        assertEquals(sampleStatement.getSettleDate(), dto.getSettleDate());
        assertEquals(StatementType.DAILY.name(), dto.getStatementType());
        assertEquals(sampleStatement.getTotalAssetsStart(), dto.getTotalAssetsStart());
        assertEquals(sampleStatement.getTotalAssetsEnd(), dto.getTotalAssetsEnd());
        assertEquals(sampleStatement.getAvailableCash(), dto.getAvailableCash());
        assertEquals(sampleStatement.getFrozenCash(), dto.getFrozenCash());
        assertEquals(sampleStatement.getMarketValue(), dto.getMarketValue());
        assertEquals(sampleStatement.getTotalProfitLoss(), dto.getTotalProfitLoss());
        assertEquals(sampleStatement.getTodayProfitLoss(), dto.getTodayProfitLoss());
        assertEquals(sampleStatement.getTotalCommission(), dto.getTotalCommission());
        assertEquals(sampleStatement.getTotalStampDuty(), dto.getTotalStampDuty());
        assertEquals(sampleStatement.getTotalExchangeFee(), dto.getTotalExchangeFee());
        assertEquals(sampleStatement.getNetProfitLoss(), dto.getNetProfitLoss());
        assertEquals(sampleStatement.getCreatedAt(), dto.getCreatedAt());
    }

    @Test
    void toDTO_AllFieldsMapped() {
        StatementDTO dto = statementService.toDTO(sampleStatement);

        assertEquals(BigDecimal.valueOf(100000), dto.getTotalAssetsStart());
        assertEquals(BigDecimal.valueOf(105000), dto.getTotalAssetsEnd());
        assertEquals(BigDecimal.valueOf(50000), dto.getAvailableCash());
        assertEquals(BigDecimal.ZERO, dto.getFrozenCash());
        assertEquals(BigDecimal.valueOf(55000), dto.getMarketValue());
        assertEquals(BigDecimal.valueOf(5000), dto.getTotalProfitLoss());
        assertEquals(BigDecimal.valueOf(500), dto.getTodayProfitLoss());
        assertEquals(BigDecimal.valueOf(80), dto.getTotalCommission());
        assertEquals(BigDecimal.valueOf(30), dto.getTotalStampDuty());
        assertEquals(BigDecimal.valueOf(10), dto.getTotalExchangeFee());
        assertEquals(BigDecimal.valueOf(380), dto.getNetProfitLoss());
    }

    @Test
    void generateDailyStatement_MultipleAccounts() {
        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                anyString(), eq(settleDate), eq(StatementType.DAILY)))
                .thenReturn(Optional.empty());
        when(statementRepository.save(any(StatementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        statementService.generateDailyStatement("ACC-001", settleDate);
        statementService.generateDailyStatement("ACC-002", settleDate);

        verify(statementRepository, times(2)).save(any(StatementEntity.class));
    }

    @Test
    void queryStatements_EmptyResult() {
        Pageable pageable = PageRequest.of(0, 10);

        when(statementRepository.findByAccountIdAndSettleDateAndStatementType(
                "ACC-EMPTY", settleDate, StatementType.DAILY))
                .thenReturn(Optional.empty());

        Page<StatementEntity> result = statementService.queryStatements(
                "ACC-EMPTY", settleDate, StatementType.DAILY, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
