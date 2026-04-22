package com.qts.biz.trade.dto;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Modify Order Command DTO
 * Represents the command to modify an existing order
 */
public class ModifyOrderCmd {

    @NotBlank(message = "account_id is required")
    @Size(max = 32, message = "account_id max length is 32")
    private String accountId;

    @NotBlank(message = "order_id is required")
    private String orderId;

    private BigDecimal newPrice;

    private Integer newQuantity;

    @NotNull(message = "modify_version is required")
    private Integer modifyVersion;

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

    public BigDecimal getNewPrice() {
        return newPrice;
    }

    public void setNewPrice(BigDecimal newPrice) {
        this.newPrice = newPrice;
    }

    public Integer getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(Integer newQuantity) {
        this.newQuantity = newQuantity;
    }

    public Integer getModifyVersion() {
        return modifyVersion;
    }

    public void setModifyVersion(Integer modifyVersion) {
        this.modifyVersion = modifyVersion;
    }
}