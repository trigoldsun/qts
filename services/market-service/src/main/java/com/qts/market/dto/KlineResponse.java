package com.qts.market.dto;

import com.qts.market.model.AdjustmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * K线查询响应数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KlineResponse {

    /**
     * 标的代码
     */
    private String symbol;

    /**
     * K线周期
     */
    private String period;

    /**
     * K线数据列表
     */
    private List<KlineData> klines;

    /**
     * 复权类型
     */
    private AdjustmentType adjustment;

    /**
     * 单条K线数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KlineData {
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private Long volume;
        private BigDecimal amount;
        private LocalDateTime timestamp;
    }
}
