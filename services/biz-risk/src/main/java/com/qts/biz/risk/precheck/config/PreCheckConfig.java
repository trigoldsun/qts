package com.qts.biz.risk.precheck.config;

import org.springframework.context.annotation.Configuration;

/**
 * PreCheck configuration
 * Central configuration for risk pre-check service
 */
@Configuration
public class PreCheckConfig {

    // Position limit: single stock <= 20% of total assets
    public static final double MAX_POSITION_RATIO = 0.20;

    // Price change limit: ±10% of previous close
    public static final double MAX_PRICE_CHANGE_RATIO = 0.10;

    // Default margin rate: 10%
    public static final double DEFAULT_MARGIN_RATE = 0.10;

    // Trading session times
    public static final String TRADING_SESSIONS = "9:15-9:25, 9:30-11:30, 13:00-14:57";
}