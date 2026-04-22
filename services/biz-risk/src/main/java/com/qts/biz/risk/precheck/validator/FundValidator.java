package com.qts.biz.risk.precheck.validator;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fund validator
 * Validates available funds >= order amount * margin rate
 */
public class FundValidator implements PreCheckValidator {

    @Override
    public ValidationResult validate(PreCheckRequest request, ValidationContext context) {
        if (request.isSell()) {
            // Sell orders don't require fund check for margin
            return ValidationResult.pass();
        }

        BigDecimal availableCash = context.getAvailableCash();
        BigDecimal orderAmount = request.getPrice().multiply(request.getQuantity());
        BigDecimal marginRate = request.getMarginRate() != null 
            ? request.getMarginRate() 
            : BigDecimal.valueOf(0.1); // default 10%

        BigDecimal requiredMargin = orderAmount.multiply(marginRate).setScale(4, RoundingMode.HALF_UP);

        if (availableCash.compareTo(requiredMargin) < 0) {
            return ValidationResult.fail(String.format(
                "Insufficient funds: available=%.2f, required=%.2f (amount=%.2f * marginRate=%.2f)",
                availableCash.doubleValue(),
                requiredMargin.doubleValue(),
                orderAmount.doubleValue(),
                marginRate.doubleValue()
            ));
        }

        return ValidationResult.pass();
    }

    @Override
    public String getRuleName() {
        return "FUND_CHECK";
    }
}