package com.qts.biz.trade.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitBreaker state machine and progressive recovery.
 */
class CircuitBreakerTest {
    
    private CircuitBreaker circuitBreaker;
    private CircuitBreakerConfig defaultConfig;
    
    @BeforeEach
    void setUp() {
        defaultConfig = CircuitBreakerConfig.builder()
            .failureRateThreshold(50)
            .slowCallDurationMs(100)
            .slowCallRate(50)
            .circuitOpenDuration(30)
            .minimumNumberOfCalls(10)
            .slidingWindowSize(60)
            .build();
        circuitBreaker = new CircuitBreaker("testCircuitBreaker", defaultConfig);
    }
    
    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {
        
        @Test
        @DisplayName("Should start in CLOSED state")
        void shouldStartInClosedState() {
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
        
        @Test
        @DisplayName("Should allow request in CLOSED state")
        void shouldAllowRequestInClosedState() {
            assertTrue(circuitBreaker.allowRequest());
        }
    }
    
    @Nested
    @DisplayName("State Transition Tests - CLOSED to OPEN")
    class StateTransitionClosedToOpenTests {
        
        @Test
        @DisplayName("Should transition to OPEN when failure rate exceeds threshold")
        void shouldTransitionToOpenWhenFailureRateExceedsThreshold() {
            // Record 10 calls with 6 failures (60% failure rate)
            for (int i = 0; i < 10; i++) {
                if (i < 6) {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                } else {
                    circuitBreaker.recordSuccess(50);
                }
            }
            
            // Failure rate > 50%, should be OPEN
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        }
        
        @Test
        @DisplayName("Should stay CLOSED when failure rate is below threshold")
        void shouldStayClosedWhenFailureRateBelowThreshold() {
            // Record 10 calls with 4 failures (40% failure rate)
            for (int i = 0; i < 10; i++) {
                if (i < 4) {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                } else {
                    circuitBreaker.recordSuccess(50);
                }
            }
            
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
        
        @Test
        @DisplayName("Should transition to OPEN when slow call rate exceeds threshold")
        void shouldTransitionToOpenWhenSlowCallRateExceedsThreshold() {
            // Record 10 slow calls
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordSuccess(200); // 200ms > 100ms threshold
                circuitBreaker.recordSlowCall(200);
            }
            
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        }
    }
    
    @Nested
    @DisplayName("State Transition Tests - OPEN to HALF_OPEN")
    class StateTransitionOpenToHalfOpenTests {
        
        @Test
        @DisplayName("Should transition to HALF_OPEN after circuit open duration expires")
        void shouldTransitionToHalfOpenAfterDurationExpires() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1) // 1 second for testing
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force transition to OPEN
            for (int i = 0; i < 10; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            assertEquals(CircuitBreakerState.OPEN, cb.getState());
            
            // Wait for circuit open duration to expire
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Next allowRequest should trigger transition to HALF_OPEN
            cb.allowRequest();
            assertEquals(CircuitBreakerState.HALF_OPEN, cb.getState());
        }
        
        @Test
        @DisplayName("Should reject requests in OPEN state")
        void shouldRejectRequestsInOpenState() {
            // Force transition to OPEN
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
            
            // Should reject requests
            assertFalse(circuitBreaker.allowRequest());
        }
    }
    
    @Nested
    @DisplayName("HALF_OPEN Progressive Recovery Tests")
    class HalfOpenProgressiveRecoveryTests {
        
        @Test
        @DisplayName("Should allow limited requests in HALF_OPEN state")
        void shouldAllowLimitedRequestsInHalfOpenState() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force to OPEN then transition to HALF_OPEN
            for (int i = 0; i < 10; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // Triggers HALF_OPEN
            
            // In HALF_OPEN, should allow some requests (probabilistic check)
            // The first attempt uses 10% permission
            assertEquals(CircuitBreakerState.HALF_OPEN, cb.getState());
        }
        
        @Test
        @DisplayName("Should close circuit after successful recovery in HALF_OPEN")
        void shouldCloseCircuitAfterSuccessfulRecovery() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force to OPEN then transition to HALF_OPEN
            for (int i = 0; i < 10; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // Enter HALF_OPEN
            assertEquals(CircuitBreakerState.HALF_OPEN, cb.getState());
            
            // Record successful calls (threshold is 3)
            cb.recordSuccess(50);
            cb.recordSuccess(50);
            cb.recordSuccess(50);
            
            // Should transition to CLOSED
            assertEquals(CircuitBreakerState.CLOSED, cb.getState());
        }
        
        @Test
        @DisplayName("Should reopen circuit immediately on failure in HALF_OPEN")
        void shouldReopenCircuitOnFailureInHalfOpen() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force to OPEN then transition to HALF_OPEN
            for (int i = 0; i < 10; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // Enter HALF_OPEN
            assertEquals(CircuitBreakerState.HALF_OPEN, cb.getState());
            
            // Record a failure - should immediately reopen
            cb.recordFailure(new RuntimeException("Test Error"));
            
            assertEquals(CircuitBreakerState.OPEN, cb.getState());
        }
    }
    
    @Nested
    @DisplayName("Execute Method Tests")
    class ExecuteMethodTests {
        
        @Test
        @DisplayName("Should execute supplier successfully when circuit is CLOSED")
        void shouldExecuteSupplierSuccessfullyWhenClosed() throws Exception {
            String result = circuitBreaker.execute(() -> "success");
            assertEquals("success", result);
        }
        
        @Test
        @DisplayName("Should throw exception when circuit is OPEN")
        void shouldThrowExceptionWhenOpen() {
            // Force to OPEN
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            
            assertThrows(CircuitBreaker.CircuitBreakerException.class, () -> {
                circuitBreaker.execute(() -> "should not execute");
            });
        }
        
        @Test
        @DisplayName("Should execute and record success")
        void shouldExecuteAndRecordSuccess() throws Exception {
            AtomicInteger callCount = new AtomicInteger(0);
            
            circuitBreaker.execute(() -> {
                callCount.incrementAndGet();
                return "result";
            });
            
            assertEquals(1, callCount.get());
            assertEquals(1, circuitBreaker.getTotalCalls());
            assertEquals(0, circuitBreaker.getFailedCalls());
        }
        
        @Test
        @DisplayName("Should execute and record failure")
        void shouldExecuteAndRecordFailure() {
            assertThrows(CircuitBreaker.CircuitBreakerException.class, () -> {
                circuitBreaker.execute(() -> {
                    throw new RuntimeException("Test error");
                });
            });
            
            assertEquals(1, circuitBreaker.getTotalCalls());
            assertEquals(1, circuitBreaker.getFailedCalls());
        }
    }
    
    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {
        
        @Test
        @DisplayName("Should calculate correct failure rate")
        void shouldCalculateCorrectFailureRate() {
            for (int i = 0; i < 10; i++) {
                if (i < 7) {
                    circuitBreaker.recordSuccess(50);
                } else {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                }
            }
            
            assertEquals(70.0, circuitBreaker.getFailureRate(), 0.01);
        }
        
        @Test
        @DisplayName("Should calculate correct slow call rate")
        void shouldCalculateCorrectSlowCallRate() {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordSuccess(50);
                if (i < 6) {
                    circuitBreaker.recordSlowCall(200);
                }
            }
            
            assertEquals(60.0, circuitBreaker.getSlowCallRate(), 0.01);
        }
        
        @Test
        @DisplayName("Should track P99 duration")
        void shouldTrackP99Duration() {
            // Simulate various call durations
            long[] durations = {50, 60, 70, 80, 90, 100, 110, 120, 130, 200};
            for (long duration : durations) {
                circuitBreaker.recordSuccess(duration);
            }
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            assertTrue(metrics.getP99DurationMs() > 0);
        }
    }
    
    @Nested
    @DisplayName("Reset Tests")
    class ResetTests {
        
        @Test
        @DisplayName("Should reset metrics when transitioning to CLOSED")
        void shouldResetMetricsWhenTransitioningToClosed() {
            // Generate some metrics
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordSuccess(50);
            }
            
            assertTrue(circuitBreaker.getTotalCalls() > 0);
            
            // Transition to OPEN
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            
            // Wait and transition back to CLOSED via HALF_OPEN
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            for (int i = 0; i < 10; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // HALF_OPEN
            cb.recordSuccess(50);
            cb.recordSuccess(50);
            cb.recordSuccess(50); // CLOSED
            
            assertEquals(0, cb.getTotalCalls());
            assertEquals(0, cb.getFailedCalls());
        }
    }
}
