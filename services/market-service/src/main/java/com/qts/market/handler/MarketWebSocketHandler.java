package com.qts.market.handler;

import com.qts.market.dto.KlineResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 行情WebSocket消息处理器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送K线数据到指定主题
     * 
     * @param symbol 标的代码
     * @param period K线周期
     * @param klineData K线数据
     */
    public void pushKlineData(String symbol, String period, KlineResponse.KlineData klineData) {
        String destination = buildDestination(symbol, period);
        
        KlineResponse response = KlineResponse.builder()
                .symbol(symbol)
                .period(period)
                .klines(java.util.List.of(klineData))
                .build();
        
        messagingTemplate.convertAndSend(destination, response);
        log.debug("Pushed kline data to {}, symbol={}, period={}", 
                destination, symbol, period);
    }

    /**
     * 广播K线数据到指定主题
     * 
     * @param symbol 标的代码
     * @param period K线周期
     * @param klineData K线数据
     */
    public void broadcastKlineData(String symbol, String period, KlineResponse.KlineData klineData) {
        String destination = "/topic/kline/" + symbol + "/" + period;
        
        KlineResponse response = KlineResponse.builder()
                .symbol(symbol)
                .period(period)
                .klines(java.util.List.of(klineData))
                .build();
        
        messagingTemplate.convertAndSend(destination, response);
        log.info("Broadcast kline data to {}, symbol={}, period={}", 
                destination, symbol, period);
    }

    /**
     * 构建订阅目的地
     */
    public String buildDestination(String symbol, String period) {
        return "/topic/kline/" + symbol + "/" + period;
    }
}
