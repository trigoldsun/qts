package com.qts.biz.trade.dto;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Cancel Order Command DTO
 * Represents the command to cancel an existing order
 */
public class CancelOrderCmd {

    @NotBlank(message = "account_id is required")
    @Size(max = 32, message = "account_id max length is 32")
    private String accountId;

    @NotBlank(message = "order_id is required")
    private String orderId;

    private String clientOrderId;

    // Getters and Setters
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }
}