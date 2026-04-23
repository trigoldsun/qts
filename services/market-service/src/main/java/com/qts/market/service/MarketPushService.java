package com.qts.market.service;

import com.qts.market.dto.KlineResponse;
import com.qts.market.handler.MarketWebSocketHandler;
import com.qts.market.model.Kline;
import com.qts.market.repository.KlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 行情推送服务
 * 负责管理和推送实时行情数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPushService {

    private final KlineRepository klineRepository;
    private final MarketWebSocketHandler marketWebSocketHandler;

    // 订阅记录: symbol_period -> true
    private final Map<String, CopyOnWriteArraySet<String>> subscriptions = new ConcurrentHashMap<>();

    /**
     * 模拟行情推送任务 - 每秒执行
     * 生产环境应接入真实的行情数据源
     */
    @Scheduled(fixedRate = 1000)
    public void pushMarketData() {
        // 获取所有活跃订阅
        subscriptions.keySet().forEach(key -> {
            String[] parts = key.split("_", 2);
            if (parts.length == 2) {
                String symbol = parts[0];
                String period = parts[1];
                
                // 获取最新K线数据
                Kline latestKline = klineRepository.findLatestBySymbolAndPeriod(symbol, period);
                
                if (latestKline != null) {
                    KlineResponse.KlineData klineData = KlineResponse.KlineData.builder()
                            .open(latestKline.getOpenPrice())
                            .high(latestKline.getHighPrice())
                            .low(latestKline.getLowPrice())
                            .close(latestKline.getClosePrice())
                            .volume(latestKline.getVolume())
                            .amount(latestKline.getAmount())
                            .timestamp(latestKline.getTimestamp())
                            .build();
                    
                    // 广播到所有订阅者
                    marketWebSocketHandler.broadcastKlineData(symbol, period, klineData);
                }
            }
        });
    }

    /**
     * 订阅标的周期
     */
    public void subscribe(String symbol, String period, String sessionId) {
        String key = buildKey(symbol, period);
        
        subscriptions.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>())
                .add(sessionId);
        
        log.info("Session {} subscribed to {}_{}, total subscribers: {}", 
                sessionId, symbol, period, 
                subscriptions.get(key).size());
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String symbol, String period, String sessionId) {
        String key = buildKey(symbol, period);
        
        if (subscriptions.containsKey(key)) {
            subscriptions.get(key).remove(sessionId);
            
            log.info("Session {} unsubscribed from {}_{}, remaining: {}", 
                    sessionId, symbol, period,
                    subscriptions.get(key).size());
            
            // Clean up empty subscription sets
            if (subscriptions.get(key).isEmpty()) {
                subscriptions.remove(key);
            }
        }
    }

    /**
     * 获取活跃订阅数量
     */
    public int getActiveSubscriptionCount() {
        return subscriptions.size();
    }

    private String buildKey(String symbol, String period) {
        return symbol + "_" + period;
    }
}
