package com.qts.biz.trade.exception;

/**
 * Exception thrown when an invalid order state transition is attempted
 */
public class OrderStateTransitionException extends RuntimeException {

    public OrderStateTransitionException(String message) {
        super(message);
    }

    public OrderStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
