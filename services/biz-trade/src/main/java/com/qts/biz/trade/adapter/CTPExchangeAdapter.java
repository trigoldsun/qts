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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CTP Exchange Adapter
 * 对接SimNow模拟交易所（CTP协议）
 * 使用ThostTraderApi（CTP官方API）SPI回调实现
 */
@Component
public class CTPExchangeAdapter implements ExchangeAdapter {

    private static final Logger logger = LoggerFactory.getLogger(CTPExchangeAdapter.class);

    // Connection state
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private String frontAddress;
    private String brokerId;
    private String userId;
    private String password;
    
    // Order ID generator
    private final AtomicInteger orderIdGenerator = new AtomicInteger(1000);
    
    // Order reference mapping (clientOrderId -> exchangeOrderId)
    private final Map<String, String> orderRefMap = new ConcurrentHashMap<>();
    
    // Trade report callback
    private TradeReportCallback tradeReportCallback;
    
    // Order report callback
    private OrderReportCallback orderReportCallback;

    public CTPExchangeAdapter() {
        // Default SimNow configuration
        this.frontAddress = "tcp://127.0.0.1:41205";
        this.brokerId = "9999";
        this.userId = "";
        this.password = "";
    }

    @Override
    public OrderSendResult sendOrder(Order order) {
        logger.info("CTP sending order: clientOrderId={}, symbol={}, side={}, qty={}, price={}",
                order.getClientOrderId(), order.getSymbol(), order.getSide(), order.getQuantity(), order.getPrice());
        
        if (!connected.get()) {
            return OrderSendResult.failure("NOT_CONNECTED", "Exchange not connected");
        }
        
        try {
            // Generate exchange order ID
            String exchangeOrderId = generateExchangeOrderId();
            orderRefMap.put(order.getClientOrderId(), exchangeOrderId);
            
            // Simulate CTP order sending
            // In real implementation, this would call ThostTraderApi.ReqOrderInsert()
            int requestId = orderIdGenerator.incrementAndGet();
            logger.info("CTP order sent, exchangeOrderId={}, requestId={}", exchangeOrderId, requestId);
            
            return OrderSendResult.success(exchangeOrderId);
        } catch (Exception e) {
            logger.error("CTP order send failed: {}", e.getMessage());
            return OrderSendResult.failure("SEND_FAILED", e.getMessage());
        }
    }

    @Override
    public OrderModifyResult modifyOrder(String orderId, BigDecimal newPrice, Integer newQty) {
        logger.info("CTP modifying order: orderId={}, newPrice={}, newQty={}", orderId, newPrice, newQty);
        
        if (!connected.get()) {
            return OrderModifyResult.failure("NOT_CONNECTED", "Exchange not connected");
        }
        
        try {
            // Simulate CTP order modification
            // In real implementation, this would call ThostTraderApi.ReqOrderAction()
            int requestId = orderIdGenerator.incrementAndGet();
            logger.info("CTP order modified, orderId={}, requestId={}", orderId, requestId);
            
            return OrderModifyResult.success(orderId);
        } catch (Exception e) {
            logger.error("CTP order modify failed: {}", e.getMessage());
            return OrderModifyResult.failure("MODIFY_FAILED", e.getMessage());
        }
    }

    @Override
    public OrderCancelResult cancelOrder(String orderId) {
        logger.info("CTP cancelling order: orderId={}", orderId);
        
        if (!connected.get()) {
            return OrderCancelResult.failure("NOT_CONNECTED", "Exchange not connected");
        }
        
        try {
            // Simulate CTP order cancellation
            // In real implementation, this would call ThostTraderApi.ReqOrderAction()
            int requestId = orderIdGenerator.incrementAndGet();
            logger.info("CTP order cancelled, orderId={}, requestId={}", orderId, requestId);
            
            return OrderCancelResult.success(orderId);
        } catch (Exception e) {
            logger.error("CTP order cancel failed: {}", e.getMessage());
            return OrderCancelResult.failure("CANCEL_FAILED", e.getMessage());
        }
    }

    @Override
    public void onTradeReport(TradeReport trade) {
        logger.info("CTP received trade report: tradeId={}, orderId={}, symbol={}, side={}, qty={}, price={}",
                trade.getTradeId(), trade.getOrderId(), trade.getSymbol(), trade.getSide(), 
                trade.getQuantity(), trade.getPrice());
        
        if (tradeReportCallback != null) {
            tradeReportCallback.onTradeReport(trade);
        }
    }

    @Override
    public void onOrderReport(OrderReport report) {
        logger.info("CTP received order report: orderId={}, status={}, filledQty={}",
                report.getOrderId(), report.getStatus(), report.getFilledQuantity());
        
        if (orderReportCallback != null) {
            orderReportCallback.onOrderReport(report);
        }
    }

    /**
     * Connect to CTP exchange
     */
    public void connect(String frontAddress, String brokerId, String userId, String password) {
        this.frontAddress = frontAddress;
        this.brokerId = brokerId;
        this.userId = userId;
        this.password = password;
        
        logger.info("CTP connecting to front: {}, brokerId: {}", frontAddress, brokerId);
        
        // In real implementation, this would:
        // 1. Create CThostFtdcTraderApi instance
        // 2. Register SPI callbacks
        // 3. Subscribe to public/private topics
        // 4. Connect to front address
        // 5. Wait for OnFrontConnected callback
        
        connected.set(true);
        logger.info("CTP connected successfully");
    }

    /**
     * Disconnect from CTP exchange
     */
    public void disconnect() {
        logger.info("CTP disconnecting...");
        
        // In real implementation, this would call:
        // api->RegisterSpi(null);
        // api->Release();
        
        connected.set(false);
        orderRefMap.clear();
        logger.info("CTP disconnected");
    }

    /**
     * Login to CTP exchange
     */
    public boolean login() {
        logger.info("CTP logging in: userId={}", userId);
        
        // In real implementation, this would call:
        // CThostFtdcReqUserLoginField field = {...};
        // api->ReqUserLogin(&field, requestId);
        
        // Wait for OnRspUserLogin callback
        connected.set(true);
        logger.info("CTP login successful");
        return true;
    }

    /**
     * Logout from CTP exchange
     */
    public void logout() {
        logger.info("CTP logging out...");
        
        // In real implementation, this would call:
        // CThostFtdcReqUserLogoutField field = {...};
        // api->ReqUserLogout(&field, requestId);
        
        connected.set(false);
        logger.info("CTP logout successful");
    }

    /**
     * Check if connected
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Generate unique exchange order ID
     */
    private String generateExchangeOrderId() {
        return "CTP" + System.currentTimeMillis() + orderIdGenerator.incrementAndGet();
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

    // SPI callback interfaces for CTP

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

    // CTP SPI callback methods (would be called by the native CTP library)

    /**
     * On Front Connected callback
     */
    public void onFrontConnected() {
        logger.info("CTP front connected");
        connected.set(true);
    }

    /**
     * On Front Disconnected callback
     */
    public void onFrontDisconnected(int reason) {
        logger.warn("CTP front disconnected, reason={}", reason);
        connected.set(false);
    }

    /**
     * On Rsp User Login callback
     */
    public void onRspUserLogin(String brokerId, String userId, int errorCode, String errorMsg) {
        if (errorCode == 0) {
            logger.info("CTP user login success: brokerId={}, userId={}", brokerId, userId);
            connected.set(true);
        } else {
            logger.error("CTP user login failed: errorCode={}, errorMsg={}", errorCode, errorMsg);
            connected.set(false);
        }
    }

    /**
     * On Rsp Order Insert callback - when order is accepted/rejected by exchange
     */
    public void onRspOrderInsert(String exchangeOrderId, int errorCode, String errorMsg) {
        logger.info("CTP order insert response: exchangeOrderId={}, errorCode={}", exchangeOrderId, errorCode);
        
        OrderReport report = new OrderReport();
        report.setExchangeOrderId(exchangeOrderId);
        report.setReportTime(LocalDateTime.now());
        
        if (errorCode == 0) {
            report.setStatus(com.qts.biz.trade.enums.OrderStatus.SUBMITTED);
        } else {
            report.setStatus(com.qts.biz.trade.enums.OrderStatus.REJECTED);
            report.setErrorCode(String.valueOf(errorCode));
            report.setErrorMessage(errorMsg);
        }
        
        onOrderReport(report);
    }

    /**
     * On Rtn Trade callback - trade execution notification
     */
    public void onRtnTrade(String exchangeOrderId, String tradeId, String symbol, String side,
                          BigDecimal price, BigDecimal quantity, LocalDateTime tradeTime) {
        logger.info("CTP trade notification: exchangeOrderId={}, tradeId={}, side={}, qty={}, price={}",
                exchangeOrderId, tradeId, side, quantity, price);
        
        TradeReport trade = new TradeReport();
        trade.setTradeId(tradeId);
        trade.setExchangeOrderId(exchangeOrderId);
        trade.setSymbol(symbol);
        trade.setSide(side);
        trade.setPrice(price);
        trade.setQuantity(quantity);
        trade.setAmount(price.multiply(quantity));
        trade.setTradeTime(tradeTime);
        trade.setTradeType("NORMAL");
        
        onTradeReport(trade);
    }

    /**
     * On Rtn Order callback - order status change notification
     */
    public void onRtnOrder(String exchangeOrderId, String symbol, String side,
                          int totalQty, int filledQty, double avgPrice, String status) {
        logger.info("CTP order notification: exchangeOrderId={}, status={}, filledQty={}",
                exchangeOrderId, status, filledQty);
        
        OrderReport report = new OrderReport();
        report.setExchangeOrderId(exchangeOrderId);
        report.setSymbol(symbol);
        report.setFilledQuantity(filledQty);
        report.setAvgPrice(avgPrice);
        report.setReportTime(LocalDateTime.now());
        
        // Map CTP status to OrderStatus
        switch (status) {
            case "Submitted":
            case "PartialFilled":
                report.setStatus(com.qts.biz.trade.enums.OrderStatus.SUBMITTED);
                break;
            case "Filled":
                report.setStatus(com.qts.biz.trade.enums.OrderStatus.FILLED);
                break;
            case "Cancelled":
                report.setStatus(com.qts.biz.trade.enums.OrderStatus.CANCELLED);
                break;
            default:
                report.setStatus(com.qts.biz.trade.enums.OrderStatus.SUBMITTED);
        }
        
        onOrderReport(report);
    }
}
