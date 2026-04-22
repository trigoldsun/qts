package com.qts.biz.trade.service;

import com.qts.biz.trade.BaseTest;
import com.qts.biz.trade.client.RiskCheckClient;
import com.qts.biz.trade.dto.*;
import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.enums.OrderType;
import com.qts.biz.trade.event.OrderEventPublisher;
import com.qts.biz.trade.exception.BusinessException;
import com.qts.biz.trade.repository.OrderRepository;
import com.qts.biz.trade.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderCommandService
 * Tests order command handling with mocked dependencies
 */
public class OrderCommandServiceTest extends BaseTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine stateMachine;

    @Mock
    private OrderEventPublisher eventPublisher;

    @Mock
    private RiskCheckClient riskCheckClient;

    private OrderCommandService orderCommandService;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        orderCommandService = new OrderCommandService(
                orderRepository, stateMachine, eventPublisher, riskCheckClient);
    }

    @Test
    @DisplayName("Place order should succeed with valid parameters")
    void testPlaceOrder_Success() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        
        when(orderRepository.existsByClientOrderIdAndAccountId(anyString(), anyString())).thenReturn(false);
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            entity.setVersion(1);
            return entity;
        });
        
        RiskCheckClient.RiskCheckResult riskResult = new RiskCheckClient.RiskCheckResult();
        riskResult.setPassed(true);
        when(riskCheckClient.checkRisk(anyLong(), anyString(), anyString(), anyDouble(), anyDouble()))
                .thenReturn(riskResult);

        // When
        OrderDTO result = orderCommandService.placeOrder(cmd);

        // Then
        assertNotNull(result);
        assertNotNull(result.getOrderId());
        assertEquals(cmd.getClientOrderId(), result.getClientOrderId());
        assertEquals(cmd.getAccountId(), result.getAccountId());
        assertEquals(cmd.getSymbol(), result.getSymbol());
        assertEquals(cmd.getSide(), result.getSide());
        assertEquals(cmd.getOrderType(), result.getOrderType());
        assertEquals(cmd.getQuantity(), result.getQuantity());
        assertEquals(OrderStatus.CREATED, result.getStatus());
        
        verify(orderRepository).save(any(OrderEntity.class));
        verify(eventPublisher).publishOrderCreatedEvent(any(OrderEntity.class));
    }

    @Test
    @DisplayName("Place order should reject invalid quantity (not multiple of 100)")
    void testPlaceOrder_InvalidQuantity_NotMultipleOf100() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        cmd.setQuantity(150); // Not multiple of 100

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.placeOrder(cmd));
        
        assertEquals(1006, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("multiple of 100"));
    }

    @Test
    @DisplayName("Place order should reject invalid symbol (not 6 digits)")
    void testPlaceOrder_InvalidSymbol() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        cmd.setSymbol("ABC123"); // Not 6 digit number

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.placeOrder(cmd));
        
        assertEquals(1005, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("6 digit number"));
    }

    @Test
    @DisplayName("Place limit order should reject null price")
    void testPlaceOrder_LimitOrder_NullPrice() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        cmd.setOrderType(OrderType.LIMIT);
        cmd.setPrice(null);

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.placeOrder(cmd));
        
        assertEquals(1006, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Price must be greater than 0"));
    }

    @Test
    @DisplayName("Place stop order should reject null stop_price")
    void testPlaceOrder_StopOrder_NullStopPrice() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        cmd.setOrderType(OrderType.STOP);
        cmd.setStopPrice(null);

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.placeOrder(cmd));
        
        assertEquals(1006, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Stop price must be greater than 0"));
    }

    @Test
    @DisplayName("Place order should reject duplicate client_order_id")
    void testPlaceOrder_DuplicateOrder() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        
        when(orderRepository.existsByClientOrderIdAndAccountId(cmd.getClientOrderId(), cmd.getAccountId()))
                .thenReturn(true);

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.placeOrder(cmd));
        
        assertEquals(1007, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Duplicate order"));
    }

    @Test
    @DisplayName("Place order should reject when risk precheck fails")
    void testPlaceOrder_RiskPrecheckFailed() {
        // Given
        PlaceOrderCmd cmd = createValidPlaceOrderCmd();
        
        when(orderRepository.existsByClientOrderIdAndAccountId(anyString(), anyString())).thenReturn(false);
        
        RiskCheckClient.RiskCheckResult riskResult = new RiskCheckClient.RiskCheckResult();
        riskResult.setPassed(false);
        riskResult.setMessage("Position limit exceeded");
        when(riskCheckClient.checkRisk(anyLong(), anyString(), anyString(), anyDouble(), anyDouble()))
                .thenReturn(riskResult);

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.placeOrder(cmd));
        
        assertEquals(1004, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Risk precheck failed"));
    }

    @Test
    @DisplayName("Modify order should succeed for valid order")
    void testModifyOrder_Success() {
        // Given
        String orderId = "ORDER123";
        ModifyOrderCmd cmd = new ModifyOrderCmd();
        cmd.setOrderId(orderId);
        cmd.setAccountId("ACC001");
        cmd.setNewPrice(new BigDecimal("50.00"));
        cmd.setNewQuantity(300);
        cmd.setModifyVersion(1);

        OrderEntity existingOrder = createTestOrderEntity(orderId);
        existingOrder.setVersion(1);
        existingOrder.setStatus(OrderStatus.SUBMITTED);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        OrderDTO result = orderCommandService.modifyOrder(cmd);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("50.00"), result.getPrice());
        assertEquals(300, result.getQuantity());
    }

    @Test
    @DisplayName("Modify order should fail for non-existent order")
    void testModifyOrder_OrderNotFound() {
        // Given
        String orderId = "ORDER123";
        ModifyOrderCmd cmd = new ModifyOrderCmd();
        cmd.setOrderId(orderId);
        cmd.setAccountId("ACC001");
        cmd.setModifyVersion(1);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.empty());

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.modifyOrder(cmd));
        
        assertEquals(1008, exception.getErrorCode());
    }

    @Test
    @DisplayName("Modify order should fail for version conflict")
    void testModifyOrder_VersionConflict() {
        // Given
        String orderId = "ORDER123";
        ModifyOrderCmd cmd = new ModifyOrderCmd();
        cmd.setOrderId(orderId);
        cmd.setAccountId("ACC001");
        cmd.setModifyVersion(2); // Wrong version

        OrderEntity existingOrder = createTestOrderEntity(orderId);
        existingOrder.setVersion(1); // Current version is 1

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.modifyOrder(cmd));
        
        assertEquals(1011, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Version conflict"));
    }

    @Test
    @DisplayName("Cancel order should succeed for submitted order")
    void testCancelOrder_Success() {
        // Given
        String orderId = "ORDER123";
        CancelOrderCmd cmd = new CancelOrderCmd();
        cmd.setOrderId(orderId);
        cmd.setAccountId("ACC001");

        OrderEntity existingOrder = createTestOrderEntity(orderId);
        existingOrder.setStatus(OrderStatus.SUBMITTED);

        OrderEntity cancelledOrder = createTestOrderEntity(orderId);
        cancelledOrder.setStatus(OrderStatus.CANCELLED);
        cancelledOrder.setCancelledAt(LocalDateTime.now());

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        when(stateMachine.canCancel(OrderStatus.SUBMITTED)).thenReturn(true);
        when(stateMachine.cancel(any(OrderEntity.class))).thenReturn(cancelledOrder);

        // When
        CancelResult result = orderCommandService.cancelOrder(cmd);

        // Then
        assertNotNull(result);
        assertEquals(orderId, result.getOrderId());
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertNotNull(result.getCancelledAt());
    }

    @Test
    @DisplayName("Cancel order should fail for filled order")
    void testCancelOrder_OrderAlreadyFilled() {
        // Given
        String orderId = "ORDER123";
        CancelOrderCmd cmd = new CancelOrderCmd();
        cmd.setOrderId(orderId);
        cmd.setAccountId("ACC001");

        OrderEntity existingOrder = createTestOrderEntity(orderId);
        existingOrder.setStatus(OrderStatus.FILLED);

        when(orderRepository.findByOrderId(orderId)).thenReturn(Optional.of(existingOrder));
        when(stateMachine.canCancel(OrderStatus.FILLED)).thenReturn(false);

        // When / Then
        BusinessException exception = assertThrows(BusinessException.class, 
                () -> orderCommandService.cancelOrder(cmd));
        
        assertEquals(1010, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("cannot be cancelled"));
    }

    // ========== Helper Methods ==========

    private PlaceOrderCmd createValidPlaceOrderCmd() {
        PlaceOrderCmd cmd = new PlaceOrderCmd();
        cmd.setAccountId("ACC001");
        cmd.setSymbol("600000");
        cmd.setSide(OrderSide.BUY);
        cmd.setOrderType(OrderType.LIMIT);
        cmd.setQuantity(100);
        cmd.setPrice(new BigDecimal("10.00"));
        cmd.setClientOrderId("CLIENT123");
        cmd.setStrategyId("STRAT001");
        return cmd;
    }

    private OrderEntity createTestOrderEntity(String orderId) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(orderId);
        entity.setClientOrderId("CLIENT123");
        entity.setAccountId("ACC001");
        entity.setSymbol("600000");
        entity.setSide(OrderSide.BUY);
        entity.setOrderType(OrderType.LIMIT);
        entity.setPrice(new BigDecimal("10.00"));
        entity.setQuantity(100);
        entity.setFilledQuantity(0);
        entity.setStatus(OrderStatus.CREATED);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setVersion(1);
        return entity;
    }
}
