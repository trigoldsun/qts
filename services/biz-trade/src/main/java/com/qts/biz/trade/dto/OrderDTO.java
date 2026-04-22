package com.qts.biz.trade.dto;

import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.enums.OrderType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Order Data Transfer Object
 * Represents the order data returned to clients
 */
public class OrderDTO {

    private String orderId;
    private String clientOrderId;
    private String accountId;
    private String symbol;
    private OrderSide side;
    private OrderType orderType;
    private BigDecimal price;
    private BigDecimal stopPrice;
    private Integer quantity;
    private Integer filledQuantity;
    private BigDecimal avgPrice;
    private OrderStatus status;
    private String rejectCode;
    private String rejectReason;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;
    private LocalDateTime filledAt;
    private LocalDateTime cancelledAt;
    private String strategyId;

    // Getters and Setters
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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public OrderSide getSide() {
        return side;
    }

    public void setSide(OrderSide side) {
        this.side = side;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderType orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getFilledQuantity() {
        return filledQuantity;
    }

    public void setFilledQuantity(Integer filledQuantity) {
        this.filledQuantity = filledQuantity;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getRejectCode() {
        return rejectCode;
    }

    public void setRejectCode(String rejectCode) {
        this.rejectCode = rejectCode;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(LocalDateTime filledAt) {
        this.filledAt = filledAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(LocalDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    /**
     * Builder pattern for OrderDTO
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OrderDTO dto = new OrderDTO();

        public Builder orderId(String orderId) {
            dto.orderId = orderId;
            return this;
        }

        public Builder clientOrderId(String clientOrderId) {
            dto.clientOrderId = clientOrderId;
            return this;
        }

        public Builder accountId(String accountId) {
            dto.accountId = accountId;
            return this;
        }

        public Builder symbol(String symbol) {
            dto.symbol = symbol;
            return this;
        }

        public Builder side(OrderSide side) {
            dto.side = side;
            return this;
        }

        public Builder orderType(OrderType orderType) {
            dto.orderType = orderType;
            return this;
        }

        public Builder price(BigDecimal price) {
            dto.price = price;
            return this;
        }

        public Builder stopPrice(BigDecimal stopPrice) {
            dto.stopPrice = stopPrice;
            return this;
        }

        public Builder quantity(Integer quantity) {
            dto.quantity = quantity;
            return this;
        }

        public Builder filledQuantity(Integer filledQuantity) {
            dto.filledQuantity = filledQuantity;
            return this;
        }

        public Builder avgPrice(BigDecimal avgPrice) {
            dto.avgPrice = avgPrice;
            return this;
        }

        public Builder status(OrderStatus status) {
            dto.status = status;
            return this;
        }

        public Builder rejectCode(String rejectCode) {
            dto.rejectCode = rejectCode;
            return this;
        }

        public Builder rejectReason(String rejectReason) {
            dto.rejectReason = rejectReason;
            return this;
        }

        public Builder version(Integer version) {
            dto.version = version;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            dto.createdAt = createdAt;
            return this;
        }

        public Builder submittedAt(LocalDateTime submittedAt) {
            dto.submittedAt = submittedAt;
            return this;
        }

        public Builder filledAt(LocalDateTime filledAt) {
            dto.filledAt = filledAt;
            return this;
        }

        public Builder cancelledAt(LocalDateTime cancelledAt) {
            dto.cancelledAt = cancelledAt;
            return this;
        }

        public Builder strategyId(String strategyId) {
            dto.strategyId = strategyId;
            return this;
        }

        public OrderDTO build() {
            return dto;
        }
    }
}