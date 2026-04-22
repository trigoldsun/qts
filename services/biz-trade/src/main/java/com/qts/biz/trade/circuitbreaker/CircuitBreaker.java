package com.qts.biz.trade.circuitbreaker;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be protected by a circuit breaker.
 * 
 * When applied to a method, the CircuitBreakerAspect will intercept calls and:
 * 1. Check if the circuit is open (reject immediately if so)
 * 2. Execute the method and track success/failure
 * 3. Update circuit state based on failure rate and slow call metrics
 * 
 * Example usage:
 * <pre>
 * {@code
 * @CircuitBreaker(failureRateThreshold = 50, slowCallDuration = 30, slowCallRate = 50)
 * public OrderResult submitOrder(OrderCommand command) {
 *     // business logic
 * }
 * }
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CircuitBreaker {
    
    /**
     * Name of the circuit breaker. If not specified, the fully qualified method name is used.
     */
    String name() default "";
    
    /**
     * Failure rate threshold percentage - circuit opens when failure rate exceeds this
     * Default: 50 (50%)
     */
    int failureRateThreshold() default 50;
    
    /**
     * Slow call duration threshold in milliseconds - calls exceeding this are considered slow
     * Default: 100 milliseconds
     */
    long slowCallDurationMs() default 100;
    
    /**
     * Slow call rate threshold percentage - circuit opens when slow call rate exceeds this
     * Default: 50 (50%)
     */
    int slowCallRate() default 50;
    
    /**
     * Duration in seconds the circuit stays OPEN before transitioning to HALF_OPEN
     * Default: 30 seconds
     */
    int circuitOpenDuration() default 30;
    
    /**
     * Minimum number of calls before calculating failure rate
     * Default: 10 calls
     */
    int minimumNumberOfCalls() default 10;
    
    /**
     * Sliding window size in seconds for calculating metrics
     * Default: 60 seconds
     */
    int slidingWindowSize() default 60;
}
