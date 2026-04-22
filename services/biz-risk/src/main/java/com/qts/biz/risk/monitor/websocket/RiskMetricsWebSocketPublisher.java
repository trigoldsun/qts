package com.qts.biz.risk.monitor.websocket;

import com.qts.biz.risk.monitor.dto.RiskMonitoringMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * WebSocket Metrics Publisher
 * Pushes risk monitoring metrics to connected clients via WebSocket
 */
@Component
public class RiskMetricsWebSocketPublisher {

    private static final Logger logger = LoggerFactory.getLogger(RiskMetricsWebSocketPublisher.class);
    
    private static final String METRICS_TOPIC = "/topic/risk/metrics";
    private static final String CHANNEL_TOPIC_PREFIX = "/topic/risk/metrics/channel/";
    private static final String ACCOUNT_TOPIC_PREFIX = "/topic/risk/metrics/account/";
    
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public RiskMetricsWebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publish metrics to all subscribers
     */
    public void publishMetrics(RiskMonitoringMetrics metrics) {
        try {
            messagingTemplate.convertAndSend(METRICS_TOPIC, metrics);
            logger.debug("Published metrics to {}", METRICS_TOPIC);
        } catch (Exception e) {
            logger.error("Failed to publish metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish metrics for a specific channel
     */
    public void publishChannelMetrics(String channelId, RiskMonitoringMetrics metrics) {
        try {
            messagingTemplate.convertAndSend(CHANNEL_TOPIC_PREFIX + channelId, metrics);
            logger.debug("Published metrics to channel {}", channelId);
        } catch (Exception e) {
            logger.error("Failed to publish channel metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Publish metrics for a specific account
     */
    public void publishAccountMetrics(String accountId, RiskMonitoringMetrics metrics) {
        try {
            messagingTemplate.convertAndSend(ACCOUNT_TOPIC_PREFIX + accountId, metrics);
            logger.debug("Published metrics to account {}", accountId);
        } catch (Exception e) {
            logger.error("Failed to publish account metrics: {}", e.getMessage(), e);
        }
    }
}
