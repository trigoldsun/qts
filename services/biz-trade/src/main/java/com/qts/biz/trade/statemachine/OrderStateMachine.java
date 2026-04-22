package com.qts.biz.trade.statemachine;

import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.entity.OrderStateLog;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.event.OrderDomainEvent;
import com.qts.biz.trade.event.OrderEventPublisher;
import com.qts.biz.trade.exception.OrderStateTransitionException;
import com.qts.biz.trade.repository.OrderRepository;
import com.qts.biz.trade.repository.OrderStateLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Order State Machine
 * Manages order state transitions and publishes domain events
 * 
 * State transitions:
 * - CREATED + submit() -> SUBMITTED
 * - SUBMITTED + partialFill(qty) -> PARTIAL_FILLED
 * - PARTIAL_FILLED + fill() -> FILLED
 * - SUBMITTED/PARTIAL_FILLED + cancel() -> CANCELLED
 * - SUBMITTED/PARTIAL_FILLED + reject(reason) -> REJECTED
 */
@Component
public class OrderStateMachine {

    private static final Logger logger = LoggerFactory.getLogger(OrderStateMachine.class);

    private final OrderRepository orderRepository;
    private final OrderStateLogRepository stateLogRepository;
    private final OrderEventPublisher eventPublisher;

    @Autowired
    public OrderStateMachine(OrderRepository orderRepository,
                            OrderStateLogRepository stateLogRepository,
                            OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.stateLogRepository = stateLogRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Submit order - transition from CREATED to SUBMITTED
     */
    @Transactional
    public OrderEntity submit(OrderEntity order) {
        validateTransition(order.getStatus(), OrderStatus.SUBMITTED);
        
        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.SUBMITTED);
        order.setSubmittedAt(LocalDateTime.now());
        
        OrderEntity savedOrder = orderRepository.save(order);
        logStateTransition(order.getOrderId(), fromStatus, OrderStatus.SUBMITTED, "SUBMIT", null);
        eventPublisher.publishOrderSubmittedEvent(savedOrder);
        
        logger.info("Order {} transitioned from {} to SUBMITTED", order.getOrderId(), fromStatus);
        return savedOrder;
    }

    /**
     * Partial fill order - transition from SUBMITTED to PARTIAL_FILLED
     */
    @Transactional
    public OrderEntity partialFill(OrderEntity order, Integer filledQty) {
        validateTransition(order.getStatus(), OrderStatus.PARTIAL_FILLED);
        
        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.PARTIAL_FILLED);
        order.setFilledQuantity(filledQty);
        
        OrderEntity savedOrder = orderRepository.save(order);
        logStateTransition(order.getOrderId(), fromStatus, OrderStatus.PARTIAL_FILLED, "PARTIAL_FILL", String.valueOf(filledQty));
        eventPublisher.publishOrderPartialFilledEvent(savedOrder);
        
        logger.info("Order {} transitioned from {} to PARTIAL_FILLED with qty {}", 
                   order.getOrderId(), fromStatus, filledQty);
        return savedOrder;
    }

    /**
     * Fill order - transition to FILLED (from SUBMITTED or PARTIAL_FILLED)
     */
    @Transactional
    public OrderEntity fill(OrderEntity order, Integer totalFilledQty, Double avgPrice) {
        validateCanFill(order.getStatus());
        
        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.FILLED);
        order.setFilledQuantity(totalFilledQty);
        order.setAvgPrice(avgPrice != null ? (avgPrice instanceof java.math.BigDecimal ? (java.math.BigDecimal) avgPrice : java.math.BigDecimal.valueOf(avgPrice)) : null);
        order.setFilledAt(LocalDateTime.now());
        
        OrderEntity savedOrder = orderRepository.save(order);
        logStateTransition(order.getOrderId(), fromStatus, OrderStatus.FILLED, "FILL", null);
        eventPublisher.publishOrderFilledEvent(savedOrder);
        
        logger.info("Order {} transitioned from {} to FILLED", order.getOrderId(), fromStatus);
        return savedOrder;
    }

    /**
     * Cancel order - transition to CANCELLED (from SUBMITTED or PARTIAL_FILLED)
     */
    @Transactional
    public OrderEntity cancel(OrderEntity order) {
        validateCanCancel(order.getStatus());
        
        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        
        OrderEntity savedOrder = orderRepository.save(order);
        logStateTransition(order.getOrderId(), fromStatus, OrderStatus.CANCELLED, "CANCEL", null);
        eventPublisher.publishOrderCancelledEvent(savedOrder);
        
        logger.info("Order {} transitioned from {} to CANCELLED", order.getOrderId(), fromStatus);
        return savedOrder;
    }

    /**
     * Reject order - transition to REJECTED (from SUBMITTED or PARTIAL_FILLED)
     */
    @Transactional
    public OrderEntity reject(OrderEntity order, String reason) {
        validateCanReject(order.getStatus());
        
        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.REJECTED);
        order.setRejectCode("RISK_REJECT");
        order.setRejectReason(reason);
        
        OrderEntity savedOrder = orderRepository.save(order);
        logStateTransition(order.getOrderId(), fromStatus, OrderStatus.REJECTED, "REJECT", reason);
        eventPublisher.publishOrderRejectedEvent(savedOrder);
        
        logger.info("Order {} transitioned from {} to REJECTED: {}", order.getOrderId(), fromStatus, reason);
        return savedOrder;
    }

    /**
     * Validate state transition is allowed
     */
    private void validateTransition(OrderStatus currentStatus, OrderStatus targetStatus) {
        boolean valid = false;
        
        switch (targetStatus) {
            case SUBMITTED:
                valid = currentStatus == OrderStatus.CREATED;
                break;
            case PARTIAL_FILLED:
                valid = currentStatus == OrderStatus.SUBMITTED;
                break;
            case FILLED:
                valid = currentStatus == OrderStatus.SUBMITTED || currentStatus == OrderStatus.PARTIAL_FILLED;
                break;
            case CANCELLED:
                valid = currentStatus == OrderStatus.SUBMITTED || currentStatus == OrderStatus.PARTIAL_FILLED;
                break;
            case REJECTED:
                valid = currentStatus == OrderStatus.SUBMITTED || currentStatus == OrderStatus.PARTIAL_FILLED;
                break;
            default:
                valid = false;
        }
        
        if (!valid) {
            throw new OrderStateTransitionException(
                String.format("Invalid state transition from %s to %s", currentStatus, targetStatus));
        }
    }

    /**
     * Validate order can be filled
     */
    private void validateCanFill(OrderStatus status) {
        if (status != OrderStatus.SUBMITTED && status != OrderStatus.PARTIAL_FILLED) {
            throw new OrderStateTransitionException(
                String.format("Cannot fill order in status %s", status));
        }
    }

    /**
     * Validate order can be cancelled
     */
    private void validateCanCancel(OrderStatus status) {
        if (status != OrderStatus.SUBMITTED && status != OrderStatus.PARTIAL_FILLED) {
            throw new OrderStateTransitionException(
                String.format("Cannot cancel order in status %s", status));
        }
    }

    /**
     * Validate order can be rejected
     */
    private void validateCanReject(OrderStatus status) {
        if (status != OrderStatus.SUBMITTED && status != OrderStatus.PARTIAL_FILLED) {
            throw new OrderStateTransitionException(
                String.format("Cannot reject order in status %s", status));
        }
    }

    /**
     * Log state transition
     */
    private void logStateTransition(String orderId, OrderStatus fromStatus, 
                                    OrderStatus toStatus, String event, String reason) {
        OrderStateLog log = new OrderStateLog();
        log.setOrderId(orderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setTriggerEvent(event);
        log.setReason(reason);
        stateLogRepository.save(log);
    }

    /**
     * Check if order is in terminal state
     */
    public boolean isTerminalState(OrderStatus status) {
        return status == OrderStatus.FILLED || 
               status == OrderStatus.CANCELLED || 
               status == OrderStatus.REJECTED;
    }

    /**
     * Check if order can be modified
     */
    public boolean canModify(OrderStatus status) {
        return status == OrderStatus.CREATED || 
               status == OrderStatus.SUBMITTED || 
               status == OrderStatus.PARTIAL_FILLED;
    }

    /**
     * Check if order can be cancelled
     */
    public boolean canCancel(OrderStatus status) {
        return status == OrderStatus.SUBMITTED || status == OrderStatus.PARTIAL_FILLED;
    }
}
