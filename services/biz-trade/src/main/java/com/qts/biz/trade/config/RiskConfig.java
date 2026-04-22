package com.qts.biz.trade.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Risk Service Configuration
 * Configuration for connecting to the BIZ-RISK risk control service
 */
@Configuration
@ConfigurationProperties(prefix = "qts.risk")
public class RiskConfig {

    private String host = "risk-service";
    private int port = 50051;
    private long timeoutMs = 5000;
    private boolean enableCheck = true;

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

    public boolean isEnableCheck() {
        return enableCheck;
    }

    public void setEnableCheck(boolean enableCheck) {
        this.enableCheck = enableCheck;
    }

    public String getTarget() {
        return host + ":" + port;
    }
}
