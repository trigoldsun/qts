package com.qts.market.service;

import com.qts.market.handler.MarketWebSocketHandler;
import com.qts.market.model.Kline;
import com.qts.market.repository.KlineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArraySet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketPushServiceTest {

    @Mock
    private KlineRepository klineRepository;

    @Mock
    private MarketWebSocketHandler marketWebSocketHandler;

    @InjectMocks
    private MarketPushService marketPushService;

    private Kline createKline(String symbol, String period) {
        return Kline.builder()
                .id(1L)
                .symbol(symbol)
                .period(period)
                .timestamp(LocalDateTime.now())
                .openPrice(new BigDecimal("50000"))
                .highPrice(new BigDecimal("50100"))
                .lowPrice(new BigDecimal("49900"))
                .closePrice(new BigDecimal("50050"))
                .volume(1000L)
                .amount(new BigDecimal("50050000"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should subscribe and track subscription")
    void testSubscribe() {
        // When
        marketPushService.subscribe("BTC-USDT", "1m", "session-1");

        // Then
        assertEquals(1, marketPushService.getActiveSubscriptionCount());
    }

    @Test
    @DisplayName("Should unsubscribe and remove subscription")
    void testUnsubscribe() {
        // Given
        marketPushService.subscribe("BTC-USDT", "1m", "session-1");

        // When
        marketPushService.unsubscribe("BTC-USDT", "1m", "session-1");

        // Then
        assertEquals(0, marketPushService.getActiveSubscriptionCount());
    }

    @Test
    @DisplayName("Should support multiple sessions for same symbol/period")
    void testMultipleSessions() {
        // Given
        marketPushService.subscribe("BTC-USDT", "1m", "session-1");
        marketPushService.subscribe("BTC-USDT", "1m", "session-2");

        // Then
        assertEquals(1, marketPushService.getActiveSubscriptionCount());
    }

    @Test
    @DisplayName("Should push market data to subscribers")
    void testPushMarketData() {
        // Given
        String symbol = "BTC-USDT";
        String period = "1m";
        Kline kline = createKline(symbol, period);

        marketPushService.subscribe(symbol, period, "session-1");

        when(klineRepository.findLatestBySymbolAndPeriod(symbol, period))
                .thenReturn(kline);

        // When
        marketPushService.pushMarketData();

        // Then
        verify(marketWebSocketHandler).broadcastKlineData(
                eq(symbol), eq(period), any());
    }

    @Test
    @DisplayName("Should not push when no subscribers")
    void testPushWithNoSubscribers() {
        // When
        marketPushService.pushMarketData();

        // Then
        verify(marketWebSocketHandler, never()).broadcastKlineData(
                any(), any(), any());
    }
}
