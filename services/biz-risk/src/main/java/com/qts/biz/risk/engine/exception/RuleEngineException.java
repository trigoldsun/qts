package com.qts.biz.risk.engine.exception;

/**
 * Rule Engine Exception
 * Base exception for rule engine errors
 */
public class RuleEngineException extends RuntimeException {

    private final String errorCode;

    public RuleEngineException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RuleEngineException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
