package com.qts.biz.risk.precheck.validator;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Position limit validator
 * Validates single-stock position does not exceed 20% of total assets
 */
public class PositionLimitValidator implements PreCheckValidator {

    private static final BigDecimal MAX_POSITION_RATIO = new BigDecimal("0.20");

    @Override
    public ValidationResult validate(PreCheckRequest request, ValidationContext context) {
        if (request.isSell()) {
            // Sell orders don't affect position limit, skip validation
            return ValidationResult.pass();
        }

        BigDecimal currentPosition = context.getCurrentPosition(request.getSymbol());
        BigDecimal orderQuantity = request.getQuantity();
        BigDecimal totalAssets = context.getTotalAssets();

        if (totalAssets.compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.fail("Account has no assets");
        }

        // Calculate new position ratio after buy
        BigDecimal newPosition = currentPosition.add(orderQuantity);
        BigDecimal newPositionRatio = newPosition.divide(totalAssets, 4, RoundingMode.HALF_UP);

        if (newPositionRatio.compareTo(MAX_POSITION_RATIO) > 0) {
            return ValidationResult.fail(String.format(
                "Position limit exceeded: %.2f%% > %.0f%% (max ratio), current=%.2f, order=%.2f",
                newPositionRatio.multiply(BigDecimal.valueOf(100)).doubleValue(),
                MAX_POSITION_RATIO.multiply(BigDecimal.valueOf(100)).doubleValue(),
                currentPosition.doubleValue(),
                orderQuantity.doubleValue()
            ));
        }

        return ValidationResult.pass();
    }

    @Override
    public String getRuleName() {
        return "POSITION_LIMIT";
    }
}