package com.qts.biz.trade.adapter;

import com.qts.biz.trade.dto.Order;
import com.qts.biz.trade.dto.OrderCancelResult;
import com.qts.biz.trade.dto.OrderModifyResult;
import com.qts.biz.trade.dto.OrderReport;
import com.qts.biz.trade.dto.OrderSendResult;
import com.qts.biz.trade.dto.TradeReport;

import java.math.BigDecimal;

/**
 * Exchange Adapter Interface
 * SPI mechanism for对接交易所CTP接口
 */
public interface ExchangeAdapter {

    /**
     * Send order to exchange
     */
    OrderSendResult sendOrder(Order order);

    /**
     * Modify existing order
     */
    OrderModifyResult modifyOrder(String orderId, BigDecimal newPrice, Integer newQty);

    /**
     * Cancel order
     */
    OrderCancelResult cancelOrder(String orderId);

    /**
     * Handle trade report from exchange
     */
    void onTradeReport(TradeReport trade);

    /**
     * Handle order report from exchange
     */
    void onOrderReport(OrderReport report);
}
