package com.qts.biz.risk.precheck.validator;

import com.qts.biz.risk.precheck.dto.PreCheckRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Trading session validator
 * Validates orders are placed within allowed trading hours:
 * - 9:15-9:25 (call auction)
 * - 9:30-11:30 (continuous trading morning)
 * - 13:00-14:57 (continuous trading afternoon)
 */
public class TradingSessionValidator implements PreCheckValidator {

    private static final LocalTime SESSION1_START = LocalTime.of(9, 15);
    private static final LocalTime SESSION1_END = LocalTime.of(9, 25);
    private static final LocalTime SESSION2_START = LocalTime.of(9, 30);
    private static final LocalTime SESSION2_END = LocalTime.of(11, 30);
    private static final LocalTime SESSION3_START = LocalTime.of(13, 0);
    private static final LocalTime SESSION3_END = LocalTime.of(14, 57);

    @Override
    public ValidationResult validate(PreCheckRequest request, ValidationContext context) {
        LocalDateTime now = context.getCurrentTime();
        LocalTime time = now.toLocalTime();

        boolean isValid = isInSession(time, SESSION1_START, SESSION1_END) ||
                          isInSession(time, SESSION2_START, SESSION2_END) ||
                          isInSession(time, SESSION3_START, SESSION3_END);

        if (!isValid) {
            return ValidationResult.fail(String.format(
                "Outside trading session: current time=%s, valid sessions: 9:15-9:25, 9:30-11:30, 13:00-14:57",
                time.toString()
            ));
        }

        return ValidationResult.pass();
    }

    private boolean isInSession(LocalTime time, LocalTime start, LocalTime end) {
        return !time.isBefore(start) && !time.isAfter(end);
    }

    @Override
    public String getRuleName() {
        return "TRADING_SESSION";
    }
}