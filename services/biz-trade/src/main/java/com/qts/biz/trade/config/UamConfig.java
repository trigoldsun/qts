package com.qts.biz.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * UAM (User Authentication Management) Configuration
 * Handles user authentication and authorization for the trade service
 */
@Configuration
@ConfigurationProperties(prefix = "qts.uam")
public class UamConfig {

    private String host = "uam-service";
    private int port = 50051;
    private long timeoutMs = 5000;
    private boolean enableAuth = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean isEnableAuth() {
        return enableAuth;
    }

    public void setEnableAuth(boolean enableAuth) {
        this.enableAuth = enableAuth;
    }

    public String getTarget() {
        return host + ":" + port;
    }
}
