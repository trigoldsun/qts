package com.qts.biz.trade.statemachine;

import com.qts.biz.trade.BaseTest;
import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.entity.OrderStateLog;
import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.enums.OrderType;
import com.qts.biz.trade.event.OrderEventPublisher;
import com.qts.biz.trade.exception.OrderStateTransitionException;
import com.qts.biz.trade.repository.OrderRepository;
import com.qts.biz.trade.repository.OrderStateLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderStateMachine
 * Tests order state transitions and validation
 */
public class OrderStateMachineTest extends BaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateLogRepository stateLogRepository;

    @Mock
    private OrderEventPublisher eventPublisher;

    private OrderStateMachine stateMachine;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        stateMachine = new OrderStateMachine(orderRepository, stateLogRepository, eventPublisher);
    }

    @Test
    @DisplayName("Submit should transition from CREATED to SUBMITTED")
    void testSubmit_FromCreatedToSubmitted() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.CREATED);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderEntity result = stateMachine.submit(order);

        // Then
        assertEquals(OrderStatus.SUBMITTED, result.getStatus());
        assertNotNull(result.getSubmittedAt());
        
        verify(eventPublisher).publishOrderSubmittedEvent(any(OrderEntity.class));
        verify(stateLogRepository).save(any(OrderStateLog.class));
    }

    @Test
    @DisplayName("Submit should fail from SUBMITTED state")
    void testSubmit_FromSubmitted_ShouldFail() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.SUBMITTED);

        // When / Then
        assertThrows(OrderStateTransitionException.class, () -> stateMachine.submit(order));
    }

    @Test
    @DisplayName("Partial fill should transition from SUBMITTED to PARTIAL_FILLED")
    void testPartialFill_FromSubmittedToPartialFilled() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.SUBMITTED);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderEntity result = stateMachine.partialFill(order, 50);

        // Then
        assertEquals(OrderStatus.PARTIAL_FILLED, result.getStatus());
        assertEquals(50, result.getFilledQuantity());
        
        verify(eventPublisher).publishOrderPartialFilledEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Fill should transition from PARTIAL_FILLED to FILLED")
    void testFill_FromPartialFilledToFilled() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.PARTIAL_FILLED);
        order.setFilledQuantity(50);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderEntity result = stateMachine.fill(order, 100, 10.5);

        // Then
        assertEquals(OrderStatus.FILLED, result.getStatus());
        assertEquals(100, result.getFilledQuantity());
        assertEquals(BigDecimal.valueOf(10.5), result.getAvgPrice());
        assertNotNull(result.getFilledAt());
        
        verify(eventPublisher).publishOrderFilledEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Fill should transition from SUBMITTED to FILLED (direct)")
    void testFill_FromSubmittedToFilled() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.SUBMITTED);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderEntity result = stateMachine.fill(order, 100, 10.0);

        // Then
        assertEquals(OrderStatus.FILLED, result.getStatus());
        assertEquals(100, result.getFilledQuantity());
        
        verify(eventPublisher).publishOrderFilledEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Cancel should transition from SUBMITTED to CANCELLED")
    void testCancel_FromSubmitted() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.SUBMITTED);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderEntity result = stateMachine.cancel(order);

        // Then
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
        
        verify(eventPublisher).publishOrderCancelledEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Cancel should transition from PARTIAL_FILLED to CANCELLED")
    void testCancel_FromPartialFilled() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.PARTIAL_FILLED);
        order.setFilledQuantity(50);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        OrderEntity result = stateMachine.cancel(order);

        // Then
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        
        verify(eventPublisher).publishOrderCancelledEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Cancel should fail from CREATED state")
    void testCancel_FromCreated_ShouldFail() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.CREATED);

        // When / Then
        assertThrows(OrderStateTransitionException.class, () -> stateMachine.cancel(order));
    }

    @Test
    @DisplayName("Cancel should fail from FILLED state")
    void testCancel_FromFilled_ShouldFail() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.FILLED);

        // When / Then
        assertThrows(OrderStateTransitionException.class, () -> stateMachine.cancel(order));
    }

    @Test
    @DisplayName("Reject should transition from SUBMITTED to REJECTED")
    void testReject_FromSubmitted() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.SUBMITTED);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stateLogRepository.save(any(OrderStateLog.class))).thenAnswer(inv -> inv.getArgument(0));

        String reason = "Risk limit exceeded";

        // When
        OrderEntity result = stateMachine.reject(order, reason);

        // Then
        assertEquals(OrderStatus.REJECTED, result.getStatus());
        assertEquals("RISK_REJECT", result.getRejectCode());
        assertEquals(reason, result.getRejectReason());
        
        verify(eventPublisher).publishOrderRejectedEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Reject should fail from CREATED state")
    void testReject_FromCreated_ShouldFail() {
        // Given
        OrderEntity order = createTestOrder(OrderStatus.CREATED);

        // When / Then
        assertThrows(OrderStateTransitionException.class, 
                () -> stateMachine.reject(order, "some reason"));
    }

    @Test
    @DisplayName("isTerminalState should return true for FILLED")
    void testIsTerminalState_Filled() {
        assertTrue(stateMachine.isTerminalState(OrderStatus.FILLED));
    }

    @Test
    @DisplayName("isTerminalState should return true for CANCELLED")
    void testIsTerminalState_Cancelled() {
        assertTrue(stateMachine.isTerminalState(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("isTerminalState should return true for REJECTED")
    void testIsTerminalState_Rejected() {
        assertTrue(stateMachine.isTerminalState(OrderStatus.REJECTED));
    }

    @Test
    @DisplayName("isTerminalState should return false for non-terminal states")
    void testIsTerminalState_NonTerminal() {
        assertFalse(stateMachine.isTerminalState(OrderStatus.CREATED));
        assertFalse(stateMachine.isTerminalState(OrderStatus.SUBMITTED));
        assertFalse(stateMachine.isTerminalState(OrderStatus.PARTIAL_FILLED));
    }

    @Test
    @DisplayName("canModify should return true for modifiable states")
    void testCanModify() {
        assertTrue(stateMachine.canModify(OrderStatus.CREATED));
        assertTrue(stateMachine.canModify(OrderStatus.SUBMITTED));
        assertTrue(stateMachine.canModify(OrderStatus.PARTIAL_FILLED));
    }

    @Test
    @DisplayName("canModify should return false for terminal states")
    void testCanModify_TerminalStates() {
        assertFalse(stateMachine.canModify(OrderStatus.FILLED));
        assertFalse(stateMachine.canModify(OrderStatus.CANCELLED));
        assertFalse(stateMachine.canModify(OrderStatus.REJECTED));
    }

    @Test
    @DisplayName("canCancel should return true for SUBMITTED and PARTIAL_FILLED")
    void testCanCancel() {
        assertTrue(stateMachine.canCancel(OrderStatus.SUBMITTED));
        assertTrue(stateMachine.canCancel(OrderStatus.PARTIAL_FILLED));
        assertFalse(stateMachine.canCancel(OrderStatus.CREATED));
        assertFalse(stateMachine.canCancel(OrderStatus.FILLED));
        assertFalse(stateMachine.canCancel(OrderStatus.CANCELLED));
        assertFalse(stateMachine.canCancel(OrderStatus.REJECTED));
    }

    // ========== Helper Methods ==========

    private OrderEntity createTestOrder(OrderStatus status) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId("ORDER123");
        entity.setClientOrderId("CLIENT123");
        entity.setAccountId("ACC001");
        entity.setSymbol("600000");
        entity.setSide(OrderSide.BUY);
        entity.setOrderType(OrderType.LIMIT);
        entity.setPrice(BigDecimal.valueOf(10.00));
        entity.setQuantity(100);
        entity.setFilledQuantity(0);
        entity.setStatus(status);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVersion(1);
        return entity;
    }
}
