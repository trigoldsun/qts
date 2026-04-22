package com.qts.biz.risk.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for CircuitBreaker behavior
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerConfig {
    
    /**
     * Failure rate threshold percentage - circuit opens when failure rate exceeds this
     * Default: 50 (50%)
     */
    private int failureRateThreshold = 50;
    
    /**
     * Slow call duration threshold in milliseconds - calls exceeding this are considered slow
     * Default: 100 milliseconds
     */
    private int slowCallDurationMs = 100;
    
    /**
     * Slow call rate threshold percentage - circuit opens when slow call rate exceeds this
     * Default: 50 (50%)
     */
    private int slowCallRate = 50;
    
    /**
     * Duration in seconds the circuit stays OPEN before transitioning to HALF_OPEN
     * Default: 30 seconds
     */
    private int circuitOpenDuration = 30;
    
    /**
     * Minimum number of calls before calculating failure rate
     * Default: 10 calls
     */
    private int minimumNumberOfCalls = 10;
    
    /**
     * Sliding window size in seconds for calculating metrics
     * Default: 60 seconds
     */
    private int slidingWindowSize = 60;
    
    /**
     * Sliding window bucket count for fine-grained metrics
     * Default: 10 buckets
     */
    private int slidingWindowBucketCount = 10;
}
