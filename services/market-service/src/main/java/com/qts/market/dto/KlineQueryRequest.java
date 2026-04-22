package com.qts.market.dto;

import com.qts.market.model.AdjustmentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * K线查询请求参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineQueryRequest {

    /**
     * 标的代码，如 BTC-USDT, SH600000
     */
    @NotBlank(message = "symbol is required")
    private String symbol;

    /**
     * K线周期：1m, 5m, 1h, 1d
     */
    @NotBlank(message = "period is required")
    @Pattern(regexp = "^(1m|5m|1h|1d)$", message = "period must be one of: 1m, 5m, 1h, 1d")
    private String period;

    /**
     * 开始时间（可选）
     */
    private LocalDateTime startTime;

    /**
     * 结束时间（可选）
     */
    private LocalDateTime endTime;

    /**
     * 返回条数，默认100，最大1000
     */
    @Min(value = 1, message = "count must be at least 1")
    @Max(value = 1000, message = "count must not exceed 1000")
    private Integer count = 100;

    /**
     * 复权类型：FORWARD, BACKWARD, NONE
     */
    private AdjustmentType adjustment = AdjustmentType.NONE;
}
