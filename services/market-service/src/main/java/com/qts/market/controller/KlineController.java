package com.qts.market.controller;

import com.qts.market.dto.KlineQueryRequest;
import com.qts.market.dto.KlineResponse;
import com.qts.market.model.AdjustmentType;
import com.qts.market.service.KlineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * K线查询控制器
 * GET /v1/market/kline
 */
@Slf4j
@RestController
@RequestMapping("/v1/market")
@RequiredArgsConstructor
public class KlineController {

    private final KlineService klineService;

    /**
     * 查询历史K线数据
     * 
     * @param symbol 标的代码
     * @param period K线周期 (1m/5m/1h/1d)
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param count 返回条数，默认100，最大1000
     * @param adjustment 复权类型 (FORWARD/BACKWARD/NONE)
     * @return K线数据响应
     */
    @GetMapping("/kline")
    public ResponseEntity<ApiResponse<KlineResponse>> getKline(
            @ModelAttribute @Valid KlineQueryRequest request) {

        log.info("GET /v1/market/kline - symbol={}, period={}, count={}, adjustment={}",
                request.getSymbol(), request.getPeriod(), request.getCount(), request.getAdjustment());

        KlineResponse response = klineService.queryKline(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 通用API响应封装
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApiResponse<T> {
        private int code;
        private String message;
        private T data;
        private String requestId;

        public static <T> ApiResponse<T> success(T data) {
            return new ApiResponse<>(0, "success", data, java.util.UUID.randomUUID().toString());
        }

        public static <T> ApiResponse<T> error(int code, String message) {
            return new ApiResponse<>(code, message, null, java.util.UUID.randomUUID().toString());
        }
    }
}
