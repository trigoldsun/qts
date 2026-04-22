package com.qts.market.controller;

import com.qts.market.dto.KlineQueryRequest;
import com.qts.market.dto.KlineResponse;
import com.qts.market.model.AdjustmentType;
import com.qts.market.service.KlineService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(KlineController.class)
class KlineControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KlineService klineService;

    @Test
    @DisplayName("GET /v1/market/kline - should return kline data")
    void testGetKline() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now();
        List<KlineResponse.KlineData> klines = Arrays.asList(
                KlineResponse.KlineData.builder()
                        .open(new BigDecimal("50000"))
                        .high(new BigDecimal("50100"))
                        .low(new BigDecimal("49900"))
                        .close(new BigDecimal("50050"))
                        .volume(1000L)
                        .amount(new BigDecimal("50050000"))
                        .timestamp(now.minusMinutes(1))
                        .build(),
                KlineResponse.KlineData.builder()
                        .open(new BigDecimal("50050"))
                        .high(new BigDecimal("50150"))
                        .low(new BigDecimal("49950"))
                        .close(new BigDecimal("50000"))
                        .volume(1500L)
                        .amount(new BigDecimal("75075000"))
                        .timestamp(now)
                        .build()
        );

        KlineResponse response = KlineResponse.builder()
                .symbol("BTC-USDT")
                .period("1m")
                .klines(klines)
                .adjustment(AdjustmentType.NONE)
                .build();

        when(klineService.queryKline(any(KlineQueryRequest.class))).thenReturn(response);

        // When/Then
        mockMvc.perform(get("/v1/market/kline")
                        .param("symbol", "BTC-USDT")
                        .param("period", "1m")
                        .param("count", "100")
                        .param("adjustment", "NONE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.symbol").value("BTC-USDT"))
                .andExpect(jsonPath("$.data.period").value("1m"))
                .andExpect(jsonPath("$.data.klines").isArray())
                .andExpect(jsonPath("$.data.klines.length()").value(2));
    }

    @Test
    @DisplayName("GET /v1/market/kline - should return error for missing symbol")
    void testGetKlineMissingSymbol() throws Exception {
        mockMvc.perform(get("/v1/market/kline")
                        .param("period", "1m")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/market/kline - should return error for invalid period")
    void testGetKlineInvalidPeriod() throws Exception {
        mockMvc.perform(get("/v1/market/kline")
                        .param("symbol", "BTC-USDT")
                        .param("period", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
