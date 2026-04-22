package com.qts.biz.risk.engine.exception;

/**
 * Risk Check Exception
 * Exception during risk evaluation process
 */
public class RiskCheckException extends RuntimeException {

    private final String rejectCode;

    public RiskCheckException(String rejectCode, String message) {
        super(message);
        this.rejectCode = rejectCode;
    }

    public RiskCheckException(String rejectCode, String message, Throwable cause) {
        super(message, cause);
        this.rejectCode = rejectCode;
    }

    public String getRejectCode() {
        return rejectCode;
    }
}
