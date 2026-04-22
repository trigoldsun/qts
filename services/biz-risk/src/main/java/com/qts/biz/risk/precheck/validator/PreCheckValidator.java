package com.qts.biz.risk.precheck.validator;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Interface for pre-check validators
 * Each validator performs a specific risk check rule
 */
public interface PreCheckValidator {

    /**
     * Validate the pre-check request
     * @param request The pre-check request
     * @param context Context providing external data (positions, assets, prices, etc.)
     * @return ValidationResult indicating pass or failure with reason
     */
    ValidationResult validate(PreCheckRequest request, ValidationContext context);

    /**
     * Get the name of this validation rule
     */
    String getRuleName();

    /**
     * Context interface providing external data for validation
     */
    interface ValidationContext {
        BigDecimal getCurrentPosition(String symbol);
        BigDecimal getTotalAssets();
        BigDecimal getAvailableCash();
        BigDecimal getPreviousClose(String symbol);
        LocalDateTime getCurrentTime();
    }

    /**
     * Validation result
     */
    class ValidationResult {
        private final boolean passed;
        private final String reason;

        private ValidationResult(boolean passed, String reason) {
            this.passed = passed;
            this.reason = reason;
        }

        public static ValidationResult pass() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(false, reason);
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }
    }
}