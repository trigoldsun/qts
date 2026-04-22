package com.qts.biz.trade.event;

import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * Order Domain Event
 * Base class for all order-related domain events
 */
public class OrderDomainEvent {

    private String eventId;
    private String orderId;
    private String accountId;
    private OrderStatus status;
    private LocalDateTime occurredAt;
    private String eventType;

    public OrderDomainEvent() {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }

    public OrderDomainEvent(OrderEntity order, String eventType) {
        this();
        this.orderId = order.getOrderId();
        this.accountId = order.getAccountId();
        this.status = order.getStatus();
        this.eventType = eventType;
    }

    // Getters and Setters
    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
}
