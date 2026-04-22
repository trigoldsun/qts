package com.qts.biz.trade.dto;

import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderType;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Place Order Command DTO
 * Represents the command to place a new order
 */
public class PlaceOrderCmd {

    @NotBlank(message = "account_id is required")
    @Size(max = 32, message = "account_id max length is 32")
    private String accountId;

    @NotBlank(message = "symbol is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "symbol must be 6 digit number")
    private String symbol;

    @NotNull(message = "side is required")
    private OrderSide side;

    @NotNull(message = "order_type is required")
    private OrderType orderType;

    @NotNull(message = "quantity is required")
    @Min(value = 100, message = "quantity must be at least 100")
    @DecimalMultiple(value = 100, message = "quantity must be multiple of 100")
    private Integer quantity;

    private BigDecimal price;

    private BigDecimal stopPrice;

    @NotBlank(message = "client_order_id is required")
    @Size(max = 64, message = "client_order_id max length is 64")
    private String clientOrderId;

    @Size(max = 64, message = "strategy_id max length is 64")
    private String strategyId;

    // Getters and Setters
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

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
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

    public String getClientOrderId() {
        return clientOrderId;
    }

    public void setClientOrderId(String clientOrderId) {
        this.clientOrderId = clientOrderId;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }
}