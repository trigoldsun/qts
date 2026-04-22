package com.qts.biz.trade.adapter;

import com.qts.biz.trade.dto.Order;
import com.qts.biz.trade.dto.OrderCancelResult;
import com.qts.biz.trade.dto.OrderModifyResult;
import com.qts.biz.trade.dto.OrderReport;
import com.qts.biz.trade.dto.OrderSendResult;
import com.qts.biz.trade.dto.TradeReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Mock Exchange Adapter
 * 用于测试/MVP阶段
 * 模拟订单报送、成交回报（延迟1秒触发）、支持随机部分成交
 */
@Component
public class MockExchangeAdapter implements ExchangeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MockExchangeAdapter.class);

    private final Random random = new Random();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Order state tracking
    private final Map<String, Order> pendingOrders = new ConcurrentHashMap<>();
    private final Map<String, String> clientToExchangeOrderId = new ConcurrentHashMap<>();
    
    // Trade report callback
    private TradeReportCallback tradeReportCallback;
    
    // Order report callback
    private OrderReportCallback orderReportCallback;
    
    // Simulate partial fills
    private boolean simulatePartialFills = true;
    private double partialFillProbability = 0.3;
    
    // Connection state
    private volatile boolean connected = false;

    @Override
    public OrderSendResult sendOrder(Order order) {
        logger.info("MockExchange sending order: clientOrderId={}, symbol={}, side={}, qty={}, price={}",
                order.getClientOrderId(), order.getSymbol(), order.getSide(), order.getQuantity(), order.getPrice());
        
        if (!connected) {
            return OrderSendResult.failure("NOT_CONNECTED", "Exchange not connected");
        }
        
        try {
            // Generate exchange order ID
            String exchangeOrderId = "MOCK" + System.currentTimeMillis();
            clientToExchangeOrderId.put(order.getClientOrderId(), exchangeOrderId);
            pendingOrders.put(exchangeOrderId, order);
            
            // Simulate network delay and order acceptance
            scheduler.schedule(() -> {
                OrderReport report = new OrderReport();
                report.setOrderId(order.getClientOrderId());
                report.setExchangeOrderId(exchangeOrderId);
                report.setAccountId(order.getAccountId());
                report.setSymbol(order.getSymbol());
                report.setStatus(com.qts.biz.trade.enums.OrderStatus.SUBMITTED);
                report.setFilledQuantity(0);
                report.setReportTime(LocalDateTime.now());
                
                logger.info("MockExchange order accepted: exchangeOrderId={}", exchangeOrderId);
                onOrderReport(report);
                
                // Schedule potential trade execution after 1 second
                scheduleTradeExecution(exchangeOrderId, order);
            }, 100, TimeUnit.MILLISECONDS);
            
            return OrderSendResult.success(exchangeOrderId);
        } catch (Exception e) {
            logger.error("MockExchange order send failed: {}", e.getMessage());
            return OrderSendResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    @Override
    public OrderModifyResult modifyOrder(String orderId, BigDecimal newPrice, Integer newQty) {
        logger.info("MockExchange modifying order: orderId={}, newPrice={}, newQty={}", orderId, newPrice, newQty);
        
        if (!connected) {
            return OrderModifyResult.failure("NOT_CONNECTED", "Exchange not connected");
        }
        
        try {
            // Simulate modification delay
            scheduler.schedule(() -> {
                OrderModifyResult result = OrderModifyResult.success(orderId);
                logger.info("MockExchange order modified: orderId={}", orderId);
            }, 50, TimeUnit.MILLISECONDS);
            
            return OrderModifyResult.success(orderId);
        } catch (Exception e) {
            logger.error("MockExchange order modify failed: {}", e.getMessage());
            return OrderModifyResult.failure("MODIFY_FAILED", e.getMessage());
        }
    }

    @Override
    public OrderCancelResult cancelOrder(String orderId) {
        logger.info("MockExchange cancelling order: orderId={}", orderId);
        
        if (!connected) {
            return OrderCancelResult.failure("NOT_CONNECTED", "Exchange not connected");
        }
        
        try {
            // Remove from pending orders
            pendingOrders.remove(orderId);
            
            // Simulate cancellation delay
            scheduler.schedule(() -> {
                OrderReport report = new OrderReport();
                report.setOrderId(orderId);
                report.setExchangeOrderId(orderId);
                report.setStatus(com.qts.biz.trade.enums.OrderStatus.CANCELLED);
                report.setReportTime(LocalDateTime.now());
                
                logger.info("MockExchange order cancelled: orderId={}", orderId);
                onOrderReport(report);
            }, 50, TimeUnit.MILLISECONDS);
            
            return OrderCancelResult.success(orderId);
        } catch (Exception e) {
            logger.error("MockExchange order cancel failed: {}", e.getMessage());
            return OrderCancelResult.failure("CANCEL_FAILED", e.getMessage());
        }
    }

    @Override
    public void onTradeReport(TradeReport trade) {
        logger.info("MockExchange processing trade report: tradeId={}, orderId={}", 
                trade.getTradeId(), trade.getOrderId());
        
        if (tradeReportCallback != null) {
            tradeReportCallback.onTradeReport(trade);
        }
    }

    @Override
    public void onOrderReport(OrderReport report) {
        logger.info("MockExchange processing order report: orderId={}, status={}", 
                report.getOrderId(), report.getStatus());
        
        if (orderReportCallback != null) {
            orderReportCallback.onOrderReport(report);
        }
    }

    /**
     * Schedule trade execution (simulated after 1 second delay)
     */
    private void scheduleTradeExecution(String exchangeOrderId, Order order) {
        scheduler.schedule(() -> {
            if (!pendingOrders.containsKey(exchangeOrderId)) {
                // Order was cancelled
                return;
            }
            
            if (simulatePartialFills && random.nextDouble() < partialFillProbability) {
                // Partial fill
                int partialQty = order.getQuantity() / 2;
                executeTrade(exchangeOrderId, order, partialQty);
                
                // Schedule remaining quantity
                scheduler.schedule(() -> {
                    if (pendingOrders.containsKey(exchangeOrderId)) {
                        executeTrade(exchangeOrderId, order, order.getQuantity() - partialQty);
                    }
                }, 1, TimeUnit.SECONDS);
            } else {
                // Full fill
                executeTrade(exchangeOrderId, order, order.getQuantity());
            }
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Execute trade and send trade report
     */
    private void executeTrade(String exchangeOrderId, Order order, int quantity) {
        BigDecimal price = order.getPrice() != null ? order.getPrice() : BigDecimal.valueOf(100.0);
        
        TradeReport trade = new TradeReport();
        trade.setTradeId("TRADE" + System.currentTimeMillis());
        trade.setOrderId(order.getClientOrderId());
        trade.setExchangeOrderId(exchangeOrderId);
        trade.setAccountId(order.getAccountId());
        trade.setSymbol(order.getSymbol());
        trade.setSide(order.getSide().name());
        trade.setPrice(price);
        trade.setQuantity(BigDecimal.valueOf(quantity));
        trade.setAmount(price.multiply(BigDecimal.valueOf(quantity)));
        trade.setTradeTime(LocalDateTime.now());
        trade.setTradeType("NORMAL");
        
        logger.info("MockExchange trade executed: tradeId={}, exchangeOrderId={}, qty={}, price={}",
                trade.getTradeId(), exchangeOrderId, quantity, price);
        
        pendingOrders.remove(exchangeOrderId);
        onTradeReport(trade);
        
        // Send final order report
        OrderReport report = new OrderReport();
        report.setOrderId(order.getClientOrderId());
        report.setExchangeOrderId(exchangeOrderId);
        report.setAccountId(order.getAccountId());
        report.setSymbol(order.getSymbol());
        report.setStatus(com.qts.biz.trade.enums.OrderStatus.FILLED);
        report.setFilledQuantity(order.getQuantity());
        report.setAvgPrice(price.doubleValue());
        report.setReportTime(LocalDateTime.now());
        
        onOrderReport(report);
    }

    /**
     * Connect to mock exchange
     */
    public void connect() {
        logger.info("MockExchange connecting...");
        connected = true;
        logger.info("MockExchange connected");
    }

    /**
     * Disconnect from mock exchange
     */
    public void disconnect() {
        logger.info("MockExchange disconnecting...");
        connected = false;
        pendingOrders.clear();
        clientToExchangeOrderId.clear();
        logger.info("MockExchange disconnected");
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * Enable/disable partial fill simulation
     */
    public void setSimulatePartialFills(boolean simulate) {
        this.simulatePartialFills = simulate;
    }

    /**
     * Set partial fill probability
     */
    public void setPartialFillProbability(double probability) {
        this.partialFillProbability = probability;
    }

    /**
     * Set trade report callback
     */
    public void setTradeReportCallback(TradeReportCallback callback) {
        this.tradeReportCallback = callback;
    }

    /**
     * Set order report callback
     */
    public void setOrderReportCallback(OrderReportCallback callback) {
        this.orderReportCallback = callback;
    }

    /**
     * Trade report callback interface
     */
    public interface TradeReportCallback {
        void onTradeReport(TradeReport trade);
    }

    /**
     * Order report callback interface
     */
    public interface OrderReportCallback {
        void onOrderReport(OrderReport report);
    }

    /**
     * Shutdown scheduler
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
