package com.qts.biz.trade.circuitbreaker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Event emitted by CircuitBreaker for state changes and call results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerEvent {
    
    /**
     * Type of circuit breaker event
     */
    public enum EventType {
        STATE_TRANSITION,
        CALL_SUCCESS,
        CALL_FAILURE,
        CALL_SLOW,
        METRICS_UPDATED
    }
    
    /**
     * The circuit breaker name that emitted this event
     */
    private String circuitBreakerName;
    
    /**
     * Type of event
     */
    private EventType eventType;
    
    /**
     * Previous state (for STATE_TRANSITION events)
     */
    private CircuitBreakerState previousState;
    
    /**
     * New state (for STATE_TRANSITION events)
     */
    private CircuitBreakerState newState;
    
    /**
     * Timestamp when event occurred
     */
    private Instant timestamp;
    
    /**
     * Additional context/details about the event
     */
    private String details;
    
    /**
     * Create a state transition event
     */
    public static CircuitBreakerEvent stateTransition(String name, CircuitBreakerState from, CircuitBreakerState to) {
        return CircuitBreakerEvent.builder()
                .circuitBreakerName(name)
                .eventType(EventType.STATE_TRANSITION)
                .previousState(from)
                .newState(to)
                .timestamp(Instant.now())
                .details(String.format("Circuit breaker '%s' transitioned from %s to %s", name, from, to))
                .build();
    }
    
    /**
     * Create a call success event
     */
    public static CircuitBreakerEvent callSuccess(String name, long durationMs) {
        return CircuitBreakerEvent.builder()
                .circuitBreakerName(name)
                .eventType(EventType.CALL_SUCCESS)
                .timestamp(Instant.now())
                .details(String.format("Call succeeded in %dms", durationMs))
                .build();
    }
    
    /**
     * Create a call failure event
     */
    public static CircuitBreakerEvent callFailure(String name, Throwable cause) {
        return CircuitBreakerEvent.builder()
                .circuitBreakerName(name)
                .eventType(EventType.CALL_FAILURE)
                .timestamp(Instant.now())
                .details(String.format("Call failed: %s", cause != null ? cause.getMessage() : "unknown"))
                .build();
    }
    
    /**
     * Create a call slow event
     */
    public static CircuitBreakerEvent callSlow(String name, long durationMs, long threshold) {
        return CircuitBreakerEvent.builder()
                .circuitBreakerName(name)
                .eventType(EventType.CALL_SLOW)
                .timestamp(Instant.now())
                .details(String.format("Call took %dms (threshold: %dms)", durationMs, threshold))
                .build();
    }
}
