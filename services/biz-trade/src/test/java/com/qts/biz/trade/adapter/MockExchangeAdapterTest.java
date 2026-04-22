package com.qts.biz.trade.adapter;

import com.qts.biz.trade.dto.Order;
import com.qts.biz.trade.dto.OrderCancelResult;
import com.qts.biz.trade.dto.OrderModifyResult;
import com.qts.biz.trade.dto.OrderReport;
import com.qts.biz.trade.dto.OrderSendResult;
import com.qts.biz.trade.dto.TradeReport;
import com.qts.biz.trade.enums.OrderSide;
import com.qts.biz.trade.enums.OrderStatus;
import com.qts.biz.trade.enums.OrderType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MockExchangeAdapter
 */
class MockExchangeAdapterTest {

    private MockExchangeAdapter adapter;
    private CountDownLatch orderReportLatch;
    private CountDownLatch tradeReportLatch;
    private AtomicReference<OrderReport> capturedOrderReport;
    private AtomicReference<TradeReport> capturedTradeReport;

    @BeforeEach
    void setUp() {
        adapter = new MockExchangeAdapter();
        adapter.connect();
        
        orderReportLatch = new CountDownLatch(1);
        tradeReportLatch = new CountDownLatch(1);
        capturedOrderReport = new AtomicReference<>();
        capturedTradeReport = new AtomicReference<>();
        
        adapter.setOrderReportCallback(report -> {
            capturedOrderReport.set(report);
            orderReportLatch.countDown();
        });
        
        adapter.setTradeReportCallback(trade -> {
            capturedTradeReport.set(trade);
            tradeReportLatch.countDown();
        });
    }

    @AfterEach
    void tearDown() {
        adapter.disconnect();
        adapter.shutdown();
    }

    @Test
    void testSendOrder_Success() {
        Order order = createTestOrder();
        
        OrderSendResult result = adapter.sendOrder(order);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("MOCK"));
    }

    @Test
    void testSendOrder_NotConnected() {
        adapter.disconnect();
        
        Order order = createTestOrder();
        OrderSendResult result = adapter.sendOrder(order);
        
        assertFalse(result.isSuccess());
        assertEquals("NOT_CONNECTED", result.getErrorCode());
    }

    @Test
    void testCancelOrder_Success() throws Exception {
        Order order = createTestOrder();
        OrderSendResult sendResult = adapter.sendOrder(order);
        String exchangeOrderId = sendResult.getOrderId();
        
        // Wait for order acceptance
        orderReportLatch.await(500, TimeUnit.MILLISECONDS);
        
        CountDownLatch cancelLatch = new CountDownLatch(1);
        adapter.setOrderReportCallback(report -> {
            if (report.getStatus() == OrderStatus.CANCELLED) {
                cancelLatch.countDown();
            }
        });
        
        OrderCancelResult cancelResult = adapter.cancelOrder(exchangeOrderId);
        
        assertTrue(cancelResult.isSuccess());
        assertTrue(cancelLatch.await(500, TimeUnit.MILLISECONDS));
    }

    @Test
    void testModifyOrder_Success() {
        Order order = createTestOrder();
        OrderSendResult sendResult = adapter.sendOrder(order);
        
        OrderModifyResult modifyResult = adapter.modifyOrder(
                sendResult.getOrderId(), 
                BigDecimal.valueOf(105.0), 
                200
        );
        
        assertTrue(modifyResult.isSuccess());
    }

    @Test
    void testTradeExecution_WithDelay() throws Exception {
        adapter.setSimulatePartialFills(false);
        
        Order order = createTestOrder();
        OrderSendResult sendResult = adapter.sendOrder(order);
        
        // Wait for trade execution (1 second delay + buffer)
        boolean tradeReceived = tradeReportLatch.await(3, TimeUnit.SECONDS);
        
        assertTrue(tradeReceived, "Trade report should be received within timeout");
        
        TradeReport trade = capturedTradeReport.get();
        assertNotNull(trade);
        assertEquals(order.getSymbol(), trade.getSymbol());
        assertEquals(order.getSide().name(), trade.getSide());
        assertEquals(order.getQuantity().intValue(), trade.getQuantity().intValue());
        assertEquals(BigDecimal.valueOf(100.0), trade.getPrice());
    }

    @Test
    void testPartialFill() throws Exception {
        adapter.setSimulatePartialFills(true);
        adapter.setPartialFillProbability(1.0); // Always partial fill
        
        Order order = createTestOrder();
        OrderSendResult sendResult = adapter.sendOrder(order);
        
        // Should receive multiple trade reports for partial fills
        CountDownLatch multiTradeLatch = new CountDownLatch(2);
        adapter.setTradeReportCallback(trade -> {
            multiTradeLatch.countDown();
        });
        
        boolean tradesReceived = multiTradeLatch.await(5, TimeUnit.SECONDS);
        
        assertTrue(tradesReceived, "Should receive multiple trade reports for partial fill");
    }

    @Test
    void testOrderFlow_FullFill() throws Exception {
        adapter.setSimulatePartialFills(false);
        
        Order order = createTestOrder();
        OrderSendResult sendResult = adapter.sendOrder(order);
        
        // Wait for both order acceptance and trade execution
        CountDownLatch bothReports = new CountDownLatch(2);
        adapter.setOrderReportCallback(report -> {
            if (report.getStatus() == OrderStatus.FILLED) {
                bothReports.countDown();
            }
        });
        adapter.setTradeReportCallback(trade -> {
            bothReports.countDown();
        });
        
        boolean completed = bothReports.await(5, TimeUnit.SECONDS);
        
        assertTrue(completed);
    }

    private Order createTestOrder() {
        Order order = new Order();
        order.setClientOrderId("CLI" + System.currentTimeMillis());
        order.setAccountId("12345");
        order.setSymbol("600000");
        order.setSide(OrderSide.BUY);
        order.setOrderType(OrderType.LIMIT);
        order.setQuantity(100);
        order.setPrice(BigDecimal.valueOf(100.0));
        return order;
    }
}
