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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CTPExchangeAdapter
 */
class CTPExchangeAdapterTest {

    private CTPExchangeAdapter adapter;
    private AtomicReference<OrderReport> capturedOrderReport;
    private AtomicReference<TradeReport> capturedTradeReport;

    @BeforeEach
    void setUp() {
        adapter = new CTPExchangeAdapter();
        
        capturedOrderReport = new AtomicReference<>();
        capturedTradeReport = new AtomicReference<>();
        
        adapter.setOrderReportCallback(capturedOrderReport::set);
        adapter.setTradeReportCallback(capturedTradeReport::set);
    }

    @Test
    void testConnect() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        assertTrue(adapter.isConnected());
    }

    @Test
    void testDisconnect() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        adapter.disconnect();
        
        assertFalse(adapter.isConnected());
    }

    @Test
    void testLogin() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        boolean loggedIn = adapter.login();
        
        assertTrue(loggedIn);
        assertTrue(adapter.isConnected());
    }

    @Test
    void testLogout() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        adapter.login();
        adapter.logout();
        
        assertFalse(adapter.isConnected());
    }

    @Test
    void testSendOrder_NotConnected() {
        // Not connected - should fail
        Order order = createTestOrder();
        OrderSendResult result = adapter.sendOrder(order);
        
        assertFalse(result.isSuccess());
        assertEquals("NOT_CONNECTED", result.getErrorCode());
    }

    @Test
    void testSendOrder_Connected() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        Order order = createTestOrder();
        OrderSendResult result = adapter.sendOrder(order);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getOrderId());
        assertTrue(result.getOrderId().startsWith("CTP"));
    }

    @Test
    void testModifyOrder_NotConnected() {
        OrderModifyResult result = adapter.modifyOrder("ORDER123", BigDecimal.valueOf(105.0), 200);
        
        assertFalse(result.isSuccess());
        assertEquals("NOT_CONNECTED", result.getErrorCode());
    }

    @Test
    void testModifyOrder_Connected() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        OrderModifyResult result = adapter.modifyOrder("ORDER123", BigDecimal.valueOf(105.0), 200);
        
        assertTrue(result.isSuccess());
    }

    @Test
    void testCancelOrder_NotConnected() {
        OrderCancelResult result = adapter.cancelOrder("ORDER123");
        
        assertFalse(result.isSuccess());
        assertEquals("NOT_CONNECTED", result.getErrorCode());
    }

    @Test
    void testCancelOrder_Connected() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        OrderCancelResult result = adapter.cancelOrder("ORDER123");
        
        assertTrue(result.isSuccess());
    }

    @Test
    void testOnTradeReport() {
        TradeReport trade = new TradeReport();
        trade.setTradeId("TRADE001");
        trade.setOrderId("ORDER001");
        trade.setSymbol("600000");
        trade.setSide("BUY");
        trade.setQuantity(BigDecimal.valueOf(100));
        trade.setPrice(BigDecimal.valueOf(10.0));
        
        adapter.onTradeReport(trade);
        
        TradeReport captured = capturedTradeReport.get();
        assertNotNull(captured);
        assertEquals("TRADE001", captured.getTradeId());
    }

    @Test
    void testOnOrderReport() {
        OrderReport report = new OrderReport();
        report.setOrderId("ORDER001");
        report.setStatus(OrderStatus.SUBMITTED);
        
        adapter.onOrderReport(report);
        
        OrderReport captured = capturedOrderReport.get();
        assertNotNull(captured);
        assertEquals("ORDER001", captured.getOrderId());
        assertEquals(OrderStatus.SUBMITTED, captured.getStatus());
    }

    @Test
    void testOnRspOrderInsert_Accepted() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        adapter.onRspOrderInsert("CTP001", 0, "");
        
        OrderReport captured = capturedOrderReport.get();
        assertNotNull(captured);
        assertEquals(OrderStatus.SUBMITTED, captured.getStatus());
    }

    @Test
    void testOnRspOrderInsert_Rejected() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        adapter.onRspOrderInsert("CTP001", -1, "Insufficient margin");
        
        OrderReport captured = capturedOrderReport.get();
        assertNotNull(captured);
        assertEquals(OrderStatus.REJECTED, captured.getStatus());
        assertEquals("-1", captured.getErrorCode());
    }

    @Test
    void testOnRtnTrade() {
        adapter.connect("tcp://127.0.0.1:41205", "9999", "testuser", "testpass");
        
        adapter.onRtnTrade("CTP001", "TRADE001", "600000", "BUY", 
                BigDecimal.valueOf(100), BigDecimal.valueOf(10.0), 
                java.time.LocalDateTime.now());
        
        TradeReport captured = capturedTradeReport.get();
        assertNotNull(captured);
        assertEquals("TRADE001", captured.getTradeId());
        assertEquals("CTP001", captured.getExchangeOrderId());
        assertEquals("600000", captured.getSymbol());
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
