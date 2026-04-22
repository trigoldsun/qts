package com.qts.biz.risk.monitor.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration for Risk Metrics Push
 * Endpoint: /ws/risk/metrics
 * Push frequency: ≤1s
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple broker for topic-based messaging
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint for risk metrics
        registry.addEndpoint("/ws/risk/metrics")
            .setAllowedOriginPatterns("*")
            .withSockJS();
        
        // Fallback endpoint without SockJS
        registry.addEndpoint("/ws/risk/metrics")
            .setAllowedOriginPatterns("*");
    }
}
