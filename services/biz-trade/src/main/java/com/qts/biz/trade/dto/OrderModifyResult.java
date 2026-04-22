package com.qts.biz.trade.dto;

/**
 * Order modify result DTO
 */
public class OrderModifyResult {

    private boolean success;
    private String orderId;
    private String errorCode;
    private String errorMessage;

    public OrderModifyResult() {}

    public OrderModifyResult(boolean success, String orderId) {
        this.success = success;
        this.orderId = orderId;
    }

    public static OrderModifyResult success(String orderId) {
        return new OrderModifyResult(true, orderId);
    }

    public static OrderModifyResult failure(String errorCode, String errorMessage) {
        OrderModifyResult result = new OrderModifyResult(false, null);
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
