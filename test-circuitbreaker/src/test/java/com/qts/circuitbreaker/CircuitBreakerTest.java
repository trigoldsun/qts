package com.qts.circuitbreaker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {
    private CircuitBreaker circuitBreaker;
    private CircuitBreakerConfig defaultConfig;
    
    @BeforeEach
    void setUp() {
        defaultConfig = CircuitBreakerConfig.builder()
            .failureRateThreshold(50)
            .slowCallDurationMs(100)
            .slowCallRate(50)
            .circuitOpenDuration(1) // 1 second for fast testing
            .minimumNumberOfCalls(10)
            .build();
        circuitBreaker = new CircuitBreaker("testCircuitBreaker", defaultConfig);
    }
    
    @Nested
    @DisplayName("Initial State Tests")
    class InitialStateTests {
        @Test
        void shouldStartInClosedState() {
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
        @Test
        void shouldAllowRequestInClosedState() {
            assertTrue(circuitBreaker.allowRequest());
        }
    }
    
    @Nested
    @DisplayName("State Transition: CLOSED to OPEN")
    class StateTransitionClosedToOpenTests {
        @Test
        void shouldTransitionToOpenWhenFailureRateExceedsThreshold() {
            // Record exactly 10 calls with 6 failures (60% failure rate)
            // After 10th call, checkFailureRateThreshold will be called and threshold exceeded
            for (int i = 0; i < 10; i++) {
                if (i < 6) {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                } else {
                    circuitBreaker.recordSuccess(50);
                }
            }
            // After 10 calls with 60% failure rate, should be OPEN
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        }
        
        @Test
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
        void shouldNotEvaluateBeforeMinimumCalls() {
            // Record only 5 calls - should still be CLOSED regardless of failure rate
            for (int i = 0; i < 5; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
    }
    
    @Nested
    @DisplayName("State Transition: OPEN to HALF_OPEN")
    class StateTransitionOpenToHalfOpenTests {
        @Test
        void shouldTransitionToHalfOpenAfterDurationExpires() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
            
            Thread.sleep(1100);
            circuitBreaker.allowRequest();
            assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());
        }
        
        @Test
        void shouldRejectRequestsInOpenState() {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            assertFalse(circuitBreaker.allowRequest());
        }
    }
    
    @Nested
    @DisplayName("HALF_OPEN Progressive Recovery Tests")
    class HalfOpenProgressiveRecoveryTests {
        @Test
        void shouldCloseCircuitAfterSuccessfulRecovery() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            Thread.sleep(1100);
            circuitBreaker.allowRequest();
            assertEquals(CircuitBreakerState.HALF_OPEN, circuitBreaker.getState());
            
            circuitBreaker.recordSuccess(50);
            circuitBreaker.recordSuccess(50);
            circuitBreaker.recordSuccess(50);
            assertEquals(CircuitBreakerState.CLOSED, circuitBreaker.getState());
        }
        
        @Test
        void shouldReopenCircuitOnFailureInHalfOpen() throws InterruptedException {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            Thread.sleep(1100);
            circuitBreaker.allowRequest();
            circuitBreaker.recordFailure(new RuntimeException("Test Error"));
            assertEquals(CircuitBreakerState.OPEN, circuitBreaker.getState());
        }
    }
    
    @Nested
    @DisplayName("Execute Method Tests")
    class ExecuteMethodTests {
        @Test
        void shouldExecuteSupplierSuccessfullyWhenClosed() throws Exception {
            String result = circuitBreaker.execute(() -> "success");
            assertEquals("success", result);
        }
        
        @Test
        void shouldThrowExceptionWhenOpen() {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordFailure(new RuntimeException("Error"));
            }
            assertThrows(CircuitBreaker.CircuitBreakerException.class, () -> {
                circuitBreaker.execute(() -> "should not execute");
            });
        }
        
        @Test
        void shouldRecordSuccessAndFailure() {
            AtomicInteger callCount = new AtomicInteger(0);
            circuitBreaker.execute(() -> { callCount.incrementAndGet(); return "result"; });
            assertEquals(1, callCount.get());
            assertEquals(1, circuitBreaker.getTotalCalls());
            assertEquals(0, circuitBreaker.getFailedCalls());
            
            assertThrows(CircuitBreaker.CircuitBreakerException.class, () -> {
                circuitBreaker.execute(() -> { throw new RuntimeException("Test error"); });
            });
            assertEquals(2, circuitBreaker.getTotalCalls());
            assertEquals(1, circuitBreaker.getFailedCalls());
        }
    }
    
    @Nested
    @DisplayName("Metrics Tests")
    class MetricsTests {
        @Test
        void shouldCalculateCorrectFailureRate() {
            for (int i = 0; i < 10; i++) {
                if (i < 7) {
                    circuitBreaker.recordSuccess(50);
                } else {
                    circuitBreaker.recordFailure(new RuntimeException("Error"));
                }
            }
            // 3 failures out of 10 = 30%
            assertEquals(30.0, circuitBreaker.getFailureRate(), 0.01);
        }
        
        @Test
        void shouldCalculateCorrectSlowCallRate() {
            for (int i = 0; i < 10; i++) {
                circuitBreaker.recordSuccess(50);
                if (i < 6) {
                    circuitBreaker.recordSlowCall(200);
                }
            }
            // 6 slow calls out of 10 total calls = 60%
            assertEquals(60.0, circuitBreaker.getSlowCallRate(), 0.01);
        }
    }
}
