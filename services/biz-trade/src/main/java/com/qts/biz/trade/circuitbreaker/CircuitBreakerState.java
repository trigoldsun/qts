package com.qts.biz.trade.circuitbreaker;

/**
 * CircuitBreaker state enumeration representing the three states of the circuit breaker:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Circuit is open, requests are rejected immediately
 * - HALF_OPEN: Trial state, limited requests pass through to test recovery
 */
public enum CircuitBreakerState {
    /**
     * Circuit is closed - normal operation
     * All requests pass through and are monitored
     */
    CLOSED,
    
    /**
     * Circuit is open - requests are rejected
     * After the failure threshold is exceeded, circuit opens
     * and rejects all requests for a specified duration
     */
    OPEN,
    
    /**
     * Circuit is half-open - trial state
     * Allows a limited number of requests to test if the service
     * has recovered. Progressive放量 (traffic increase) strategy
     * is applied: 10% -> 30% -> 50% -> 100%
     */
    HALF_OPEN
}
