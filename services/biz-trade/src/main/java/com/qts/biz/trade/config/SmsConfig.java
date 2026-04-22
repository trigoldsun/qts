package com.qts.biz.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SMS (Security Management Service) Configuration
 * Handles security compliance and regulatory checks for trading
 */
@Configuration
@ConfigurationProperties(prefix = "qts.sms")
public class SmsConfig {

    private String host = "sms-service";
    private int port = 50051;
    private long timeoutMs = 5000;
    private boolean enableCompliance = true;

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

    public boolean isEnableCompliance() {
        return enableCompliance;
    }

    public void setEnableCompliance(boolean enableCompliance) {
        this.enableCompliance = enableCompliance;
    }

    public String getTarget() {
        return host + ":" + port;
    }
}
