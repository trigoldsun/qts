package com.qts.biz.settle.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 日终清算请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailySettlementRequest {

    @NotNull(message = "settle_date is required")
    private LocalDate settleDate;

    private List<String> accountIds;  // null means all accounts
}
