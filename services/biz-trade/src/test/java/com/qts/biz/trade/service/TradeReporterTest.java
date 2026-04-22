package com.qts.biz.trade.service;

import com.qts.biz.trade.dto.OrderReport;
import com.qts.biz.trade.dto.TradeDTO;
import com.qts.biz.trade.dto.TradeReport;
import com.qts.biz.trade.entity.OrderEntity;
import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.enums.OrderType;
import com.qts.biz.trade.repository.OrderRepository;
import com.qts.biz.trade.statemachine.OrderStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TradeReporter
 */
@ExtendWith(MockitoExtension.class)
class TradeReporterTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStateMachine orderStateMachine;

    @Mock
    private PositionManager positionManager;

    @Mock
    private AssetManager assetManager;

    @Mock
    private KafkaTemplate<String, com.qts.biz.trade.event.TradeExecutedEvent> kafkaTemplate;

    private TradeReporter tradeReporter;

    @BeforeEach
    void setUp() {
        tradeReporter = new TradeReporter(
                orderRepository,
                orderStateMachine,
                positionManager,
                assetManager,
                kafkaTemplate
        );
    }

    @Test
    void testProcessTradeReport_BuyTrade_Success() {
        // Given
        TradeReport tradeReport = createTradeReport("TRADE001", "ORDER001", "BUY", 100, 10.0);
        OrderEntity order = createOrderEntity("ORDER001", OrderStatus.SUBMITTED);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        
        // When
        tradeReporter.processTradeReport(tradeReport);
        
        // Then
        verify(orderStateMachine).fill(eq(order), eq(100), any(Double.class));
        verify(positionManager).addPosition(any(TradeDTO.class));
        verify(assetManager).updateAssetFromTrade(any(TradeDTO.class));
        verify(kafkaTemplate).send(any(String.class), eq("TRADE001"), any(com.qts.biz.trade.event.TradeExecutedEvent.class));
    }

    @Test
    void testProcessTradeReport_SellTrade_Success() {
        // Given
        TradeReport tradeReport = createTradeReport("TRADE001", "ORDER001", "SELL", 100, 10.0);
        OrderEntity order = createOrderEntity("ORDER001", OrderStatus.SUBMITTED);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        
        // When
        tradeReporter.processTradeReport(tradeReport);
        
        // Then
        verify(orderStateMachine).fill(eq(order), eq(100), any(Double.class));
        verify(positionManager).reducePosition(any(TradeDTO.class));
        verify(assetManager).updateAssetFromTrade(any(TradeDTO.class));
    }

    @Test
    void testProcessTradeReport_PartialFill() {
        // Given
        TradeReport tradeReport = createTradeReport("TRADE001", "ORDER001", "BUY", 50, 10.0);
        OrderEntity order = createOrderEntity("ORDER001", OrderStatus.SUBMITTED);
        order.setQuantity(100);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        
        // When
        tradeReporter.processTradeReport(tradeReport);
        
        // Then
        verify(orderStateMachine).partialFill(eq(order), eq(50));
        verify(positionManager).addPosition(any(TradeDTO.class));
    }

    @Test
    void testProcessTradeReport_OrderNotFound() {
        // Given
        TradeReport tradeReport = createTradeReport("TRADE001", "ORDER001", "BUY", 100, 10.0);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.empty());
        when(orderRepository.findByClientOrderId("ORDER001")).thenReturn(Optional.empty());
        
        // When
        tradeReporter.processTradeReport(tradeReport);
        
        // Then
        verify(orderStateMachine, never()).fill(any(), anyInt(), any(Double.class));
        verify(positionManager, never()).addPosition(any());
    }

    @Test
    void testProcessTradeReport_InvalidTradeReport() {
        // Given
        TradeReport tradeReport = new TradeReport();
        tradeReport.setTradeId(null); // Invalid - missing tradeId
        
        // When
        tradeReporter.processTradeReport(tradeReport);
        
        // Then
        verify(orderStateMachine, never()).fill(any(), anyInt(), any(Double.class));
    }

    @Test
    void testProcessOrderReport_Filled() {
        // Given
        OrderReport orderReport = new OrderReport();
        orderReport.setOrderId("ORDER001");
        orderReport.setStatus(OrderStatus.FILLED);
        orderReport.setFilledQuantity(100);
        orderReport.setAvgPrice(10.0);
        
        OrderEntity order = createOrderEntity("ORDER001", OrderStatus.PARTIAL_FILLED);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        
        // When
        tradeReporter.processOrderReport(orderReport);
        
        // Then
        verify(orderStateMachine).fill(eq(order), eq(100), eq(10.0));
    }

    @Test
    void testProcessOrderReport_Cancelled() {
        // Given
        OrderReport orderReport = new OrderReport();
        orderReport.setOrderId("ORDER001");
        orderReport.setStatus(OrderStatus.CANCELLED);
        
        OrderEntity order = createOrderEntity("ORDER001", OrderStatus.SUBMITTED);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        
        // When
        tradeReporter.processOrderReport(orderReport);
        
        // Then
        verify(orderStateMachine).cancel(order);
    }

    @Test
    void testProcessOrderReport_Rejected() {
        // Given
        OrderReport orderReport = new OrderReport();
        orderReport.setOrderId("ORDER001");
        orderReport.setStatus(OrderStatus.REJECTED);
        orderReport.setErrorMessage("Insufficient margin");
        
        OrderEntity order = createOrderEntity("ORDER001", OrderStatus.SUBMITTED);
        
        when(orderRepository.findByOrderId("ORDER001")).thenReturn(Optional.of(order));
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(order);
        
        // When
        tradeReporter.processOrderReport(orderReport);
        
        // Then
        verify(orderStateMachine).reject(order, "Insufficient margin");
    }

    private TradeReport createTradeReport(String tradeId, String orderId, String side, 
                                          int quantity, double price) {
        TradeReport report = new TradeReport();
        report.setTradeId(tradeId);
        report.setOrderId(orderId);
        report.setAccountId("12345");
        report.setSymbol("600000");
        report.setSide(side);
        report.setQuantity(BigDecimal.valueOf(quantity));
        report.setPrice(BigDecimal.valueOf(price));
        report.setAmount(BigDecimal.valueOf(quantity * price));
        report.setTradeTime(LocalDateTime.now());
        report.setTradeType("NORMAL");
        return report;
    }

    private OrderEntity createOrderEntity(String orderId, OrderStatus status) {
        OrderEntity entity = new OrderEntity();
        entity.setOrderId(orderId);
        entity.setClientOrderId("CLI" + orderId);
        entity.setAccountId("12345");
        entity.setSymbol("600000");
        entity.setSide(OrderSide.BUY);
        entity.setOrderType(OrderType.LIMIT);
        entity.setQuantity(100);
        entity.setFilledQuantity(0);
        entity.setStatus(status);
        return entity;
    }
}
