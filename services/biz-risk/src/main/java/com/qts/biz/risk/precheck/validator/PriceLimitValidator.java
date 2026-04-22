package com.qts.biz.risk.precheck.validator;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Price limit validator
 * Validates order price is within ±10% of previous close price
 */
public class PriceLimitValidator implements PreCheckValidator {

    private static final BigDecimal MAX_PRICE_CHANGE_RATIO = new BigDecimal("0.10");

    @Override
    public ValidationResult validate(PreCheckRequest request, ValidationContext context) {
        BigDecimal orderPrice = request.getPrice();
        BigDecimal previousClose = context.getPreviousClose(request.getSymbol());

        if (previousClose == null || previousClose.compareTo(BigDecimal.ZERO) <= 0) {
            // No previous close price, skip validation or fail depending on requirements
            return ValidationResult.pass(); // Allow trading if no price reference
        }

        BigDecimal priceChange = orderPrice.subtract(previousClose).abs();
        BigDecimal priceChangeRatio = priceChange.divide(previousClose, 4, RoundingMode.HALF_UP);

        if (priceChangeRatio.compareTo(MAX_PRICE_CHANGE_RATIO) > 0) {
            return ValidationResult.fail(String.format(
                "Price limit exceeded: change=%.2f%% > %.0f%%, orderPrice=%.2f, previousClose=%.2f",
                priceChangeRatio.multiply(BigDecimal.valueOf(100)).doubleValue(),
                MAX_PRICE_CHANGE_RATIO.multiply(BigDecimal.valueOf(100)).doubleValue(),
                orderPrice.doubleValue(),
                previousClose.doubleValue()
            ));
        }

        return ValidationResult.pass();
    }

    @Override
    public String getRuleName() {
        return "PRICE_LIMIT";
    }
}