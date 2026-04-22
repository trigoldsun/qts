package com.qts.market.controller;

import com.qts.market.handler.MarketWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * WebSocket行情推送控制器
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class MarketPushController {

    private final MarketWebSocketHandler marketWebSocketHandler;

    /**
     * 订阅行情
     * 客户端发送: /app/subscribe
     * 消息体: {"symbol": "BTC-USDT", "period": "1m"}
     */
    @MessageMapping("/subscribe")
    public void subscribe(@Payload SubscriptionRequest request) {
        log.info("Subscription request: symbol={}, period={}", 
                request.getSymbol(), request.getPeriod());
        
        String destination = marketWebSocketHandler.buildDestination(
                request.getSymbol(), request.getPeriod());
        
        log.info("Client subscribed to: {}", destination);
    }

    /**
     * 取消订阅
     */
    @MessageMapping("/unsubscribe")
    public void unsubscribe(@Payload SubscriptionRequest request) {
        log.info("Unsubscription request: symbol={}, period={}", 
                request.getSymbol(), request.getPeriod());
    }

    /**
     * 订阅请求
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SubscriptionRequest {
        private String symbol;
        private String period;
    }
}
