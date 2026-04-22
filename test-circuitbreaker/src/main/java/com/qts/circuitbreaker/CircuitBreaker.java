package com.qts.circuitbreaker;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Supplier;

public class CircuitBreaker {
    private final String name;
    private final CircuitBreakerConfig config;
    
    private final AtomicReference<CircuitBreakerState> currentState = 
        new AtomicReference<>(CircuitBreakerState.CLOSED);
    private volatile long lastStateChangeTime;
    private volatile long openedAt;
    
    // Metrics
    private final LongAdder totalCalls = new LongAdder();
    private final LongAdder failedCalls = new LongAdder();
    private final LongAdder slowCalls = new LongAdder();
    private final AtomicLong p99DurationNs = new AtomicLong(0);
    
    // HALF_OPEN progressive放量: 10% -> 30% -> 50% -> 100%
    private static final double[] HALF_OPEN_PERMISSIONS = {0.1, 0.3, 0.5, 1.0};
    private volatile int halfOpenAttemptIndex = 0;
    private volatile int halfOpenSuccessfulCalls = 0;
    private static final int HALF_OPEN_SUCCESS_THRESHOLD = 3;
    
    public CircuitBreaker(String name, CircuitBreakerConfig config) {
        this.name = name;
        this.config = config != null ? config : CircuitBreakerConfig.builder().build();
        this.lastStateChangeTime = System.currentTimeMillis();
    }
    
    public CircuitBreakerState getState() {
        return currentState.get();
    }
    
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
    
    public void recordSuccess(long durationMs) {
        totalCalls.increment();
        updateP99(durationMs);
        
        if (currentState.get() == CircuitBreakerState.HALF_OPEN) {
            halfOpenSuccessfulCalls++;
            if (halfOpenSuccessfulCalls >= HALF_OPEN_SUCCESS_THRESHOLD) {
                transitionToClosed();
            }
        }
    }
    
    public void recordFailure(Throwable cause) {
        totalCalls.increment();
        failedCalls.increment();
        
        if (currentState.get() == CircuitBreakerState.HALF_OPEN) {
            transitionToOpen();
            return;
        }
        checkFailureRateThreshold();
    }
    
    public void recordSlowCall(long durationMs) {
        slowCalls.increment();
        checkSlowCallRateThreshold();
    }
    
    public <T> T execute(Supplier<T> supplier) throws CircuitBreakerException {
        if (!allowRequest()) {
            throw new CircuitBreakerException(
                "Circuit breaker [" + name + "] is OPEN - request rejected");
        }
        long startTime = System.nanoTime();
        try {
            T result = supplier.get();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
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
    
    private boolean allowInHalfOpen() {
        double permission = HALF_OPEN_PERMISSIONS[Math.min(halfOpenAttemptIndex, HALF_OPEN_PERMISSIONS.length - 1)];
        return Math.random() < permission;
    }
    
    private boolean shouldTransitionToHalfOpen() {
        long openDuration = System.currentTimeMillis() - openedAt;
        return openDuration >= config.getCircuitOpenDuration() * 1000;
    }
    
    private void transitionToOpen() {
        CircuitBreakerState previousState = currentState.getAndSet(CircuitBreakerState.OPEN);
        if (previousState != CircuitBreakerState.OPEN) {
            openedAt = System.currentTimeMillis();
            lastStateChangeTime = System.currentTimeMillis();
        }
    }
    
    private void transitionToHalfOpen() {
        CircuitBreakerState previousState = currentState.getAndSet(CircuitBreakerState.HALF_OPEN);
        if (previousState != CircuitBreakerState.HALF_OPEN) {
            lastStateChangeTime = System.currentTimeMillis();
            halfOpenAttemptIndex++;
            halfOpenSuccessfulCalls = 0;
        }
    }
    
    private void transitionToClosed() {
        CircuitBreakerState previousState = currentState.getAndSet(CircuitBreakerState.CLOSED);
        if (previousState != CircuitBreakerState.CLOSED) {
            lastStateChangeTime = System.currentTimeMillis();
            halfOpenAttemptIndex = 0;
            halfOpenSuccessfulCalls = 0;
            resetMetrics();
        }
    }
    
    private void checkFailureRateThreshold() {
        if (totalCalls.sum() < config.getMinimumNumberOfCalls()) return;
        double failureRate = (double) failedCalls.sum() / totalCalls.sum() * 100;
        if (failureRate > config.getFailureRateThreshold()) {
            transitionToOpen();
        }
    }
    
    private void checkSlowCallRateThreshold() {
        if (totalCalls.sum() < config.getMinimumNumberOfCalls()) return;
        double slowCallRate = (double) slowCalls.sum() / totalCalls.sum() * 100;
        if (slowCallRate > config.getSlowCallRate()) {
            transitionToOpen();
        }
    }
    
    private void updateP99(long durationMs) {
        long newP99 = durationMs * 1_000_000;
        p99DurationNs.updateAndGet(current -> current == 0 ? newP99 : (long)(current * 0.9 + newP99 * 0.1));
    }
    
    private void resetMetrics() {
        totalCalls.reset();
        failedCalls.reset();
        slowCalls.reset();
    }
    
    public String getName() { return name; }
    public CircuitBreakerConfig getConfig() { return config; }
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
    public int getHalfOpenAttemptIndex() { return halfOpenAttemptIndex; }
    
    public CircuitBreakerMetrics getMetrics() {
        return CircuitBreakerMetrics.builder()
            .name(name)
            .state(currentState.get())
            .totalCalls(totalCalls.sum())
            .failedCalls(failedCalls.sum())
            .slowCalls(slowCalls.sum())
            .failureRate(getFailureRate())
            .slowCallRate(getSlowCallRate())
            .lastStateChangeTime(lastStateChangeTime)
            .build();
    }
    
    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class CircuitBreakerMetrics {
        private String name;
        private CircuitBreakerState state;
        private long totalCalls;
        private long failedCalls;
        private long slowCalls;
        private double failureRate;
        private double slowCallRate;
        private long lastStateChangeTime;
    }
    
    public static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message) { super(message); }
        public CircuitBreakerException(String message, Throwable cause) { super(message, cause); }
    }
}
