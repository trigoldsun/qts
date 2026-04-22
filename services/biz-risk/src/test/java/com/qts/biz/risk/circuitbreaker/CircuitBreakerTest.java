package com.qts.biz.risk.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CircuitBreaker state machine and progressive recovery.
 * 
 * Tests cover:
 * - CLOSED→OPEN: Error rate > 50% triggers circuit open
 * - OPEN→HALF_OPEN: After 30s recovery timeout
 * - HALF_OPEN→CLOSED: Successful probe returns to closed
 * - Progressive recovery: 10%→30%→50%→100% traffic increase
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
            // minimumNumberOfCalls=10, threshold=50%
            // 10 failures -> rate=100% > 50%, transition at 10th call
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        }
        
        @Test
        @DisplayName("Should stay CLOSED when failure rate is at threshold")
        void shouldStayClosedWhenFailureRateAtThreshold() {
            // Record 10 calls with 5 failures (50% failure rate - at threshold, not exceeding)
            for (int i = 0; i < 10; i++) {
                if (i < 5) {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                } else {
                    circuitBreaker.recordSuccess(50);
                }
            }
            
            // At exactly 50%, should stay CLOSED (exceeds means >, not >=)
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
        
        @Test
        @DisplayName("Should transition to OPEN when slow call rate exceeds threshold")
        void shouldTransitionToOpenWhenSlowCallRateExceedsThreshold() {
            // Record 11 slow calls (100% slow call rate)
            for (int i = 0; i < 11; i++) {
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
            for (int i = 0; i < 11; i++) {
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
        @DisplayName("Should reject requests in OPEN state before timeout")
        void shouldRejectRequestsInOpenStateBeforeTimeout() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .circuitOpenDuration(10) // 10 seconds
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force to OPEN
            for (int i = 0; i < 11; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            assertEquals(CircuitBreakerState.OPEN, cb.getState());
            
            // Should reject requests
            assertFalse(cb.allowRequest());
        }
    }
    
    @Nested
    @DisplayName("HALF_OPEN Progressive Recovery Tests")
    class HalfOpenProgressiveRecoveryTests {
        
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
            for (int i = 0; i < 11; i++) {
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
            for (int i = 0; i < 11; i++) {
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
        
        @Test
        @DisplayName("Should increment half-open attempt index on re-entry")
        void shouldIncrementHalfOpenAttemptIndexOnReentry() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force to OPEN then transition to HALF_OPEN
            for (int i = 0; i < 11; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // Enter HALF_OPEN with attempt index 1
            assertEquals(1, cb.getHalfOpenAttemptIndex());
            
            // Trigger failure to go back to OPEN
            cb.recordFailure(new RuntimeException("Failure"));
            assertEquals(CircuitBreakerState.OPEN, cb.getState());
            
            // Wait and re-enter HALF_OPEN
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // Enter HALF_OPEN with attempt index 2
            assertEquals(2, cb.getHalfOpenAttemptIndex());
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
            for (int i = 0; i < 11; i++) {
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
            // i < 7: 7 successes, i >= 7: 3 failures (i=7,8,9)
            for (int i = 0; i < 10; i++) {
                if (i < 7) {
                    circuitBreaker.recordSuccess(50);
                } else {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                }
            }
            
            // 3 failures / 10 total = 30%
            assertEquals(30.0, circuitBreaker.getFailureRate(), 0.01);
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
            // Wait for circuit to close via recovery
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Generate some metrics
            for (int i = 0; i < 5; i++) {
                cb.recordSuccess(50);
            }
            
            assertTrue(cb.getTotalCalls() > 0);
            
            // Force to OPEN
            for (int i = 0; i < 11; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            // Wait and transition to HALF_OPEN then CLOSED
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
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should not transition before minimum calls reached")
        void shouldNotTransitionBeforeMinimumCallsReached() {
            // Record only 5 calls (less than minimum 10)
            for (int i = 0; i < 5; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            
            // Should still be CLOSED because minimum calls not reached
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
        
        @Test
        @DisplayName("Should handle null exception in recordFailure")
        void shouldHandleNullExceptionInRecordFailure() {
            circuitBreaker.recordFailure(null);
            
            assertEquals(1, circuitBreaker.getTotalCalls());
            assertEquals(1, circuitBreaker.getFailedCalls());
        }
        
        @Test
        @DisplayName("Should return correct metrics snapshot")
        void shouldReturnCorrectMetricsSnapshot() {
            for (int i = 0; i < 5; i++) {
                circuitBreaker.recordSuccess(50);
            }
            circuitBreaker.recordFailure(new RuntimeException("Error"));
            
            CircuitBreaker.CircuitBreakerMetrics metrics = circuitBreaker.getMetrics();
            
            assertEquals("testCircuitBreaker", metrics.getName());
            assertEquals(6, metrics.getTotalCalls());
            assertEquals(1, metrics.getFailedCalls());
            assertTrue(metrics.getFailureRate() > 0);
        }
        
        @Test
        @DisplayName("Should allow request after successful recovery")
        void shouldAllowRequestAfterSuccessfulRecovery() {
            CircuitBreakerConfig shortConfig = CircuitBreakerConfig.builder()
                .failureRateThreshold(50)
                .slowCallDurationMs(100)
                .slowCallRate(50)
                .circuitOpenDuration(1)
                .minimumNumberOfCalls(10)
                .build();
            CircuitBreaker cb = new CircuitBreaker("test", shortConfig);
            
            // Force to OPEN
            for (int i = 0; i < 11; i++) {
                cb.recordFailure(new RuntimeException("Error"));
            }
            
            // Wait and transition to HALF_OPEN
            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            cb.allowRequest(); // HALF_OPEN
            cb.recordSuccess(50);
            cb.recordSuccess(50);
            cb.recordSuccess(50); // CLOSED
            
            // Now should allow requests
            assertTrue(cb.allowRequest());
            assertEquals(CircuitBreakerState.CLOSED, cb.getState());
        }
    }
}
