package com.qts.market.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket配置
 * 配置STOMP消息代理和端点
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 配置消息代理前缀
        // /app 开头的信息会路由到@MessageMapping标注的方法
        registry.setApplicationDestinationPrefixes("/app");
        
        // 配置推送前缀 - 客户端订阅主题时使用
        // /topic 用于广播（一对多）
        // /queue 用于点对点（一对一）
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 可选：配置用户特定的目的地前缀
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过该端点连接WebSocket
        // withSockJS() 支持SockJS协议（浏览器兼容）
        registry.addEndpoint("/ws/market")
                .setAllowedOriginPatterns("*")
                .withSockJS();
        
        // 原始WebSocket端点（无SockJS）
        registry.addEndpoint("/ws/market")
                .setAllowedOriginPatterns("*");
    }
}
