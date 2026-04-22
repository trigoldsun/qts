package com.qts.biz.trade.dto;

/**
 * Order send result DTO
 */
public class OrderSendResult {

    private boolean success;
    private String orderId;
    private String errorCode;
    private String errorMessage;

    public OrderSendResult() {}

    public OrderSendResult(boolean success, String orderId) {
        this.success = success;
        this.orderId = orderId;
    }

    public static OrderSendResult success(String orderId) {
        return new OrderSendResult(true, orderId);
    }

    public static OrderSendResult failure(String errorCode, String errorMessage) {
        OrderSendResult result = new OrderSendResult(false, null);
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
