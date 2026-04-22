package com.qts.market.service;

import com.qts.market.dto.KlineQueryRequest;
import com.qts.market.dto.KlineResponse;
import com.qts.market.model.AdjustmentType;
import com.qts.market.model.Kline;
import com.qts.market.repository.KlineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KlineServiceTest {

    @Mock
    private KlineRepository klineRepository;

    @InjectMocks
    private KlineService klineService;

    private Kline createKline(String symbol, String period, LocalDateTime timestamp,
                              BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                              Long volume, BigDecimal amount) {
        return Kline.builder()
                .id(1L)
                .symbol(symbol)
                .period(period)
                .timestamp(timestamp)
                .openPrice(open)
                .highPrice(high)
                .lowPrice(low)
                .closePrice(close)
                .volume(volume)
                .amount(amount)
                .adjustment(AdjustmentType.NONE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setUp() {
    }

    @Test
    @DisplayName("Should query kline by count")
    void testQueryKlineByCount() {
        // Given
        KlineQueryRequest request = KlineQueryRequest.builder()
                .symbol("BTC-USDT")
                .period("1m")
                .count(10)
                .adjustment(AdjustmentType.NONE)
                .build();

        LocalDateTime now = LocalDateTime.now();
        List<Kline> klines = Arrays.asList(
                createKline("BTC-USDT", "1m", now.minusMinutes(0),
                        new BigDecimal("50000"), new BigDecimal("50100"), new BigDecimal("49900"), new BigDecimal("50050"),
                        1000L, new BigDecimal("50050000")),
                createKline("BTC-USDT", "1m", now.minusMinutes(1),
                        new BigDecimal("50050"), new BigDecimal("50150"), new BigDecimal("49950"), new BigDecimal("50000"),
                        1500L, new BigDecimal("75075000"))
        );

        when(klineRepository.findBySymbolAndPeriodOrderByTimestampDesc(
                eq("BTC-USDT"), eq("1m"), any(PageRequest.class)))
                .thenReturn(klines);

        // When
        KlineResponse response = klineService.queryKline(request);

        // Then
        assertNotNull(response);
        assertEquals("BTC-USDT", response.getSymbol());
        assertEquals("1m", response.getPeriod());
        assertEquals(2, response.getKlines().size());
        assertEquals(AdjustmentType.NONE, response.getAdjustment());
    }

    @Test
    @DisplayName("Should query kline by time range")
    void testQueryKlineByTimeRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();

        KlineQueryRequest request = KlineQueryRequest.builder()
                .symbol("ETH-USDT")
                .period("5m")
                .startTime(startTime)
                .endTime(endTime)
                .count(100)
                .adjustment(AdjustmentType.NONE)
                .build();

        List<Kline> klines = Arrays.asList(
                createKline("ETH-USDT", "5m", startTime.plusMinutes(0),
                        new BigDecimal("3000"), new BigDecimal("3050"), new BigDecimal("2980"), new BigDecimal("3020"),
                        500L, new BigDecimal("1510000")),
                createKline("ETH-USDT", "5m", startTime.plusMinutes(5),
                        new BigDecimal("3020"), new BigDecimal("3060"), new BigDecimal("3010"), new BigDecimal("3030"),
                        600L, new BigDecimal("1818000"))
        );

        when(klineRepository.findBySymbolAndPeriodAndTimeRange(
                eq("ETH-USDT"), eq("5m"), eq(startTime), eq(endTime)))
                .thenReturn(klines);

        // When
        KlineResponse response = klineService.queryKline(request);

        // Then
        assertNotNull(response);
        assertEquals("ETH-USDT", response.getSymbol());
        assertEquals("5m", response.getPeriod());
        assertEquals(2, response.getKlines().size());
    }

    @Test
    @DisplayName("Should return empty list when no data")
    void testQueryKlineEmptyResult() {
        // Given
        KlineQueryRequest request = KlineQueryRequest.builder()
                .symbol("UNKNOWN")
                .period("1m")
                .count(100)
                .adjustment(AdjustmentType.NONE)
                .build();

        when(klineRepository.findBySymbolAndPeriodOrderByTimestampDesc(
                eq("UNKNOWN"), eq("1m"), any(PageRequest.class)))
                .thenReturn(List.of());

        // When
        KlineResponse response = klineService.queryKline(request);

        // Then
        assertNotNull(response);
        assertEquals("UNKNOWN", response.getSymbol());
        assertTrue(response.getKlines().isEmpty());
    }
}
