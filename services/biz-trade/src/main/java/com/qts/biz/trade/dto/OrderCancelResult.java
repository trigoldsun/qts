package com.qts.biz.trade.dto;

/**
 * Order cancel result DTO
 */
public class OrderCancelResult {

    private boolean success;
    private String orderId;
    private String errorCode;
    private String errorMessage;

    public OrderCancelResult() {}

    public OrderCancelResult(boolean success, String orderId) {
        this.success = success;
        this.orderId = orderId;
    }

    public static OrderCancelResult success(String orderId) {
        return new OrderCancelResult(true, orderId);
    }

    public static OrderCancelResult failure(String errorCode, String errorMessage) {
        OrderCancelResult result = new OrderCancelResult(false, null);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
