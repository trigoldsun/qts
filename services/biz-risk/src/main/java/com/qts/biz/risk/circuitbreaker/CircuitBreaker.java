package com.qts.biz.risk.circuitbreaker;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

/**
 * CircuitBreaker implementation with three states:
 * - CLOSED: Normal operation
 * - OPEN: Rejecting all requests
 * - HALF_OPEN: Testing recovery with progressive traffic increase
 * 
 * Supports:
 * - Failure rate threshold (default 50%)
 * - Slow call detection (P99 > 100ms)
 * - Progressive recovery (10% -> 30% -> 50% -> 100%)
 */
@Slf4j
public class CircuitBreaker {
    
    private final String name;
    private final CircuitBreakerConfig config;
    
    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    
    private final AtomicReference<CircuitBreakerState> currentState = 
        new AtomicReference<>(CircuitBreakerState.CLOSED);
    
    public CircuitBreakerState getState() {
        return currentState.get();
    }
    
    private volatile long lastStateChangeTime;
    private volatile long openedAt;
    
    // Metrics tracking
    private final LongAdder totalCalls = new LongAdder();
    private final LongAdder failedCalls = new LongAdder();
    private final LongAdder slowCalls = new LongAdder();
    private final AtomicLong totalDurationNs = new AtomicLong(0);
    private final AtomicLong p99DurationNs = new AtomicLong(0);
    
    // Sliding window metrics (in buckets)
    private static final int BUCKET_COUNT = 10;
    private final Bucket[] buckets = new Bucket[BUCKET_COUNT];
    
    // HALF_OPEN progressive放量: 10% -> 30% -> 50% -> 100%
    private static final double[] HALF_OPEN_PERMISSIONS = {0.1, 0.3, 0.5, 1.0};
    private volatile int halfOpenAttemptIndex = 0;
    private volatile int halfOpenSuccessfulCalls = 0;
    private static final int HALF_OPEN_SUCCESS_THRESHOLD = 3;
    
    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = name;
        this.config = config != null ? config : CircuitBreakerConfig.builder().build();
        this.lastStateChangeTime = System.currentTimeMillis();
        initBuckets();
    }
    
    private void initBuckets() {
        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets[i] = new Bucket();
        }
    }
    
    /**
     * Check if request is allowed through the circuit breaker
     */
    public boolean allowRequest() {
        switch (currentState.get()) {
            case CLOSED:
                return true;
            case OPEN:
                if (shouldTransitionToHalfOpen()) {
                    transitionToHalfOpen();
                    return allowInHalfOpen();
                }
                return false;
            case HALF_OPEN:
                return allowInHalfOpen();
            default:
                return false;
        }
    }
    
    /**
     * Record a successful call
     */
    public void recordSuccess(long durationMs) {
        totalCalls.increment();
        totalDurationNs.addAndGet(durationMs * 1_000_000);
        updateP99(durationMs);
        recordInBucket(true, durationMs);
        
        if (currentState.get() == CircuitBreakerState.HALF_OPEN) {
            halfOpenSuccessfulCalls++;
            // After HALF_OPEN_SUCCESS_THRESHOLD successful calls, close the circuit
            if (halfOpenSuccessfulCalls >= HALF_OPEN_SUCCESS_THRESHOLD) {
                transitionToClosed();
            }
        }
        
        log.debug("CircuitBreaker [{}] recorded success, duration={}ms, state={}", 
            name, durationMs, currentState.get());
    }
    
    /**
     * Record a failed call
     */
    public void recordFailure(Throwable cause) {
        totalCalls.increment();
        failedCalls.increment();
        recordInBucket(false, 0);
        
        if (currentState.get() == CircuitBreakerState.HALF_OPEN) {
            // Any failure in HALF_OPEN immediately opens the circuit
            transitionToOpen();
            return;
        }
        
        // Check if we should transition to OPEN based on failure rate
        checkFailureRateThreshold();
        
        log.debug("CircuitBreaker [{}] recorded failure: {}, state={}", 
            name, cause != null ? cause.getMessage() : "unknown", currentState.get());
    }
    
    /**
     * Record a slow call (P99 > threshold)
     */
    public void recordSlowCall(long durationMs) {
        slowCalls.increment();
        
        if (durationMs > config.getSlowCallDurationMs()) {
            recordInBucket(true, durationMs, true);
        }
        
        // Check slow call rate threshold
        checkSlowCallRateThreshold();
        
        log.debug("CircuitBreaker [{}] recorded slow call: {}ms", name, durationMs);
    }
    
    /**
     * Execute supplier with circuit breaker protection
     */
    public <T> T execute(Supplier<T> supplier) throws CircuitBreakerException {
        if (!allowRequest()) {
            throw new CircuitBreakerException(
                "Circuit breaker [" + name + "] is OPEN - request rejected");
        }
        
        long startTime = System.nanoTime();
        try {
            T result = supplier.get();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            
            // Check if call was slow (P99 > threshold)
            if (durationMs > config.getSlowCallDurationMs()) {
                recordSlowCall(durationMs);
            }
            
            recordSuccess(durationMs);
            return result;
        } catch (Exception e) {
            recordFailure(e);
            throw new CircuitBreakerException("Circuit breaker execution failed", e);
        }
    }
    
    /**
     * Execute runnable with circuit breaker protection
     */
    public void execute(Runnable runnable) throws CircuitBreakerException {
        execute(() -> {
            runnable.run();
            return null;
        });
    }
    
    private boolean allowInHalfOpen() {
        double permission = HALF_OPEN_PERMISSIONS[Math.min(halfOpenAttemptIndex, HALF_OPEN_PERMISSIONS.length - 1)];
        // Simple probabilistic check - allows percentage of requests
        // More sophisticated implementation would use actual request counting
        return Math.random() < permission;
    }
    
    private boolean shouldTransitionToHalfOpen() {
        long openDuration = System.currentTimeMillis() - openedAt;
        return openDuration >= config.getCircuitOpenDuration() * 1000;
    }
    
    void transitionToOpen() {
        CircuitBreakerState previousState = currentState.getAndSet(CircuitBreakerState.OPEN);
        if (previousState != CircuitBreakerState.OPEN) {
            openedAt = System.currentTimeMillis();
            lastStateChangeTime = System.currentTimeMillis();
            log.info("CircuitBreaker [{}] transitioned from {} to OPEN (failure rate exceeded)", 
                name, previousState);
        }
    }
    
    void transitionToHalfOpen() {
        CircuitBreakerState previousState = currentState.getAndSet(CircuitBreakerState.HALF_OPEN);
        if (previousState != CircuitBreakerState.HALF_OPEN) {
            lastStateChangeTime = System.currentTimeMillis();
            halfOpenAttemptIndex++;
            halfOpenSuccessfulCalls = 0;
            log.info("CircuitBreaker [{}] transitioned from {} to HALF_OPEN (attempt #{})", 
                name, previousState, halfOpenAttemptIndex);
        }
    }
    
    void transitionToClosed() {
        CircuitBreakerState previousState = currentState.getAndSet(CircuitBreakerState.CLOSED);
        if (previousState != CircuitBreakerState.CLOSED) {
            lastStateChangeTime = System.currentTimeMillis();
            halfOpenAttemptIndex = 0;
            halfOpenSuccessfulCalls = 0;
            resetMetrics();
            log.info("CircuitBreaker [{}] transitioned from {} to CLOSED (recovery successful)", 
                name, previousState);
        }
    }
    
    private void checkFailureRateThreshold() {
        if (totalCalls.sum() < config.getMinimumNumberOfCalls()) {
            return;
        }
        
        double failureRate = (double) failedCalls.sum() / totalCalls.sum() * 100;
        if (failureRate > config.getFailureRateThreshold()) {
            transitionToOpen();
        }
    }
    
    private void checkSlowCallRateThreshold() {
        if (totalCalls.sum() < config.getMinimumNumberOfCalls()) {
            return;
        }
        
        double slowCallRate = (double) slowCalls.sum() / totalCalls.sum() * 100;
        if (slowCallRate > config.getSlowCallRate()) {
            transitionToOpen();
        }
    }
    
    private void recordInBucket(boolean success, long durationMs) {
        recordInBucket(success, durationMs, false);
    }
    
    private void recordInBucket(boolean success, long durationMs, boolean slow) {
        int bucketIndex = (int) (System.currentTimeMillis() / 1000) % BUCKET_COUNT;
        Bucket bucket = buckets[bucketIndex];
        if (success) {
            bucket.successCount.increment();
            if (slow || durationMs > config.getSlowCallDurationMs()) {
                bucket.slowCount.increment();
            }
        } else {
            bucket.failureCount.increment();
        }
        bucket.callCount.increment();
    }
    
    private void updateP99(long durationMs) {
        // Simplified P99 calculation - in production would use proper percentile algorithm
        long currentP99 = p99DurationNs.get();
        long newP99 = durationMs * 1_000_000;
        p99DurationNs.updateAndGet(current -> 
            current == 0 ? newP99 : (long)(current * 0.9 + newP99 * 0.1));
    }
    
    private void resetMetrics() {
        totalCalls.reset();
        failedCalls.reset();
        slowCalls.reset();
        totalDurationNs.set(0);
        for (Bucket bucket : buckets) {
            bucket.reset();
        }
    }
    
    // Getters for testing and monitoring
    public String getName() { return name; }
    public CircuitBreakerConfig getConfig() { return config; }
    public long getLastStateChangeTime() { return lastStateChangeTime; }
    public long getTotalCalls() { return totalCalls.sum(); }
    public long getFailedCalls() { return failedCalls.sum(); }
    public long getSlowCalls() { return slowCalls.sum(); }
    public double getFailureRate() { 
        long total = totalCalls.sum();
        return total > 0 ? (double) failedCalls.sum() / total * 100 : 0; 
    }
    public double getSlowCallRate() { 
        long total = totalCalls.sum();
        return total > 0 ? (double) slowCalls.sum() / total * 100 : 0; 
    }
    public long getP99DurationMs() { return p99DurationNs.get() / 1_000_000; }
    public int getHalfOpenAttemptIndex() { return halfOpenAttemptIndex; }
    
    /**
     * Get current metrics snapshot
     */
    public CircuitBreakerMetrics getMetrics() {
        return CircuitBreakerMetrics.builder()
            .name(name)
            .state(currentState.get())
            .totalCalls(totalCalls.sum())
            .failedCalls(failedCalls.sum())
            .slowCalls(slowCalls.sum())
            .failureRate(getFailureRate())
            .slowCallRate(getSlowCallRate())
            .p99DurationMs(getP99DurationMs())
            .lastStateChangeTime(lastStateChangeTime)
            .build();
    }
    
    /**
     * Inner class for sliding window buckets
     */
    private static class Bucket {
        LongAdder callCount = new LongAdder();
        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        LongAdder slowCount = new LongAdder();
        
        void reset() {
            callCount.reset();
            successCount.reset();
            failureCount.reset();
            slowCount.reset();
        }
    }
    
    /**
     * Metrics holder for monitoring
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CircuitBreakerMetrics {
        private String name;
        private CircuitBreakerState state;
        private long totalCalls;
        private long failedCalls;
        private long slowCalls;
        private double failureRate;
        private double slowCallRate;
        private long p99DurationMs;
        private long lastStateChangeTime;
    }
    
    /**
     * Exception thrown when circuit breaker rejects a request
     */
    public static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message) {
            super(message);
        }
        public CircuitBreakerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
