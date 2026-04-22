package com.qts.biz.trade.dto;

import com.qts.biz.trade.enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * Cancel Order Result DTO
 * Represents the result of a cancel order operation
 */
public class CancelResult {

    private String orderId;
    private OrderStatus status;
    private LocalDateTime cancelledAt;

    public CancelResult() {}

    public CancelResult(String orderId, OrderStatus status, LocalDateTime cancelledAt) {
        this.orderId = orderId;
        this.status = status;
        this.cancelledAt = cancelledAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }
}
