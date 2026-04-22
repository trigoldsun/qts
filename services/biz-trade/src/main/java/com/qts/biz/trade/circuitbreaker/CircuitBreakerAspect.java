package com.qts.biz.trade.circuitbreaker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect for CircuitBreaker annotation.
 * Intercepts methods annotated with @CircuitBreaker and applies circuit breaker logic.
 */
@Aspect
@Component
@Slf4j
@Order(100)
@RequiredArgsConstructor
public class CircuitBreakerAspect {
    
    private final CircuitBreakerRegistry registry;
    
    /**
     * Around advice for methods annotated with @CircuitBreaker
     */
    @Around("@annotation(circuitBreakerAnnotation)")
    public Object aroundCircuitBreakerMethod(
            ProceedingJoinPoint joinPoint,
            CircuitBreaker circuitBreakerAnnotation) throws Throwable {
        
        // Determine circuit breaker name
        String cbName = resolveCircuitBreakerName(joinPoint, circuitBreakerAnnotation);
        
        // Get or create circuit breaker instance
        CircuitBreakerConfig config = buildConfig(circuitBreakerAnnotation);
        CircuitBreaker circuitBreaker = registry.getOrCreate(cbName, config);
        
        // Check if request is allowed
        if (!circuitBreaker.allowRequest()) {
            log.warn("CircuitBreaker [{}] rejected request for method: {}", 
                cbName, joinPoint.getSignature().toShortString());
            throw new CircuitBreaker.CircuitBreakerException(
                "Circuit breaker [" + cbName + "] is OPEN - request rejected");
        }
        
        // Execute the method with circuit breaker protection
        long startTime = System.nanoTime();
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            
            // Check if call was slow
            if (durationMs > circuitBreakerAnnotation.slowCallDurationMs()) {
                circuitBreaker.recordSlowCall(durationMs);
            }
            
            circuitBreaker.recordSuccess(durationMs);
            return result;
            
        } catch (Throwable throwable) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            circuitBreaker.recordFailure(throwable);
            log.error("CircuitBreaker [{}] recorded failure for method: {}, error: {}", 
                cbName, joinPoint.getSignature().toShortString(), throwable.getMessage());
            throw throwable;
        }
    }
    
    /**
     * Resolve circuit breaker name from annotation or method signature
     */
    private String resolveCircuitBreakerName(ProceedingJoinPoint joinPoint, CircuitBreaker annotation) {
        if (annotation.name() != null && !annotation.name().isEmpty()) {
            return annotation.name();
        }
        // Default to fully qualified method name
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
    
    /**
     * Build circuit breaker config from annotation values
     */
    private CircuitBreakerConfig buildConfig(CircuitBreaker annotation) {
        return CircuitBreakerConfig.builder()
            .failureRateThreshold(annotation.failureRateThreshold())
            .slowCallDuration((int) (annotation.slowCallDurationMs() / 1000))
            .slowCallDurationMs(annotation.slowCallDurationMs())
            .slowCallRate(annotation.slowCallRate())
            .circuitOpenDuration(annotation.circuitOpenDuration())
            .minimumNumberOfCalls(annotation.minimumNumberOfCalls())
            .slidingWindowSize(annotation.slidingWindowSize())
            .build();
    }
}
